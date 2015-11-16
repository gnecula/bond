package io.github.necula01.tutorial.heatwatcher;

import io.github.necula01.bond.*;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;


public class HeatWatcherTest {

  // This is how to enable Bond for this class
  @Rule
  public BondTestRule btr = new BondTestRule();

  private TimeMocker timeMocker;
  private TemperatureMocker tempMocker;

  // A test where the higher-level functions getTemperature is mocked out.
  // No alerts.
  @Test
  public void testOkWarning() {
    deployTimeMock();

    // Instantiate TemperatureMocker with starting temperature and
    // temperature rates, as pairs of timeSinceStart, tempIncrRatePerPinute
    tempMocker = new TemperatureMocker(timeMocker, 70,
                                       new double[] {0, 0.5, 60, 1.2, 110, 0.12});

    // Deploy a Bond agent for getTemperature
    SpyAgent getTemperatureAgent = new SpyAgent()
        .withResult(new Resulter() {
          public Double accept(Map<String, Object> map) {
            return tempMocker.getTemperature();
          }
        });
    Bond.deployAgent("HeatWatcher.getTemperature", getTemperatureAgent);

    // Now invoke the code
    HeatWatcher watcher = new HeatWatcher();
    watcher.monitorLoop(timeMocker.getCurrentTime() + 400);
  }

  // A test where makeRequest is mocked out, and returns different responses
  // depending on the URL that is passed in. This can allow you to test that the
  // parsing logic in getTemperature is working correctly.
  @Test
  public void testCritical() {
    deployTimeMock();

    double[] tempRates = {0, 0.5, 60, 2.5, 120, 3.0, 140, 0.5};
    tempMocker = new TemperatureMocker(timeMocker, 70, tempRates);

    // Do not deploy an agent for getTemperature, let it call makeRequest
    SpyAgent makeRequestMessagesAgent = new SpyAgent()
        .withFilterKeyContains("url", "messages")
        .withResult(HeatWatcher.AlertState.OK.toString());
    Bond.deployAgent("HeatWatcher.makeRequest", makeRequestMessagesAgent);

    // Deploy an agent for the makeRequest to return a temperature
    SpyAgent makeRequestTemperatureAgent = new SpyAgent()
        .withFilterKeyContains("url", "temperature")
        .withResult(new Resulter() {
          public String accept(Map<String, Object> map) {
            double temp = tempMocker.getTemperature();
            return "<temperature>" + temp + "</temperature>";
          }
        });
    Bond.deployAgent("HeatWatcher.makeRequest", makeRequestTemperatureAgent);

    // Now invoke the code
    HeatWatcher watcher = new HeatWatcher();
    watcher.monitorLoop(timeMocker.getCurrentTime() + 400);
  }
  // rst_TestEnd

  // Turn on the getCurrentTime mock
  private void deployTimeMock() {
    // Default 10/23/2015 @ 2:35am (UTC)
    timeMocker = new TimeMocker(1445567700);
    SpyAgent getCurrentTimeAgent = new SpyAgent()
        .withResult(new Resulter() {
          public Double accept(Map<String, Object> map) {
            return timeMocker.getCurrentTime();
          }
        });
    Bond.deployAgent("HeatWatcher.getCurrentTime", getCurrentTimeAgent);

    // mock the sleep also
    SpyAgent sleepAgent = new SpyAgent()
        .withDoer(new Doer() {
          public void accept(Map<String, Object> map) {
            int seconds = (Integer) map.get("seconds");
            timeMocker.sleep(seconds);
          }
        });
    Bond.deployAgent("HeatWatcher.sleep", sleepAgent);
  }
}


// rst_TimeMocker
// A class for mocking time
class TimeMocker {
  private double currentTime;

  public TimeMocker(double now) {
    currentTime = now;
  }

  public double getCurrentTime() {
    return currentTime;
  }

  public void sleep(int seconds) {
    currentTime += seconds;
  }
}


// rst_TemperatureMocker
// A class for mocking temperature
class TemperatureMocker {

  private double[] tempRates;
  private double tempStart;
  private TimeMocker timeMocker;
  private double startTime;

  /**
   * @param timeMocker: a TimeMocker
   * @param tempStart:  the starting temperature
   * @param tempRates:  an array of timeSinceStart, tempIncreaseRatePerMin, ...
   */
  public TemperatureMocker(TimeMocker timeMocker,
                           double tempStart,
                           double[] tempRates) {
    this.timeMocker = timeMocker;
    this.tempStart = tempStart;
    this.tempRates = tempRates;
    startTime = timeMocker.getCurrentTime();

  }

  // Get the current temperature, based on the current time
  public double getTemperature() {
    double timeSinceStart = timeMocker.getCurrentTime() - startTime;
    double temp = tempStart;

    double lastTime = tempRates[0];
    double lastRate = tempRates[1];

    for (int i = 2; i < tempRates.length; i += 2) {
      double rTime = tempRates[i];
      double rRate = tempRates[i + 1];
      if (timeSinceStart <= rTime) {
        break;
      }
      temp += (rTime - lastTime) * lastRate / 60.0;
      lastTime = rTime;
      lastRate = rRate;
    }
    return temp + (timeSinceStart - lastTime) * lastRate / 60.0;
  }
}