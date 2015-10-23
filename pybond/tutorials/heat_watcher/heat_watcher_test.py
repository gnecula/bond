#
# Tests using Bond for the heat_watcher.py app

import unittest
import bond

from heat_watcher import HeatWatcher

class HeatWatcherTest:

    def setup_mocks(self, temp_rates):
        "Helper function to setup the mocks and agents"
        self.time_mocker = TimeMocker()
        self.temp_mocker = TemperatureMocker(time_mocker=self.time_mocker,
                                             temp_start=70,
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
        self.setup_mocks([ (0, 0.5), (60, 1.2), (300, 0.0)])
        HeatWatcher().monitor_loop(self.time_mocker.current_time() + 400)

# rst_TimeMocker
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

    def get_temperature(self):
        now = self.time_mocker.time()
        time_since_start = now - self.start_time
        # See if we need to advance to the next temperature rate
        if len(self.temp_rates) > 1 and time_since_start >= self.temp_rates[1][0]:
            self.temp_rates.pop(0)

        # The first pair is the one we use to get the rate
        rate = self.temp_rates[0][1] if len(self.temp_rates) > 0 else 0
        self.last_temp += (now - self.last_temp_time) / 60 * rate
        self.last_temp_time = now
        return self.last_temp

if __name__ == '__main__':
    unittest.main ()