from __future__ import print_function
from functools import wraps
import inspect
import copy
import os
import json


# Special result from spy when no agent matches, or no agent provides a result
AGENT_RESULT_NONE = '_bond_agent_result_none'

# Special result from spy when an agent specifically wants the spy point
# to continue. This is useful for spy points that require an agent result
AGENT_RESULT_CONTINUE = '_bond_agent_result_continue'


# We export some function to module-level for more convenient use

def start_test(current_python_test,
               test_name=None,
               observation_directory=None,
               reconcile=None,
               spy_groups=None):
    """
    This function should be called in a ``unittest.TestCase`` before any
    of the other Bond functions can be used. This will initialize the Bond
    module for the current test, and will ensure proper cleanup of Bond
    state when the test ends, including the comparison with the
    reference observations. For example,

    .. code::

         def test_something(self):
            bond.start_test(self)
            ...

    :param current_python_test: the instance of ``unittest.TestCase`` that is running. This is the
           only mandatory parameter. Bond uses this parameter to obtain good values for
           the other optional parameters, and also to know when the test ends,
           to activate the observation comparison.
    :param test_name: (optional) the name of the test. By default, it is ``TestCase.testName``.
    :param observation_directory: (optional) the directory where the observation files are stored.
           By default this is the ``test_observations`` subdirectory in the
           directory containing the test file. The directory will be created if not present.
           You should plan to commit the
           test observations to your repository, as reference for future test runs.
    :param reconcile: (optional) the method used to reconcile the current observations with the
           saved reference observations. By default the value of the
           environment variable ``BOND_RECONCILE`` is used, or if missing, the
           default is ``abort``.

           * ``abort`` (aborts the test when there are differences)
           * ``accept`` (accepts the differences as the new reference)
           * ``console`` (show ``diff`` results and prompt at the console
             whether to accept them or not, or possibly start visual merging tools)
           * ``kdiff3`` (use kdiff3, if installed, to merge observations)

    :param spy_groups: (optional) the list, or tuple, of spy point groups that are enabled. By default,
                      enable all spy points that do not have an ``enable_for_groups``
                      attribute.


    """
    Bond.instance().start_test(current_python_test, test_name=test_name,
                               observation_directory=observation_directory,
                               reconcile=reconcile, spy_groups=spy_groups)


def settings(observation_directory=None,
             reconcile=None,
             spy_groups=None):
    """
    Override settings that were set in :py:func:`start_test`. Only apply for the duration
    of a test, so this should be called after :py:func:`start_test`. This
    is useful if you set general test parameters with :py:func:`start_test` in a ``setUp()`` block,
    but want to override them for some specific tests.

    :param observation_directory: (optional) the directory where the observation files are stored.
           By default this is the ``test_observations`` subdirectory in the
           directory containing the test file. The directory will be created if not present.
           You should plan to commit the
           test observations to your repository, as reference for future test runs.
    :param reconcile: (optional) the method used to reconcile the current observations with the
           saved reference observations. By default the value of the
           environment variable ``BOND_RECONCILE`` is used, or if missing, the
           default is ``abort``.

           * ``abort`` (aborts the test when there are differences)
           * ``accept`` (accepts the differences as the new reference)
           * ``console`` (show ``diff`` results and prompt at the console
             whether to accept them or not, or possibly start visual merging tools)
           * ``kdiff3`` (use kdiff3, if installed, to merge observations)

    :param spy_groups: (optional) the list, or tuple, of spy point groups that are enabled. By default,
                      enable all spy points that do not have an ``enable_for_groups``
                      attribute.

    """
    Bond.instance().settings(observation_directory=observation_directory,
                             reconcile=reconcile,
                             spy_groups=spy_groups)


def active():
    """
    This function can be called to find out if a ``bond.start_test`` is currently active.
    For example,

    .. code::

         if bond.active():
            ..do something..
    """
    return Bond.instance().active()

def spy(spy_point_name=None, **kwargs):
    """
    This is the most frequently used Bond function. It will collect the key-value pairs passed
    in the argument list and will emit them to the spy observation log.

    If you are not during testing (:py:func:`start_test` has not been called) then
    this function does not do anything.

    If there is an agent deployed for the current spy point (see :py:func:`deploy_agent`),
    it will process the agent.

    .. code::

         bond.spy(file_name=file_name, content=data)
         bond.spy(spy_point_name="other spy", args=args, output=output)

    The values are formatted to JSON using the json module, with sorted keys, and indentation, with
    one value per line, to streamline the observation comparison.
    For user-defined classes, the method ``to_json`` is called on the instance before it is formatted.
    This method should return a JSON-serializable data structure.

    If you have deployed agents (see :py:func:`deploy_agent`) that are applicable to this spy point,
    the agents can specify a
    ``formatter`` that can intervene to modify the observation dictionary before it is
    serialized to JSON.

    :param spy_point_name: (optional) the spy point name, useful to distinguish among different observations, and to
           select the agents that are applicable to this spy point. There is no need for this value to
           be unique in your test. You only need to have this value if you want to :py:func:`deploy_agent` for
           this spy point later in your test. If you do use this parameter, then it will be observed
           with the key ``__spy_point__`` to ensure that it appears first in the sorted observation.

    :param kwargs: key-value pairs to be observed. This forms the observation dictionary that is
           serialized as the current observation.

    :return: the result from the agent, if any (see :py:func:`deploy_agent`), or ``bond.AGENT_RESULT_NONE``.
    """
    return Bond.instance().spy(spy_point_name=spy_point_name, **kwargs)


def deploy_agent(spy_point_name, **kwargs):
    """
    Create and deploy a new agent for the named spy point. When a spy point is encountered, the agents are searched
    in reverse order of their deployment, and the first agent that matches is used.

    .. code::

        bond.deploy_agent("my file", file_name__contains='passwd',
                          result="mock result")


    :param spy_point_name: (mandatory) the spy point where the agent is deployed.
    :param kwargs: (optional) key-value pairs that control whether the agent is active and what it does.
         The following keys are recognized:

        * Keys that restrict for which invocations of bond.spy this agent is active. All of these conditions
          must be true for the agent to be the active one:

          * key=val : only when the observation dictionary contains the 'key' with the given value
          * key__contains=substr : only when the observation dictionary contains the 'key' with a string value
            that contains the given substr.
          * key__startswith=substr : only when the observation dictionary contains the 'key' with a
            string value that starts with the given substr.
          * key__endswith=substr : only when the observation dictionary contains the 'key' with a string value
            that ends with the given substr.
          * filter=func : only when the given func returns true when passed observation dictionary.
            The function should not make changes to the observation dictionary.
            Uses the observation before formatting.

        * Keys that control what the observer does when processed:

          * do=func : executes the given function with the observation dictionary.
            func can also be a list of functions, executed in order.
            The function should not make changes to the observation dictionary.
            Uses the observation before formatting.

        * Keys that control what the corresponding spy returns (by default ``AGENT_RESULT_NONE``):

          * exception=x : the call to bond.spy throws the given exception. If 'x' is a function
            it is invoked on the observation dictionary to compute the exception to throw.
            The function should not make changes to the observation dictionary.
            Uses the observation before formatting.
          * result=x : the call to bond.spy returns the given value. If 'x' is a function
            it is invoked on the observe argument dictionary to compute the value to return.
            If the function throws an exception then the spied function thrown an exception.
            The function should not make changes to the observation dictionary.
            Uses the observation before formatting.

        * Keys that control how the observation is saved. This is processed after all the above functions.

          * formatter : if specified, a function that is given the observation and can update it in place.
            The formatted observation is what gets serialized and saved.

    :return: nothing
    """
    Bond.instance().deploy_agent(spy_point_name, **kwargs)


def spy_point(spy_point_name=None,
              enabled_for_groups=None,
              require_agent_result=False,
              excluded_keys=('self',),
              spy_result=False):
    """
    Function and method decorator for spying arguments and results of methods. This decorator is safe
    to use on production code. It will have effects only if the function :py:func:`start_test` has
    been called to initialize the Bond module.

    Must be applied directly to a method or a function, not to another decorator.

    .. code::

        @staticmethod
        @bond.spy_point()
        def my_sneaky_function(arg1='', arg2=None):
            # does something

    :param spy_point_name: (optional) A name to use for this spy point. Default is obtained from the name
                           of the decorated function: for module methods, `module.method_name`. For other
                           methods, `ClassName.method_name`.
    :param enabled_for_groups: (optional) A list or tuple of spy point groups to which this spy point belongs.
                           If missing then it is enabled for all groups. These names are arbitrary labels
                           that :py:func:`start_test` can use to turn off groups of spy points.
                           If you are writing a library that others are using, you should use a distinctive
                           spy group for your spy points, to avoid your library starting to spy if embedded
                           in some other test using Bond.
    :param require_agent_result: (optional) if True, and if this spy point is enabled, then there must be an
                           agent that provides a result, or else the invocation of the function aborts.
                           The agent may still provide ``AGENT_RESULT_CONTINUE`` to tell the spy point
                           to continue the invocation of the underlying function. This parameter is
                           used to mark functions that should not be invoked normally during testing, e.g.,
                           invoking shell commands, or requesting user input.
    :param excluded_keys: (optional) a tuple or list of parameter key names to skip when saving the observations.
                         Further manipulation of what gets observed can be done from agents.
    :param spy_result: (optional) if True, then the result value is spied also, using a spy_point name of
                       `spy_point_name.result`. If there is an agent providing a result for
                       this spy point, then the agent result is saved as the observation.
    """
    # TODO: Should we also have an excluded_from_groups parameter?
    # TODO right now excluding 'self' using excludedKeys, should attempt to find a better way?
    def wrap(fn):
        # TODO: we get an error here if we do not specify spy_point_name and this is a staticmethod
        # TODO: This is if we have @spy_point() @staticmethod
        # ERIK: The @staticmethod should appear above @spy_point and that solves this issue

        # We have as little code here as possible, because this runs in production code
        # ^ not if we use the try/except on import idiom. But good to still work if bond is imported
        if not inspect.isfunction(fn):
            raise TypeError('The observeFunction decorator may only be applied to functions/methods!')

        # Convert enabled_for_groups into a tuple
        if enabled_for_groups is None:
            enabled_for_groups_local = None
        elif isinstance(enabled_for_groups, basestring):
            enabled_for_groups_local = (enabled_for_groups,)
        else:
            assert isinstance(enabled_for_groups, (list, tuple))
            enabled_for_groups_local = enabled_for_groups

        @wraps(fn)
        def fn_wrapper(*args, **kwargs):
            # Bypass spying if we are not TESTING
            if not active():
                return fn(*args, **kwargs)
            the_bond = Bond.instance()
            if enabled_for_groups_local is not None:
                for grp in enabled_for_groups_local:
                    if grp in the_bond.spy_groups:
                        break
                else:
                    # We are only enabled for some groups, but none of those and active
                    return fn(*args, **kwargs)

            arginfo = inspect.getargspec(fn)
            # TODO ETK this should be better: callargs = inspect.getcallargs(fn, *args, **kwargs)
            if spy_point_name is None:
                # We recognize instance methods by the first argument 'self'
                # TODO: there must be a better way to do this
                spy_point_name_local = None
                if arginfo and arginfo[0]:
                    if arginfo[0][0] == 'self':
                        spy_point_name_local = args[0].__class__.__name__ + '.' + fn.__name__
                    elif arginfo[0][0] == 'cls':
                        # A class method
                        spy_point_name_local = args[0].__name__ + '.' + fn.__name__
                if spy_point_name_local is None:
                    # TODO We get here both for staticmethod and for module-level functions
                    # If we had the spy_point wrapper outside the @staticmethod we could tell
                    # more easily what kind of method this was !!
                    module_name = getattr(fn, '__module__')
                    if module_name == '__main__':  # Get the original module name from the filename
                        module_name = os.path.splitext(os.path.basename(inspect.getmodule(fn).__file__))[0]
                    # Keep only the last component of the name
                    module_name = module_name.split('.')[-1]
                    spy_point_name_local = module_name + '.' + fn.__name__
            else:
                spy_point_name_local = spy_point_name

            observation_dictionary = {}
            for idx, arg in enumerate(args):
                observation_dictionary[arginfo.args[idx]] = arg
            for key, val in kwargs.iteritems():
                observation_dictionary[key] = val
            observation_dictionary = {key: val for (key, val) in observation_dictionary.iteritems()
                                      if key not in excluded_keys}

            response = the_bond.spy(spy_point_name=spy_point_name_local,
                                    **observation_dictionary)
            if require_agent_result:
                assert response is not AGENT_RESULT_NONE, \
                    'You MUST mock out spy_point {}: {}'.format(spy_point_name_local,
                                                                repr(observation_dictionary))
            if response is AGENT_RESULT_NONE or response is AGENT_RESULT_CONTINUE:
                return_val = fn(*args, **kwargs)
            else:
                return_val = response

            if spy_result:
                the_bond.spy(spy_point_name_local + '.result', result=return_val)
            return return_val

        return fn_wrapper

    return wrap


class Bond:
    DEFAULT_OBSERVATION_DIRECTORY = '/tmp/bond_observations'

    _instance = None

    @staticmethod
    def instance():
        if Bond._instance is None:
            Bond._instance = Bond()
        return Bond._instance

    def __init__(self):
        self._settings = {}

        self.test_framework_bridge = None
        self.start_count_failures = None
        self.start_count_errors = None
        self.test_name = None
        self.spy_groups = None  # Map indexed on enabled spy groups
        self.observations = []  # Here we will collect the observations
        self.spy_agents = {}  # Map from spy_point_name to SpyAgents

    def settings(self, **kwargs):
        """
        Set the settings for Bond.
        See documentation for top-level settings function
        :param kwargs:
        :return:
        """
        # Get the not-None keys
        for k, v in kwargs.iteritems():
            self._settings[k] = v
        if 'spy_groups' in self._settings:
            self._set_spy_groups(self._settings['spy_groups'])

    def start_test(self,
                   current_python_test,
                   **kwargs):
        """
        Signal the starting of a new test.
        See documentation for top-level start_test function
        :param kwargs:
        :return:
        """

        self.observations = []
        self.spy_agents = {}
        self.spy_groups = {}
        self.test_framework_bridge = TestFrameworkBridge.make_bridge(current_python_test)

        self._settings = {}  # Clear settings before each test
        self.settings(**kwargs)

        self.test_name = (self._settings.get('test_name') or
                          self.test_framework_bridge.full_test_name())

        # Register us on test exit
        self.test_framework_bridge.on_finish_test(self._finish_test)

        if 'observation_directory' not in self._settings:
            print('WARNING: you should set the settings(observation_directory). Observations saved to {}'.format(
                Bond.DEFAULT_OBSERVATION_DIRECTORY
            ))

    def active(self):
        return (self.test_framework_bridge is not None)

    def spy(self, spy_point_name=None, **kwargs):
        if not self.test_framework_bridge:
            # Don't do anything if we are not testing
            return None

        if spy_point_name is not None:
            assert isinstance(spy_point_name, basestring), "spy_point_name must be a string"

            # Find the agent to apply. We process the agents in order, because they are deployed at the start of the list
            active_agent = None

            for agent in self.spy_agents.get(spy_point_name, []):
                if not agent.filter(kwargs):
                    continue
                active_agent = agent
                break
        else:
            active_agent = None

        observation = copy.deepcopy(kwargs)
        if spy_point_name is not None:
            observation['__spy_point__'] = spy_point_name  # Use a key that should come first alphabetically

        def save_observation():
            # We postpone applying the formatter until we have run the "doer" and the "result"
            formatted = self._format_observation(observation,
                                                 active_agent=active_agent)
            print("Observing: " + formatted + "\n")
            self.observations.append(formatted)

        # Apply the doer if present
        try:
            res = AGENT_RESULT_NONE

            if active_agent is not None:
                active_agent.do(observation)

                res = active_agent.result(observation)  # This may throw an exception
        finally:
            save_observation()

        if res != AGENT_RESULT_NONE:
            print("   Result " + repr(res))
            return res

        return AGENT_RESULT_NONE

    def deploy_agent(self, spy_point_name, **kwargs):
        """
        Deploy an agent for a spy point.
        See documentation for the top-level deploy_agent function.
        :param spy_point_name:
        :param kwargs:
        :return:
        """
        assert self.test_framework_bridge, "Should not call deploy_agent unless you have called start_test first"
        assert isinstance(spy_point_name, basestring), "spy_point_name must be a string"

        agent = SpyAgent(spy_point_name, **kwargs)
        spy_agent_list = self.spy_agents.get(spy_point_name)
        if spy_agent_list is None:
            spy_agent_list = []
            self.spy_agents[spy_point_name] = spy_agent_list
        # add the agent at the start of the list
        spy_agent_list.insert(0, agent)

    def _set_spy_groups(self, spy_groups):
        self.spy_groups = {}
        if spy_groups is not None:
            if isinstance(spy_groups, basestring):
                self.spy_groups = {spy_groups: True}
            else:
                assert isinstance(spy_groups, (list, tuple))

                for sg in spy_groups:
                    assert isinstance(sg, basestring)
                    self.spy_groups[sg] = True

    def _format_observation(self,
                            observation,
                            active_agent=None):
        # TODO: I do not quite like how formatters work. See issue #1
        if active_agent:
            active_agent.formatter(observation)

        return json.dumps(observation,
                          sort_keys=True,
                          indent=4,
                          default=self._custom_json_serializer)

    def _custom_json_serializer(self, obj):
        # TODO: figure out how to do this. Must be customizable from settings
        if 'to_json' in obj.__class__.__dict__:
            return obj.__class__.to_json(obj)
        if hasattr(obj, 'to_json'):
            return obj.to_json()
        if type(obj) == type(lambda : 0):
            return "\"<lambda>\""


    def _finish_test(self):
        """
        Called internally when a test ends
        :return:
        """
        try:
            # Were there failures and errors in this test?
            test_failed = self.test_framework_bridge.test_failed()
            # Save the observations
            if test_failed:
                # Show the failures and errors now
                print(test_failed)
                no_save = test_failed
            else:
                no_save = None

            fname = self._observation_file_name()
            fdir = os.path.dirname(fname)
            if not os.path.isdir(fdir):
                os.makedirs(fdir)
                top_git_ignore = os.path.join(self._observation_directory(), '.gitignore')
                # TODO: This should be configurable, you may not use git
                if not os.path.isfile(top_git_ignore):
                    # Add the .gitignore file
                    with open(top_git_ignore, 'w') as f:
                        f.write("*_now.json\n*.diff\n")

            reference_file = fname + '.json'
            current_file = fname + '_now.json'

            if os.path.isfile(current_file):
                os.unlink(current_file)
            self._save_observations(current_file)

            # WE have to reconcile them
            reconcile_res = self._reconcile_observations(reference_file, current_file, no_save=no_save)

            if not test_failed:
                # If the test did not fail already, but it failed reconcile, fail the test
                assert reconcile_res, 'Reconciling observations for {}'.format(self.test_name)
        finally:
            # Mark that we are outside of a test
            self.test_framework_bridge = None
        pass

    def _observation_file_name(self):
        fname = os.path.join(*[self._observation_directory()] +
                              self.test_name.split('.'))
        return fname

    def _observation_directory(self):
        obs_dir = self._settings.get('observation_directory')
        if obs_dir is not None:
            return obs_dir

        # We build the observation directory based on the path of the current test file
        test_file = self.test_framework_bridge.test_file_name()
        if test_file:
            return os.path.join(os.path.dirname(test_file),
                                'test_observations')
        print("WARNING: Using temporary directory for Bond test observations. "
              "Use observation_directory parameter to start_test or settings")
        return Bond.DEFAULT_OBSERVATION_DIRECTORY

    def _save_observations(self, file_name):
        remaining = len(self.observations)
        with open(file_name, 'w') as f:
            f.write('[\n')
            for obs in self.observations:
                f.write(obs)
                remaining -= 1
                if remaining > 0:
                    f.write(',\n')
                else:
                    f.write('\n')
            f.write(']\n')

    def _reconcile_observations(self,
                                reference_file,
                                current_file,
                                no_save=None):
        settings = dict(reconcile=self._settings.get('reconcile'))
        return bond_reconcile.reconcile_observations(settings,
                                                     test_name=self.test_name,
                                                     reference_file=reference_file,
                                                     current_file=current_file,
                                                     no_save=no_save)


class SpyAgent:
    """
    A spy agent applies to a particular spy_point_name, has
    some optional filters to select only certain observations,
    and has optional mocking parameters.
    See documentation for the deploy_agent top-level function.
    """

    def __init__(self, spy_point_name, **kwargs):
        self.spy_point_name = spy_point_name
        self.result_spec = AGENT_RESULT_NONE
        self.exception_spec = None
        self.formatter_spec = None
        self.doers = []  # A list of things to do
        self.point_filter = None  # The filter for pointName, if present
        self.filters = []  # The generic filters

        for k in kwargs:
            if k == 'result':
                self.result_spec = kwargs['result']
            elif k == 'exception':
                self.exception_spec = kwargs['exception']
            elif k == 'formatter':
                self.formatter_spec = kwargs['formatter']
            elif k == 'do':
                doers = kwargs[k]
                if isinstance(doers, list):
                    self.doers += doers
                else:
                    self.doers.append(doers)
            else:
                # Must be a filter
                fo = SpyAgentFilter(k, kwargs[k])
                self.filters.append(fo)

    def filter(self, observation):
        """
        Run the filter on an observation to see if the SpyAgent applies
        :param observation:
        :return: True, if the
        """
        for f in self.filters:
            if not f.filter(observation):
                return False
        return True

    def formatter(self, observation):
        """Apply the formatter to modify the observation in place"""
        if self.formatter_spec is not None:
            self.formatter_spec(observation)

    def do(self, observation):
        for d in self.doers:
            d(observation)

    def result(self, observation):
        """Compute the result"""
        es = self.exception_spec
        if es is not None:
            if hasattr(es, '__call__'):
                raise es(observation)
            else:
                raise es

        r = self.result_spec
        if r is not AGENT_RESULT_NONE and hasattr(r, '__call__'):
            return r(observation)
        else:
            return r


class SpyAgentFilter:
    """
    Each SpyAgent can have multiple filters.
    See documentation for deploy_agent function.
    """

    def __init__(self, filter_key, filter_value):
        self.field_name = None  # The observation field name the filter applies to
        self.filter_func = None  # A filter function (applies to the field value)
        if filter_key == 'filter':
            assert isinstance(filter_value, type(lambda: 0))
            self.field_name = None
            self.filter_func = filter_value
            return

        parts = filter_key.split("__")
        if len(parts) == 1:
            self.field_name = parts[0]
            self.filter_func = (lambda f: f == filter_value)
        elif len(parts) == 2:
            self.field_name = parts[0]
            cmp_spec = parts[1]
            if cmp_spec == 'exact':
                self.filter_func = (lambda f: f == filter_value)
            elif cmp_spec == 'eq':
                self.filter_func = (lambda f: f == filter_value)
            elif cmp_spec == 'startswith':
                self.filter_func = (lambda f: f.find(filter_value) == 0)
            elif cmp_spec == 'endswith':
                self.filter_func = (lambda f: f.rfind(filter_value) == len(f) - len(filter_value))
            elif cmp_spec == 'contains':
                self.filter_func = (lambda f: filter_value in f)
            else:
                assert False, "Unknown operator: " + cmp_spec
        else:
            assert False

    def filter(self, observation):
        if self.field_name:
            return self.field_name in observation and self.filter_func(observation[self.field_name])
        else:
            return self.filter_func(observation)


class TestFrameworkBridge:
    """
    A class to abstract the interface to the host test framework
    """
    def __init__(self,
                 current_python_test):
        self.current_python_test = current_python_test

    @staticmethod
    def make_bridge(current_python_test):
        """
        Make the proper bridge for the current python test
        :param current_python_test:
        :return:
        """

        # We test for the presence of fields
        if hasattr(current_python_test, '_resultForDoCleanups'):
            resultForDoCleanups = current_python_test._resultForDoCleanups
            if (hasattr(resultForDoCleanups, 'failures') and
                hasattr(resultForDoCleanups, 'errors')):

                return TestFrameworkBridgeUnittest(current_python_test)

            if hasattr(resultForDoCleanups, '_fixtureinfo'):
                return TestFrameworkBridgePyTest(current_python_test)

        assert False, "Can't recognize the test framework"

    def full_test_name(self):
        """
        The full name of the test: Class.test
        """
        return self.current_python_test.__class__.__name__ + "." + self.current_python_test._testMethodName

    def test_file_name(self):
        """
        The name of the .py file where the test is defined
        :return:
        """
        return inspect.getfile(self.current_python_test.__class__)

    def on_finish_test(self, _callback):
        """
        Register a callback to be called on test end
        """
        self.current_python_test.addCleanup(_callback)

    def test_failed(self):
        """
        Return an error message if the test has failed
        :return:
        """
        assert False, "Must override"


class TestFrameworkBridgeUnittest(TestFrameworkBridge):
    """
    A bridge for the standard unitest
    """
    def __init__(self,
                 current_python_test):
        TestFrameworkBridge.__init__(self, current_python_test)

        # TODO: the rest is specific to unittest. We need to factor it out to allow other frameworks. See issue #2
        # (the use of current_python_test._testMethodName above is unittest specific as well)

        # We remember the start counter for failures and errors
        # This is the best way I know how to tell that a test has failed
        self.start_count_failures = len(self.current_python_test._resultForDoCleanups.failures)
        self.start_count_errors = len(self.current_python_test._resultForDoCleanups.errors)


    def test_failed(self):
        """
        Return true if the test has failed
        :return:
        """
        failures_and_errors = self._get_failures_and_errors()
        if failures_and_errors:
            return "\n".join(failures_and_errors)
        else:
            return None


    def _get_failures_and_errors(self):
        """
        Return a list of failures and errors so far
        """
        res = []
        for fmsg in self.current_python_test._resultForDoCleanups.failures[self.start_count_failures:]:
            res.append(fmsg[1])

        for emsg in self.current_python_test._resultForDoCleanups.errors[self.start_count_errors:]:
            res.append(emsg[1])
        return res


class TestFrameworkBridgePyTest(TestFrameworkBridge):
    """
    A bridge for py.test
    """
    def __init__(self,
                 current_python_test):
        TestFrameworkBridge.__init__(self, current_python_test)
        self.start_count_excinfo = self._count_excinfo()

    def test_failed(self):
        """
        Return true if the test has failed
        :return:
        """
        return self._count_excinfo() > self.start_count_excinfo


    def _count_excinfo(self):
        return len(self.current_python_test._resultForDoCleanups._excinfo) \
                     if self.current_python_test._resultForDoCleanups._excinfo else 0

# Import bond_reconcile at the end, because we import bond from bond_reconcile, and
# we need at least the spy_point to be defined
import bond_reconcile
