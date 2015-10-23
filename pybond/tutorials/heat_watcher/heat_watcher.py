#
# A simple demonstration of using Bond for spying and mocking
# an application for monitoring temperature and sending alerts
#

import time
import bond

class HeatWatcher:
    """
    Monitor temperature rise over time.
    See description in the Bond documentation.
    """
    def __init__(self):
        self.last_temp = None  # The last temp measurement
        self.last_time = None  # The time when we took the last measurement
        self.last_alert_state = 'Ok'   # Ok, Warning, Critical
        self.last_alert_time = None  # The time when we sent the last alert

    def monitor_loop(self,
                     exit_time=None):
        """
        Monitor the temperature and send alerts
        :param exit_time: the time when to exit the monitor loop.
        """
        while True:
            temp = self.get_temperature()
            now = self.get_current_time()
            if exit_time is not None and now >= exit_time:
                return

            if self.last_temp is None:
                # First reading
                self.last_temp = temp
                self.last_time = now
                interval = 60
            else:
                change_rate = (temp - self.last_temp) / (now - self.last_time) * 60
                if change_rate < 1:
                    interval = 60
                    alert_state = 'Ok'
                elif change_rate < 2:
                    interval = 10
                    alert_state = 'Warning'
                else:
                    interval = 10
                    alert_state = 'Critical'

                self.last_temp = temp
                self.last_time = now
                self.last_alert_state = alert_state

                if (alert_state != self.last_alert_state or
                        (alert_state != 'Ok' and now >= 600 + self.last_alert_time)):
                    # Send an alert
                    self.send_alert("{}: Temperature is rising at {:.1f} deg/min"
                                    .format(alert_state,
                                            change_rate))
                    self.last_alert_time = now

            self.sleep(interval)


    # Spy this function, want to spy the result
    @bond.spy_point(spy_result=True,
                    require_agent_result=True)
    def get_temperature(self):
        """
        Read the temperature from a sensor
        :return: either the temperature (a number), or None
        """
        some_code_to_read_temperature()

    @bond.spy_point()
    def get_current_time(self):
        """
        Read the current time
        :return:
        """
        return time.time()

    @bond.spy_point()
    def sleep(self, seconds):
        """
        Sleep a few seconds
        :param seconds:
        :return:
        """
        time.sleep(seconds)

    @bond.spy_point(require_agent_result=True)
    def send_alert(self, message):
        """
        Send an alert
        :param message: the message to send
        :return:
        """
        some_code_to_send_alerts(message)

