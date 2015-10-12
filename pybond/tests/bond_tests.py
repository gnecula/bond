import unittest
import os
import shutil

import setup_paths_test
from bond import bond, bond_helpers

class BondTest(unittest.TestCase):

    @staticmethod
    def setupUpBond(self):
        if not os.path.isdir('/tmp/bondTest'):
            os.makedirs('/tmp/bondTest', 0777)
        # By default we abort the test if it fails. No user-interface
        bond.settings(observation_directory="/tmp/bondTest",
                      merge=os.environ.get('BOND_MERGE', 'abort'))
        bond.start_test(self)


    def setUp(self):
        BondTest.setupUpBond(self)
        self.assertTrue(bond.TESTING)

    def test_spy_basic(self):
        "A basic spy test"
        bond.spy('here',
                 int_arg=1,
                 string_arg="a string",
                 bool_arg=False,
                 array_arg=[1, 2, 3, 'a string at the end'],
                 dict_arg=dict(foo=1, bar=2))

        bond.spy('there', val=10)

    def test_spy_create_dir(self):
        "Test the creation of the observation directory"
        test_dir = '/tmp/bondTestOther'
        bond.settings(observation_directory=test_dir)
        if os.path.isdir(test_dir):
            shutil.rmtree(test_dir)

        bond.spy('first_observation', val=1)
        bond.spy('second_observation', val=2)

        # Now spy the contents of the observation directory itself
        bond_instance = bond.Bond.instance()
        bond_instance._finish_test()  # We reach into the internal API
        bond_instance.current_python_test = self # Hask to allow the test to continue

        bond.spy('collect_spy_observation_dirs',
                 directory=bond_helpers.collect_directory_contents(test_dir,
                                                                   collect_file_contents=True))
        # Now we delete the observation director, which we already collected above.
        # Otherwise, the test will try to reconcile with it
        shutil.rmtree(test_dir)



