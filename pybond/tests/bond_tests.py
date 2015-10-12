import unittest
import os
import shutil

import setup_paths_test
from bond import bond, bond_helpers

class BondTest(unittest.TestCase):

    @staticmethod
    def setupUpBondSelfTests(self,
                             spy_groups=None):
        """
        Setup Bond for self-tests
        :param spy_groups:
        :return:
        """
        bond_observations_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                                             'tests',
                                             'test_observations')

        # By default we abort the test if it fails. No user-interface
        bond.settings(observation_directory=bond_observations_dir,
                      merge=os.environ.get('BOND_MERGE', 'abort'))
        spy_groups = 'bond_self_test' if spy_groups is None else spy_groups
        bond.start_test(self,
                        spy_groups=spy_groups)


    def setUp(self):
        BondTest.setupUpBondSelfTests(self)
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

    def test_result(self):
        "Test the result aspect of the bond mocking"

        bond.deploy_agent('fun1',
                          cmd__contains="fun",
                          result=lambda obs: obs['cmd'] + ' here')
        bond.deploy_agent('fun1',
                          cmd__startswith="myfun",
                          result=123)
        # Now the spying
        self.assertEquals('a ton of fun here', bond.spy('fun1', cmd="a ton of fun"))
        self.assertEquals(bond.Bond.NO_MOCK_RESPONSE, bond.spy('nofun', cmd="myfun2"))
        self.assertEquals(123, bond.spy('fun1', cmd="myfun3"))


    def test_doers(self):
        "Test the doer aspect of the bond mocking"
        results = []

        bond.deploy_agent('fun1',
                          cmd__startswith="myfun",
                          do=(lambda args: results.append("1: " + args['cmd'])))
        bond.deploy_agent('fun1',
                          cmd__contains="fun",
                          do=(lambda args: results.append("2: " + args['cmd'])))
        # Now the spying
        bond.spy('fun1', cmd="myfun1")
        bond.spy('nofun', cmd="myfun2")
        bond.spy('fun1', cmd="myfun3")

        self.assertSequenceEqual(['2: myfun1', '1: myfun1',
                                  '2: myfun3', '1: myfun3'], results)

    def test_exception(self):
        "A test that throws exceptions"
        bond.deploy_agent('fun1',
                          cmd__startswith="myfun",
                          result=1,
                          exception=Exception("some exception"))
        self.assertRaises(Exception,
                          lambda :  bond.spy('fun1', cmd="myfun1"))

        bond.deploy_agent('fun1',
                          cmd="myfun2",
                          result=1,
                          exception=lambda : Exception("some exception"))
        self.assertRaises(Exception, lambda :  bond.spy('fun1', cmd="myfun2"))
