import unittest
import os
import shutil

import setup_paths_test
from bond import bond, bond_helpers


class BondTest(unittest.TestCase):

    @staticmethod
    def setup_bond_self_tests(test_instance, spy_groups=None):
        """
        Setup Bond for self-tests
        :param spy_groups:
        :return:
        """
        bond_observations_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                                             'tests',
                                             'test_observations')

        spy_groups = 'bond_self_test' if spy_groups is None else spy_groups
        bond.start_test(test_instance,
                        spy_groups=spy_groups,
                        observation_directory=bond_observations_dir,
                        merge=os.environ.get('BOND_MERGE', 'abort'))
        # By default we abort the test if it fails. No user-interface

    def setUp(self):
        BondTest.setup_bond_self_tests(self)
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


    def test_formatter(self):
        "Apply the formatter"

        def my_formatter(obs):
            obs['new_key'] = 'new_value'
        bond.deploy_agent('fun1',
                          cmd__contains="fun",
                          formatter=my_formatter)
        bond.deploy_agent('fun1',
                          cmd__startswith="myfun",
                          result=lambda obs: obs)

        # Now the spying. Theresult should be the modified observation
        bond.spy('spy result', res=bond.spy('fun1', cmd="myfun3"))

    def test_result(self):
        "Test the result aspect of the bond mocking"
        def my_formatter(obs):
            obs['cmd'] += ' : formatted'

        # The formatter is processed after the result is computed
        bond.deploy_agent('fun1',
                          cmd__contains="fun",
                          formatter=my_formatter,
                          result=lambda obs: obs['cmd'] + ' here')
        bond.deploy_agent('fun1',
                          cmd__startswith="myfun",
                          result=123)
        # Now the spying
        self.assertEquals('a ton of fun here', bond.spy('fun1', cmd="a ton of fun"))
        self.assertEquals(bond.AGENT_RESULT_NONE, bond.spy('nofun', cmd="myfun2"))
        self.assertEquals(123, bond.spy('fun1', cmd="myfun3"))


    def test_doers(self):
        "Test the doer aspect of the bond mocking"
        results = []
        def my_formatter(obs):
            obs['cmd'] += ' : formatted'

        bond.deploy_agent('fun1',
                          cmd__startswith="myfun",
                          formatter=my_formatter,
                          do=(lambda args: results.append("1: " + args['cmd'])))
        bond.deploy_agent('fun1',
                          cmd__contains="fun1",
                          do=(lambda args: results.append("2: " + args['cmd'])))
        # Now the spying
        bond.spy('fun1', cmd="myfun1")   # agent 2: only
        bond.spy('nofun', cmd="myfun2")  # no agent
        bond.spy('fun1', cmd="myfun3")   # agent 1: only

        self.assertSequenceEqual(['2: myfun1',
                                  '1: myfun3'], results)

    def test_exception(self):
        "A test that throws exceptions"
        def my_formatter(obs):
            obs['cmd'] += ' : formatted'

        bond.deploy_agent('fun1',
                          cmd__startswith="myfun",
                          result=1,
                          formatter=my_formatter,
                          exception=Exception("some exception"))
        self.assertRaises(Exception,
                          lambda :  bond.spy('fun1', cmd="myfun1"))

        bond.deploy_agent('fun1',
                          cmd="myfun2",
                          result=1,
                          formatter=my_formatter,
                          exception=lambda obs: Exception("some exception: "+obs['cmd']))
        self.assertRaises(Exception, lambda :  bond.spy('fun1', cmd="myfun2"))


    def test_no_spy_groups(self):
        # For this test use a separate instance of Bond
        my_bond = bond.Bond()
        my_bond.start_test(self, observation_directory='/tmp/bondObs')  # Start the test without spy_groups

    def test_no_observation_directory(self):
        # For this test use a separate instance of Bond
        my_bond = bond.Bond()
        my_bond.start_test(self)  # Start the test without spy_groups or observation dir

    @bond.spy_point(enabled_for_groups='group2')
    def annotated_method_group_enabled(self):
        return 'return value'

    @bond.spy_point()
    def annotated_method_no_group(self):
        return 'return value'

    def test_reset_spy_groups(self):
        self.annotated_method_group_enabled()
        self.annotated_method_no_group()
        bond.settings(spy_groups='group2')
        bond.spy('spy groups set')
        self.annotated_method_group_enabled()
        self.annotated_method_no_group()
        bond.settings(spy_groups=())
        bond.spy('spy groups reset',
                 obs_dir=os.path.basename(os.path.normpath(bond.Bond.instance()._observation_directory())))
        self.annotated_method_group_enabled()
        self.annotated_method_no_group()
