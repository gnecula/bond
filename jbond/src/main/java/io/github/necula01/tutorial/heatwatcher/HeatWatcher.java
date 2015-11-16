/**
 * A simple demonstration of using Bond for spying and mocking
 * in the context of an application for monitoring temperature and sending alerts.
 * <p/>
 * See a full explanation of this example at
 * http://necula01.github.io/bond/example_heat.html
 **/

package io.github.necula01.tutorial.heatwatcher;

import io.github.necula01.bond.Bond;
import com.google.common.base.Optional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Monitor temperature rise over time.
 * See description in Bond documentation.
 */
public class HeatWatcher {

  private double lastTemp = -1; // The last temp measurement
  private double lastTime = -1; // The time when we took the last measurement

  public enum AlertState {OK, WARNING, CRITICAL}

  private AlertState lastAlertState = AlertState.OK;
  // Time when we sent the last alert
  private double lastAlertTime = Double.NEGATIVE_INFINITY;

  /**
   * Run the monitoring loop
   *
   * @param exitTime: the time when to stop (for testing, default -1)
   */
  public void monitorLoop(double exitTime) {
    int interval; // Interval in seconds until next reading

    while (true) {
      double temp = getTemperature();
      double now = getCurrentTime();
      if (exitTime > -1 && now > exitTime) {
        return;
      }
      if (lastTemp == -1) {
        // First temperature reading
        lastTemp = temp;
        lastTime = now;
        interval = 60;
      } else {
        // Change rate since last reading
        double changeRate = (temp - lastTemp) / (now - lastTime) * 60.0;
        AlertState alertState;

        if (changeRate < 1.0) {
          interval = 60;
          alertState = AlertState.OK;
        } else if (changeRate < 2.0) {
          interval = 10;
          alertState = AlertState.WARNING;
        } else {
          interval = 10;
          alertState = AlertState.CRITICAL;
        }

        lastTemp = temp;
        lastTime = now;

        if ((alertState != lastAlertState) ||
                (alertState != AlertState.OK && now >= 600 + lastAlertTime)) {
          // Send an alert
          sendAlert(alertState + ": Temperature is rising at ");
          lastAlertTime = now;
        }
        lastAlertState = alertState;
      }
      try {
        sleep(interval);
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  // Read the temperature from a sensor
  double getTemperature() {
    // Check for mocking first
    Optional<Double> mockTemp = Bond.spy("HeatWatcher.getTemperature", Double.class);
    if (mockTemp.isPresent()) {
      Bond.obs("result", mockTemp.get()).spy("HeatWatcher.getTemperature.result");
      return mockTemp.get();
    }
    String tempData = makeRequest("http://system.server.com/temperature", null);
    Pattern tempPattern = Pattern.compile("<temperature>([0-9.]+)</temperature>");
    Matcher m = tempPattern.matcher(tempData);
    if (m.find()) {
      double result = Double.parseDouble(m.group(1));
      Bond.obs("result", result).spy("HeatWatcher.getTemperature.result");
      return result;
    } else {
      throw new IllegalArgumentException("Cannot parse temperature");
    }
  }

  // Get the current time, in Unix epoch seconds
  double getCurrentTime() {
    double result;
    // Check for mocking first
    Optional<Double> mockTime = Bond.spy("HeatWatcher.getCurrentTime", Double.class);
    if (mockTime.isPresent()) {
      result = mockTime.get();
    } else {
      // Actual production code
      result = ((double) System.currentTimeMillis() / 1000.0);
    }
    Bond.obs("result", mockTime.get()).spy("HeatWatcher.getCurrentTime.result");
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
  void sendAlert(String message) {
    Bond.obs("message", message).spy("HeatWatcher.sendAlert");
    makeRequest("http://backend.server.com/messages", message);
  }

  // Make a GET or POST request
  String makeRequest(String url, String data) {
    // Check for mocking first
    Optional<String> response = Bond.obs("url", url)
                                    .obs("data", data)
                                    .spy("HeatWatcher.makeRequest", String.class);
    if (response.isPresent()) {
      return response.get();
    }
    // Actual code, not shown for brevity
    return "_not_implemented_";
  }

}