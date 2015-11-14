package tutorial.heat_watcher;

import bond.*;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;


public class HeatWatcherTest {

    // This is how to enable Bond for this class
    @Rule
    public BondTestRule btr = new BondTestRule();

    // A test where the higher-level functions get_temperature is mocked out.
    // No alerts.
    @Test
    public void test_Ok_Warning() {
        deploy_time_mock();

        // Instantiate TemperatureMocker with starting temperature and
        // temperature rates, as pairs of time_since_start, temp_incr_rate_per_minute
        temp_mocker = new TemperatureMocker(time_mocker,
                                            70,
                                            new double[]{0, 0.5, 60, 1.2, 110, 0.12});

        // Deploy a Bond agent for get_temperature
        SpyAgent get_temperature_agent = new SpyAgent()
                .withResult(new Resulter() {

                    public Object accept(Map<String, Object> map) {
                        return temp_mocker.get_temperature();
                    }
                });
        Bond.deployAgent("HeatWatcher.get_temperature", get_temperature_agent);

        // Now invoke the code
        HeatWatcher watcher = new HeatWatcher();
        watcher.monitor_loop(time_mocker.get_current_time() + 400);
    }

    // A test where make_request is mocked out, and returns different responses
    // depending on the URL that is passed in. This can allow you to test that the
    // parsing logic in get_temperature is working correctly.
    @Test
    public void test_Critical() {
        deploy_time_mock();

        double[] temp_rates = {0, 0.5, 60, 2.5, 120, 3.0, 140, 0.5};
        temp_mocker = new TemperatureMocker(time_mocker, 70, temp_rates);

        // Do not deploy an agent for get_temperature, let it call make_request
        SpyAgent make_request_messages_agent = new SpyAgent()
                .withFilterKeyContains("url", "messages")
                .withResult("ok");
        Bond.deployAgent("HeatWatcher.make_request", make_request_messages_agent);

        // Deploy an agent for the make_request to return a temperature
        SpyAgent make_request_temperature_agent = new SpyAgent()
                .withFilterKeyContains("url", "temperature")
                .withResult(new Resulter() {
                    @Override
                    public Object accept(Map<String, Object> map) {
                        double temp = temp_mocker.get_temperature();
                        return "<temperature>" + Double.toString(temp) + "</temperature>";
                    }
                });
        Bond.deployAgent("HeatWatcher.make_request", make_request_temperature_agent);

        // Now invoke the code
        HeatWatcher watcher = new HeatWatcher();
        watcher.monitor_loop(time_mocker.get_current_time() + 400);
    }


    private TimeMocker time_mocker;
    private TemperatureMocker temp_mocker;

    // Turn on the get_current_time mock
    private void deploy_time_mock() {
        // Default 10/23/2015 @ 2:35am (UTC)
        time_mocker = new TimeMocker(1445567700);
        SpyAgent get_current_time_agent = new SpyAgent()
                .withResult(new Resulter() {

                    public Object accept(Map<String, Object> map) {
                        return time_mocker.get_current_time();
                    }
                });
        Bond.deployAgent("HeatWatcher.get_current_time", get_current_time_agent);

        // mock the sleep also
        SpyAgent sleep_agent = new SpyAgent()
                .withDoer(new Doer() {

                    public void accept(Map<String, Object> map) {
                        int seconds = (Integer) map.get("seconds");
                        time_mocker.sleep(seconds);
                    }
                });
        Bond.deployAgent("HeatWatcher.sleep", sleep_agent);
    }
}


// rst_TimeMocker
// A class for mocking time
class TimeMocker {
    private double current_time;

    public TimeMocker(double now) {
        current_time = now;
    }

    public double get_current_time() {
        return current_time;
    }

    public void sleep(int seconds) {
        current_time += seconds;
    }
}


// rst_TemperatureMocker
// A class for mocking temperature
class TemperatureMocker {

    private double[] temp_rates;
    private double temp_start;
    private TimeMocker time_mocker;
    private double start_time;

    /**
     * @param time_mocker: a TimeMocker
     * @param temp_start:  the starting temperature
     * @param temp_rates:  an array of time_since_start, temp_increase_rate_per_min, ...
     */
    public TemperatureMocker(TimeMocker time_mocker,
                             double temp_start,
                             double[] temp_rates) {
        this.time_mocker = time_mocker;
        this.temp_start = temp_start;
        this.temp_rates = temp_rates;
        start_time = time_mocker.get_current_time();

    }

    // Get the current temperature, based on the current time
    public double get_temperature() {
        double time_since_start = time_mocker.get_current_time() - start_time;
        double temp = temp_start;

        double last_time = temp_rates[0];
        double last_rate = temp_rates[1];

        for (int i = 2; i < temp_rates.length; i += 2) {
            double r_time = temp_rates[i];
            double r_rate = temp_rates[i + 1];
            if (time_since_start <= r_time) {
                break;
            }
            temp += (r_time - last_time) * last_rate / 60.0;
            last_time = r_time;
            last_rate = r_rate;
        }
        return temp + (time_since_start - last_time) * last_rate / 60.0;
    }
}