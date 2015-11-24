/**
 * A simple demonstration of using Bond for spying and mocking
 * in the context of an application for monitoring temperature and sending alerts.
 * <p/>
 * See a full explanation of this example at
 * http://necula01.github.io/bond/example_heat.html
 **/

package tutorial.heatwatcher;

import bond.spypoint.SpyPoint;

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
      if (exitTime > -1 && now >= exitTime) {
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
          sendAlert(String.format("%s: Temperature is rising at %.1f deg/min",
              alertState, changeRate));
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
  @SpyPoint(spyResult = true)
  double getTemperature() {
    String tempData = makeRequest("http://system.server.com/temperature", null);
    Pattern tempPattern = Pattern.compile("<temperature>([0-9.]+)</temperature>");
    Matcher m = tempPattern.matcher(tempData);
    if (m.find()) {
      return Double.parseDouble(m.group(1));
    } else {
      throw new IllegalArgumentException("Cannot parse temperature");
    }
  }

  // Get the current time, in Unix epoch seconds
  @SpyPoint
  double getCurrentTime() {
    return ((double) System.currentTimeMillis() / 1000.0);
  }

  // Sleep a number of seconds
  @SpyPoint
  void sleep(int seconds) throws InterruptedException {
    Thread.sleep(1000 * seconds);

  }

  // Send an alert
  @SpyPoint
  void sendAlert(String message) {
    makeRequest("http://backend.server.com/messages", message);
  }

  // Make a GET or POST request
  @SpyPoint(requireAgentResult = true)
  String makeRequest(String url, String data) {
    // Actual code not shown for brevity
    return "_not_implemented_";
  }

}