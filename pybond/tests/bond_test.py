import unittest
import os
import shutil

import setup_paths_test
from bond import bond, bond_helpers


def setup_bond_self_test(test_instance, spy_groups=None):
    """
    Setup Bond for self-tests
    :param spy_groups:
    :return:
    """

    # Since we use Bond both as a system-under-test and as a testing library, we
    # have to be careful with mocking. We want to mock the reconcile prompts in the
    # system under test, but not in the testing Bond.
    # We keep a variable that is set whether we are running the test. In that case,
    # the mocking works. Once the test ends, then Bond becomes the testing one.
    test_instance.during_test = True

    bond_observations_dir = os.environ.get('BOND_OBSERVATION_DIR',
                                           os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                                                        'tests',
                                                        'test_observations'))

    spy_groups = 'bond_self_test' if spy_groups is None else spy_groups
    bond.start_test(test_instance,
                    spy_groups=spy_groups,
                    observation_directory=bond_observations_dir,
                    reconcile=os.environ.get('BOND_RECONCILE', 'abort'))

    # By default we abort the test if it fails. No user-interface
    # Still even the abort reconcile wants to run a quick diff. We let it do it
    bond.deploy_agent('bond_reconcile._invoke_command',
                      cmd__startswith='diff',
                      result=bond.AGENT_RESULT_CONTINUE)

    # if we ever get to the read_console, it means that there is a problem with the test.
    # continue
    bond.deploy_agent('bond_reconcile._read_console',
                      result=bond.AGENT_RESULT_CONTINUE)

def teardown_bond_self_test(test_instance):
    test_instance.during_test = False
    # Change the settings to use a console reconcile for the end of the test
    bond.settings(reconcile=os.environ.get('BOND_RECONCILE', 'console'))

class BondTest(unittest.TestCase):


    def setUp(self):
        setup_bond_self_test(self, ())
        self.assertTrue(bond.active())

    def test_spy_basic(self):
        "A basic spy test"
        bond.spy(int_arg=1,
                 string_arg="a string",
                 bool_arg=False,
                 array_arg=[1, 2, 3, 'a string at the end'],
                 dict_arg=dict(foo=1, bar=2))
        bond.spy('there', val=10)



    def test_spy_named(self):
        "A basic spy test"
        bond.spy(spy_point_name='here',
                 int_arg=1,
                 string_arg="a string",
                 bool_arg=False,
                 array_arg=[1, 2, 3, 'a string at the end'],
                 dict_arg=dict(foo=1, bar=2))

        bond.spy('there', val=10)


    def test_spy_create_dir(self):
        "Test the creation of the observation directory"
        test_dir = '/tmp/bondTestOther'
        bond.settings(observation_directory=test_dir,
                      reconcile='accept')
        if os.path.isdir(test_dir):
            shutil.rmtree(test_dir)

        bond.spy(spy_point_name='first_observation', val=1)
        bond.spy(spy_point_name='second_observation', val=2)

        # Now spy the contents of the observation directory itself
        bond_instance = bond.Bond.instance()
        bond_instance._finish_test()  # We reach into the internal API
        bond_instance.test_framework_bridge = bond.TestFrameworkBridge.make_bridge(self) # Has to allow the test to continue

        bond.spy('collect_spy_observation_dirs',
                 directory=bond_helpers.collect_directory_contents(test_dir,
                                                                   collect_file_contents=True))
        # Now we delete the observation directory, which we already collected above.
        # Otherwise, the test will try to reconcile with it
        shutil.rmtree(test_dir)

    def test_spy_not_testing(self):
        "Trying to spy when not in testing mode"

        # Pretend the test ended
        bond_instance = bond.Bond.instance()
        old_test_framework_bridge = bond_instance.test_framework_bridge
        bond_instance.test_framework_bridge = None  # We reach into the internal API

        self.assertFalse(bond.active())
        bond.spy('first_observation', val=1)
        bond.spy('second_observation', val=2)

        # Just before the test ends, we restore current_python_test
        bond_instance.test_framework_bridge = old_test_framework_bridge  # Has to allow the test to continue

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

        # Now the spying. The result should be the modified observation
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
        self.assertEquals('a ton of fun here', bond.spy(spy_point_name='fun1', cmd="a ton of fun"))
        self.assertEquals(bond.AGENT_RESULT_NONE, bond.spy(spy_point_name='nofun', cmd="myfun2"))
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
        bond.spy(spy_point_name='fun1', cmd="myfun1")   # agent 2: only
        bond.spy(spy_point_name='nofun', cmd="myfun2")  # no agent
        bond.spy(spy_point_name='fun1', cmd="myfun3")   # agent 1: only

        self.assertSequenceEqual(['2: myfun1',
                                  '1: myfun3'], results)

    def test_skip_save_observation(self):
        "Test the ability to specify skipping saving observations and overriding it"

        bond.spy('skipped_spy_point', skip_save_observation=True, key='val')

        bond.deploy_agent('skipped_spy_point', result='Mock Value')
        ret = bond.spy('skipped_spy_point', skip_save_observation=True, key='val')
        bond.spy('skipped_return_value', val=ret)

        bond.deploy_agent('normal_spy_point', skip_save_observation=False, result='Mock Value')
        ret = bond.spy('normal_spy_point', skip_save_observation=True, key='val')
        bond.spy('not_skipped_return_value', val=ret)

        bond.deploy_agent('skipped_spy_point', skip_save_observation=True, result='Mock Value')
        ret = bond.spy('skipped_spy_point', key='val')
        bond.spy('skipped_return_value', val=ret)

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
        self.assertRaises(Exception, lambda :  bond.spy(spy_point_name='fun1', cmd="myfun2"))

    def test_no_spy_groups(self):
        # Update the settings
        bond.settings(spy_groups=None)

    def test_no_observation_directory(self):
        # Update the settings
        bond.settings(spy_groups=None, observation_directory=None)

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
        bond.spy(msg='spy groups set')
        self.annotated_method_group_enabled()
        self.annotated_method_no_group()
        bond.settings(spy_groups=())
        bond.spy(msg='spy groups reset',
                 obs_dir=os.path.basename(os.path.normpath(bond.Bond.instance()._observation_directory())))
        self.annotated_method_group_enabled()
        self.annotated_method_no_group()

    def test_custom_serializer(self):
        bond.spy(obj=CustomClass(12, 87),
                 func=lambda x: True)


class CustomClass:
    def __init__(self, arg1, args):
        self.arg1 = arg1
        self.arg2 = args

    def to_json(self):
        "Custom serializer"
        return dict(arg12=self.arg1 + self.arg2)
