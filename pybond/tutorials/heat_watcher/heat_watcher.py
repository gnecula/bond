#
# A simple demonstration of using Bond for spying and mocking
# an application for monitoring temperature and sending alerts
#
# See a full explanation of this example at
# http://necula01.github.io/bond/example_heat.html
#

import time
import re
import sys, os
import urllib, urllib2
# Add bond directory to syspath so it loads even if you haven't actually installed Bond
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from bond import bond

class HeatWatcher:
    """
    Monitor temperature rise over time.
    See description in the Bond documentation.
    """
    def __init__(self):
        self.last_temp = None  # The last temp measurement
        self.last_time = None  # The time when we took the last measurement
        self.last_alert_state = 'Ok'   # Ok, Warning, Critical
        self.last_alert_time = float('-inf')  # The time when we sent the last alert

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

                if (alert_state != self.last_alert_state or
                        (alert_state != 'Ok' and now >= 600 + self.last_alert_time)):
                    # Send an alert
                    self.send_alert("{}: Temperature is rising at {:.1f} deg/min"
                                    .format(alert_state,
                                            change_rate))
                    self.last_alert_time = now

                self.last_alert_state = alert_state

            self.sleep(interval)


    # Spy this function, want to spy the result
    @bond.spy_point(spy_result=True)
    def get_temperature(self):
        """
        Read the temperature from a sensor
        """
        resp_code, temp_data = \
            self.make_request('http://system.server.com/temperature')
        assert resp_code == 200, 'Error while retrieving temperature!'
        match = re.search('<temperature>([0-9.]+)</temperature>', temp_data)
        assert match is not None, \
            'Error while parsing temperature from: {}'.format(temp_data)
        return float(match.group(1))

    @bond.spy_point()
    def get_current_time(self):
        """
        Read the current time
        """
        return time.time()

    @bond.spy_point()
    def sleep(self, seconds):
        """
        Sleep a few seconds
        """
        time.sleep(seconds)

    @bond.spy_point()
    def send_alert(self, message):
        """
        Send an alert
        """
        self.make_request('http://backend.server.com/messages', {'message': message})

    @bond.spy_point(require_agent_result=True)
    def make_request(self, url, data=None):
        """
        HTTP request (GET, or POST if the data is provided)
        """
        resp = urllib2.urlopen(url, urllib.urlencode(data))
        return (resp.getcode(), resp.read())

