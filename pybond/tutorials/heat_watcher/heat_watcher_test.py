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

        def temp_func(time_since_start):
            if time_since_start < 60:
                return 70 + 0.5 * (time_since_start / 60.0)
            elif time_since_start < 110:
                return 70.5 + 1.2 * ((time_since_start - 60) / 60.0)
            else:
                return 71.5 + 0.12 * ((time_since_start - 110) / 60.0)

        self.deploy_time_mock()
        self.temp_mocker = TemperatureMocker(time_mocker=self.time_mocker,
                                             temp_func=temp_func)

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

        def temp_func(time_since_start):
            if time_since_start < 60:
                return 70 + 0.5 * (time_since_start / 60.0)
            elif time_since_start < 120:
                return 70.5 + 2.5 * ((time_since_start - 60) / 60.0)
            elif time_since_start < 140:
                print 'here'
                return 73 + 3 * ((time_since_start - 120) / 60.0)
            else:
                return 74 + 0.5 * ((time_since_start - 140) / 60.0)

        self.deploy_time_mock()
        self.temp_mocker = TemperatureMocker(time_mocker=self.time_mocker,
                                             temp_func=temp_func)

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
    def __init__(self, time_mocker, temp_func):
        """
        :param time_mocker: a reference to the current time mocker
        :param temp_start: the starting temperature
        :param temp_func: function that takes in the number of seconds
                          since start and returns a temperature value
        """
        self.time_mocker = time_mocker
        self.start_time = time_mocker.time()
        self.temp_func = temp_func

    def get_temperature(self, observation={}):
        now = self.time_mocker.time()
        time_since_start = now - self.start_time
        return self.temp_func(time_since_start)

if __name__ == '__main__':
    unittest.main()