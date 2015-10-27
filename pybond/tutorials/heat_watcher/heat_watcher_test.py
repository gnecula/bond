#
# Tests using Bond for the heat_watcher.py app
#
# See a full explanation of this example at
# http://necula01.github.io/bond/example_heat.html
#

import unittest
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
        A test where the higher-level functions get_temperature and send_alert are
        mocked out to return specified temperatures and do nothing, respectively.
        """

        self.deploy_time_mock()
        temp_rates = [(0, 0.5), (60, 1.2), (110, 0.12)]
        self.temp_mocker = TemperatureMocker(time_mocker=self.time_mocker,
                                             temp_start=70,
                                             temp_rates=temp_rates)

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
        temp_rates = [(0, 0.5), (60, 2.5), (120, 3), (140, 0.5)]
        self.temp_mocker = TemperatureMocker(time_mocker=self.time_mocker,
                                             temp_start=70,
                                             temp_rates=temp_rates)

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
    def __init__(self, time_mocker, temp_start, temp_rates):
        """
        :param time_mocker: a reference to the current time mocker
        :param temp_start: the starting temperature
        :param temp_rates: a list of pairs (time_since_start,
                                            temp_increase_rate_per_min)
                           ordered by time_since_start
                           (first time_since_start should be 0)
        """
        self.time_mocker = time_mocker
        self.start_time = time_mocker.time()
        self.temp_start = temp_start
        self.temp_rates = temp_rates

    def get_temperature(self, observation={}):
        time_since_start = self.time_mocker.time() - self.start_time
        temp = self.temp_start
        last_time, last_rate = self.temp_rates[0]

        # Loop through all of the rates, adding up their contributions
        for r_time, r_rate in self.temp_rates[1:]:
            if time_since_start <= r_time:
                break
            temp += (r_time - last_time) * last_rate / 60.0
            last_time, last_rate = r_time, r_rate
        return temp + (time_since_start - last_time) * last_rate / 60.0

if __name__ == '__main__':
    unittest.main()