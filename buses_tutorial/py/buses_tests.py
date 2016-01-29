import unittest
import math
import os

from bond import bond

from buses import get_closest_buses, select_stop_interactive, compute_lat_long_distance


class BusesTests(unittest.TestCase):



    def test_distance_computation(self):
        """
        A test to verify the distance computation.
        The data was verified manually w.r.t. http://andrew.hedges.name/experiments/haversine/
        :return:
        """
        d1 = compute_lat_long_distance(
            dict(lat=38.898, lon=-77.037),
            dict(lat=38.897, lon=-77.043)
        )
        self.assertEquals(0.330, d1)  # 0.531 km

        d2 = compute_lat_long_distance(
            dict(lat=38.898, lon=-97.030),
            dict(lat=38.890, lon=-97.044)
        )
        self.assertEquals(0.935, d2) # 1.504 km

        d3 = compute_lat_long_distance(
            dict(lat=38.958, lon=-97.038),
            dict(lat=38.890, lon=-97.044)
        )
        self.assertEquals(4.712, d3) # 7.581 km




if __name__ == '__main__':
    unittest.main()
