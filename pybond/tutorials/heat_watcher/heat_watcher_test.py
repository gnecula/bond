#
# Tests using Bond for the heat_watcher.py app

import unittest
import bond

from heat_watcher import HeatWatcher

class HeatWatcherTest:

    def setup_mocks(self, temp_steps):
        "Ok state then alert"
        self.time_mocker = TimeMocker()
        self.temp_mocker = TemperatureMocker(time_mocker=self.time_mocker,
                                        temp_steps=self.temp_mocker)
        bond.deploy_agent('heat_watcher.get_current_time',
                          result=self.time_mocker.current_time)
        bond.deploy_agent('heat_watcher.sleep',
                          result=self.time_mocker.sleep)
        bond.deploy_agent('heat_watcher.get_temperature',
                          result=self.temp_mocker.get_temperature)
        bond.deploy_agent('heat_watcher.send_alert',
                          result=None)

    def test_ok_warning(self):
        self.setup_mocks(((0, 70), (60, 70), (300, 75)))
        HeatWatcher().monitor_loop(self.time_mocker.current_time() + 400)

class TimeMocker:
    """
    A class to mock time
    """
    def __init__(self, current_time=1445567700):
        # Default 10/23/2015 @ 2:35am (UTC)
        self.current_time = current_time

    def time(self):
        return self.current_time

    def sleep(self, seconds):
        self.current_time += seconds

class TemperatureMocker:
    """
    A class to mock temperature
    """
    def __init__(self,
                 time_mocker,
                 temp_steps=()):
        """

        :param time_mocker: a reference to the current time mocker
        :param temp_steps: a list of pairs (time_since_start, temperature_to_report)
        :return:
        """
        self.time_mocker = time_mocker
        self.start_time =  time_mocker.time ()
        self.temp_steps = temp_steps

    def get_temperature(self):
        time_since_start = self.time_mocker.time () - self.start_time
        # See if we need to advance past the next temperature step
        if len(self.temp_steps) > 1 and time_since_start >= self.temp_steps[1][0]:
            self.temp_steps.pop(0)

        # The first pair is the one we use
        return self.temp_steps[0][1]

if __name__ == '__main__':
    unittest.main ()