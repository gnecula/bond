/**
 * A simple demonstration of using Bond for spying and mocking
 * in the context of an application for monitoring temperature and sending alerts.
 * <p/>
 * See a full explanation of this example at
 * http://necula01.github.io/bond/example_heat.html
 **/

package tutorial.heat_watcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Optional;

import bond.Bond;


/**
 * Monitor temperature rise over time.
 * See description in Bond documentation.
 */
public class HeatWatcher {

    private double last_temp = -1; // The last temp measurement
    private double last_time = -1; // The time when we took the last measurement

    private String last_alert_state = "Ok"; // Ok, Warning, Critical
    private double last_alert_time = Double.NEGATIVE_INFINITY; // Time when we sent the last alert

    /**
     * Run the monitoring loop
     * @param exit_time: the time when to stop (for testing, default -1)
     */
    public void monitor_loop(double exit_time) {
        int interval; // Interval in seconds until next reading

        while (true) {
            double temp = get_temperature();
            double now = get_current_time();
            if (exit_time > -1 && now > exit_time) {
                return;
            }
            if (last_temp == -1) {
                // First temperature reading
                last_temp = temp;
                last_time = now;
                interval = 60;
            } else {
                // Change rate since last reading
                double change_rate = (temp - last_temp) / (now - last_time) * 60.0;
                String alert_state;

                if (change_rate < 1.0) {
                    interval = 60;
                    alert_state = "Ok";
                } else if (change_rate < 2.0) {
                    interval = 10;
                    alert_state = "Warning";
                } else {
                    interval = 10;
                    alert_state = "Critical";
                }

                last_temp = temp;
                last_time = now;

                if (!alert_state.equals(last_alert_state) ||
                        (!alert_state.equals("Ok") && now >= 600 + last_alert_time)) {
                    // Send an alert
                    send_alert(alert_state + ": Temperature is rising at ");
                    last_alert_time = now;
                }
                last_alert_state = alert_state;
            }
            try {
                sleep(interval);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    // Read the temperature from a sensor
    double get_temperature() {
        // Check for mocking first
        Optional<Double> mock_temp = Bond.spy("HeatWatcher.get_temperature", Double.class);
        if (mock_temp.isPresent()) {
            Bond.obs("result", mock_temp.get()).spy("HeatWatcher.get_temperature.result");
            return mock_temp.get();
        }
        String temp_data = make_request("http://system.server.com/temperature", null);
        // assert resp_code == 200, "Error while retrieving temperature!"
        Pattern temp_pattern = Pattern.compile("<temperature>([0-9.]+)</temperature>");
        Matcher m = temp_pattern.matcher(temp_data);
        if (m.find()) {
            double result = Double.parseDouble(m.group(1));
            Bond.obs("result", result).spy("HeatWatcher.get_temperature.result");
            return result;
        } else {
            throw new IllegalArgumentException("Cannot parse temperature");
        }
    }


    // Get the current time, in Unix epoch seconds
    double get_current_time() {
        double result;
        // Check for mocking first
        Optional<Double> mock_time = Bond.spy("HeatWatcher.get_current_time", Double.class);
        if (mock_time.isPresent()) {
            result = mock_time.get();
        } else {
            // Actual production code
            result = ((double) System.currentTimeMillis() / 1000.0);
        }
        Bond.obs("result", mock_time.get()).spy("HeatWatcher.get_current_time.result");
        return result;
    }

    // Sleep a number of seconds
    void sleep(int seconds) throws InterruptedException {
        // Check for mocking first
        if (Bond.isActive()) {
            Bond.obs("seconds", seconds).spy("HeatWatcher.sleep");
            return;
        }
        Thread.sleep(1000 * seconds);

    }

    // Send an alert
    void send_alert(String message) {
        Bond.obs("message", message).spy("HeatWatcher.send_alert");
        make_request("http://backend.server.com/messages", message);
    }

    // Make a GET or POST request
    String make_request(String url, String data) {
        // Check for mocking first
        Optional<String> response = Bond.obs("url", url)
                .obs("data", data)
                .spy("HeatWatcher.make_request", String.class);
        if (response.isPresent()) {
            return response.get();
        }
        // Actual code, not shown for brevity
        return "_not_implemented_";
    }

}