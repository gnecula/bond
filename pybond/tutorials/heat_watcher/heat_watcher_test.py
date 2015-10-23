#
# Tests using Bond for the heat_watcher.py app
#
# See a full explanation of this example at
# http://necula01.github.io/bond/example_heat.html
#

import unittest
import sys, os
# Add bond directory to syspath so it loads even if you haven't actually installed Bond
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from bond import bond
from heat_watcher import HeatWatcher


class HeatWatcherTest(unittest.TestCase):

    def setUp(self):
        bond.start_test(self)

    def deploy_time_mock(self):
        "Helper function to setup the time mocks and agents"
        self.time_mocker = TimeMocker()

        bond.deploy_agent('HeatWatcher.get_current_time',
                          result=self.time_mocker.time)
        bond.deploy_agent('HeatWatcher.sleep',
                          result=self.time_mocker.sleep)

    def test_ok_warning(self):
        """
        A test where the higher-level functions get_temperature and send_alert
        are mocked out to return specified temperatures and do nothing, respectively.
        """
        self.deploy_time_mock()
        self.temp_mocker = TemperatureMocker(time_mocker=self.time_mocker,
                                             temp_start=70,
                                             temp_rates=[(0, 0.5), (60, 1.3), (110, 0.1)])

        bond.deploy_agent('HeatWatcher.get_temperature',
                          result=self.temp_mocker.get_temperature)
        bond.deploy_agent('HeatWatcher.send_alert',
                          result=None)

        HeatWatcher().monitor_loop(self.time_mocker.current_time + 400)

    def test_critical(self):
        """
        A test where make_request is mocked out, and returns different responses
        depending on the URL that is passed in. This can allow you to test that the
        parsing logic in get_temperature is working correctly.
        """
        self.deploy_time_mock()
        self.temp_mocker = TemperatureMocker(time_mocker=self.time_mocker,
                                             temp_start=70,
                                             temp_rates=[(0, 0.5), (60, 2.5), (120, 3), (140, 0.5)])

        bond.deploy_agent('HeatWatcher.make_request',
                          url__contains='messages',
                          result=None)
        bond.deploy_agent('HeatWatcher.make_request',
                          url__contains='temperature',
                          result=lambda obs: (200, '<temperature>{}</temperature>'
                          .format(self.temp_mocker.get_temperature())))

        HeatWatcher().monitor_loop(self.time_mocker.time() + 210)

# rst_TimeMocker
class TimeMocker:
    """
    A class to mock time
    """
    def __init__(self, current_time=1445567700):
        # Default 10/23/2015 @ 2:35am (UTC)
        self.current_time = current_time

    def time(self, observation={}):
        # Bond expects result functions to take in the observations hash
        # as a parameter, so we keep it here even though it isn't used.
        return self.current_time

    def sleep(self, observation):
        self.current_time += observation['seconds']

# rst_TemperatureMocker
class TemperatureMocker:
    """
    A class to mock temperature
    """
    def __init__(self,
                 time_mocker,
                 temp_start,
                 temp_rates=()):
        """

        :param time_mocker: a reference to the current time mocker
        :param temp_start: the starting temperature
        :param temp_rates: a list of pairs (time_since_start,
                                            temperature_increase_rate_per_min)
                           ordered by time_since_start
        :return:
        """
        self.time_mocker = time_mocker
        self.start_time = time_mocker.time()
        self.last_temp = temp_start       # last temp read
        self.last_temp_time = self.start_time  # last temp read time
        self.temp_rates = temp_rates

    def get_temperature(self, observation={}):
        now = self.time_mocker.time()
        time_since_start = now - self.start_time
        # See if we need to advance to the next temperature rate
        if len(self.temp_rates) > 1 and time_since_start >= self.temp_rates[1][0]:
            old_rate = self.temp_rates.pop(0)[1]
            old_rate_time = self.temp_rates[0][0]
            self.last_temp += (old_rate_time + self.start_time - self.last_temp_time) / 60.0 * old_rate
            self.last_temp_time = old_rate_time + self.start_time

        # The first pair is the one we use to get the rate
        rate = self.temp_rates[0][1] if len(self.temp_rates) > 0 else 0
        self.last_temp += (now - self.last_temp_time) / 60.0 * rate
        self.last_temp_time = now
        return self.last_temp

if __name__ == '__main__':
    unittest.main()