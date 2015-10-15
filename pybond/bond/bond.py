from __future__ import print_function
from functools import wraps
import inspect
import copy
import os
import json


# We use this global to signal that we are in Bond spying mode, i.e., start_test has been
# called.
TESTING = False

# Special result from spy when no agent matches, or no agent provides a result
AGENT_RESULT_NONE = '_bond_agent_result_none'

# Special result from spy when an agent specifically wants the spy point
# to continue. This is useful for spy points that require an agent result
AGENT_RESULT_CONTINUE = '_bond_agent_result_continue'


# We export some function to module-level for more convenient use

def start_test(current_python_test,
               test_name=None,
               observation_directory=None,
               merge=None,
               spy_groups=None):
    """
    This function should be called in a ``unittest.TestCase`` before any
    of the other Bond functions can be used. This will initialize the Bond
    module for the current test, and will ensure proper cleanup of Bond
    state when the test ends, including the comparison with the
    reference observations.

    :param current_python_test: the instance of ``unittest.TestCase`` that is running
    :param test_name: the name of the test. By default, it is ``TestCase.testName``.
    :param observation_directory: the directory where the observation files are stored.
           By default this is the ``test_observations`` subdirectory in the
           directory containing the test file.
    :param merge: the method used to merge the current observations with the
           saved reference observations. Possible values are

           * ``abort`` (aborts the test when there are differences)
           * ``accept`` (accepts the differences as the new reference)
           * ``console`` (show ``diff`` results and prompt at the console
             whether to accept them or not, or possibly start visual merging tools)
           * ``kdiff3`` (use kdiff3, if installed, to merge observations)

    :param spy_groups: the list of spy point groups that are enabled. By default,
                      enable all spy points that do not have an enable_for_groups
                      attribute.
    """
    Bond.instance().start_test(current_python_test, test_name=test_name,
                               observation_directory=observation_directory,
                               merge=merge, spy_groups=spy_groups)


def settings(observation_directory=None,
             merge=None,
             spy_groups=None):
    """
    Override settings that were set in :py:func:`start_test`. Only apply for the duration
    of a test, so this should be called after :py:func:`start_test`. This
    is useful if you set general test parameters with :py:func:`start_test` in a ``setUp()`` block,
    but want to override them for some specific tests.

    :param observation_directory: the directory where the observation files are stored.
           By default this is the ``test_observations`` subdirectory in the
           directory containing the test file.
    :param merge: the method used to merge the current observations with the
           saved reference observations. Possible values are

           * ``abort`` (aborts the test when there are differences)
           * ``accept`` (accepts the differences as the new reference)
           * ``console`` (show ``diff`` results and prompt at the console
             whether to accept them or not, or possibly start visual merging tools)
           * ``kdiff3`` (use kdiff3, if installed, to merge observations)

    :param spy_groups: the list of spy point groups that are enabled. By default,
                      enable all spy points that do not have an enable_for_groups
                      attribute.

    If any parameter is not present here, the previous value will still apply
    """
    Bond.instance().settings(observation_directory=observation_directory,
                             merge=merge,
                             spy_groups=spy_groups)


def spy(spy_point_name, **kwargs):
    """
    This is the most frequently used Bond function. It will collect the key-value pairs passed
    in the argument list and will emit them to the spy observation log.
    If there is an agent registered for the current spy point (see :py:func:`deploy_agent`),
    it will process the agent.

    The values are formatted to JSON using the json module, with sorted keys, and indentation, with
    one value per line, to streamline the observation comparison.
    For user-defined classes, the method toJSON is called on the instance before it is formatted.
    This method should return a JSON-serializable data structure.

    If the special key "formatter" is present, its value must be a function that is used on a copy of
    the arguments to produce a formatted dictionary.

    :param spyPointName: the spy point name, useful to distinguish among different observations
    :param kwargs: key-value pairs to be observed. There is a special key name:

    :return: the result from the agent, if any (see :py:func:`deploy_agent`)
    """
    return Bond.instance().spy(spy_point_name, **kwargs)


def deploy_agent(spy_point_name, **kwargs):
    """
    Create a new agent for the named spy point. When a spy point is encountered, the agents are searched
    in reverse order of their deployment, and the first agent that matches is used.

    :param spy_point_name: the point for which to create the observer. This observer will only be executed
      for invocations of Bond.Observe with the the same spyPointName.
    :param kwargs: key-value pairs that control the execution of the observer. The following keys are
      recognized:

        * Keys that restrict for which invocations of bond.spy this agent is active. All of these conditions
          must be true for the agent to be the active one:

          * key=val : only when the observation dictionary contains the 'key' with the given value
          * key__contains=substr : only when the observation dictionary contains the 'key' with a string value
            that contains the given substr
          * key__startswith=substr : only when the observation dictionary contains the 'key' with a
            string value that starts with the given substr
          * key__endswith=substr : only when the observation dictionary contains the 'key' with a string value
            that ends with the given substr
          * filter=func : only when the given func returns true when passed observation dictionary.
            Uses the observation before formatting.

        * Keys that control what the observer does when processed:

          * do=func : executes the given function with the observed argument dictionary.
            func can also be a list of functions, executed in order.
            Uses the observation before formatting.

        * Keys that control what the corresponding spy returns (by default ``AGENT_RESULT_NONE``):

          * exception=x : the call to bond.spy throws the given exception. If 'x' is a function
            it is invoked on the observe argument dictionary to compute the exception to throw.
            Uses the observation before formatting.
          * result=x : the call to bond.spy returns the given value. If 'x' is a function
            it is invoked on the observe argument dictionary to compute the value to return.
            Uses the observation before formatting.

        * Keys that control how the observation is logged. This is processed after all the above functions.

          * formatter : if specified, a function that is given the observation and can update it in place.
            The formatted observation is what gets saved.

    :return: either ``AGENT_RESULT_NONE`` if no agent matches or contains a "result", or the result from
      the first agent that matches.

    """
    Bond.instance().deploy_agent(spy_point_name, **kwargs)


# Dependency injection for mocking out bond for testing?
# TODO right now excluding 'self' using excludedKeys, should attempt to find a better way?
def spy_point(spy_point_name=None,
              enabled_for_groups=None,
              require_agent_result=False,
              excluded_keys=('self',),
              spy_result=False):
    """
    Decorator for marking Bond spy points.
    Must be applied directly to a method or a function, not to another decorator.

    :param spy_point_name: An optional name to use for this spy point. Default is obtained from the name
                           of the decorated function.
    :param enabled_for_groups: An optional list or tuple of spy point groups to which this spy point belongs.
                           If missing then it is enabled for all groups.
    :param require_agent_result: if True, and if this spy point is enabled, then there must be an
                           agent that provides a result, or else the invocation of the function aborts.
                           The agent may still provide ``AGENT_RESULT_CONTINUE`` to tell the spy point
                           to continue the invocation of the underlying function.
    :param excluded_keys: a tuple or list of parameter key names to skip when saving the observations.
    :param spy_result: if True, then the resuly value is spied also, using a spy_point name of
                       `spy_point_name.result`. If there is an agent providing a result for
                       this spy point, then the agent result is saved as the observation.
    """
    # TODO: Should we also have an excluded_from_groups parameter?

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
            if not TESTING:
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
            # print arginfo

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

            response = the_bond.spy(spy_point_name_local, **observation_dictionary)
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
        self.pointReturns = {}

        # The remaining parameters are per-test
        # TODO: maybe we do not need a singleton, and we can create a Bond
        # instance for each test ?
        # ERIK: That might make more sense, especially if you bond being used in e.g. a parallel
        # test runner. Though it's slightly less convenient since module-level functions won't work
        self.current_python_test = None
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
        global TESTING
        TESTING = True

        self.observations = []
        self.spy_agents = {}
        self.spy_groups = {}
        self.current_python_test = current_python_test

        self._settings = {}  # Clear settings before each test
        self.settings(**kwargs)

        self.test_name = (self._settings.get('test_name') or
                          current_python_test.__class__.__name__ + "." + current_python_test._testMethodName)
        # TODO: the rest is specific to unittest. We need to factor it out to allow other frameworks. See issue #2
        # (the use of current_python_test._testMethodName above is unittest specific as well)
        # Register us on test exit
        current_python_test.addCleanup(self._finish_test)
        # We remember the start counter for failures and errors
        # This is the best way I know how to tell that a test has failed
        self.start_count_failures = len(current_python_test._resultForDoCleanups.failures)
        self.start_count_errors = len(current_python_test._resultForDoCleanups.errors)

        if 'observation_directory' not in self._settings:
            print('WARNING: you should set the settings(observation_directory). Observations saved to {}'.format(
                Bond.DEFAULT_OBSERVATION_DIRECTORY
            ))

    def spy(self, spy_point_name, **kwargs):
        assert self.current_python_test, "Should not call spy unless you have called start_test first"
        assert isinstance(spy_point_name, basestring), "spy_point_name must be a string"

        # Find the agent to apply. We process the agents in order, because they are deployed at the start of the list
        active_agent = None

        for agent in self.spy_agents.get(spy_point_name, []):
            if not agent.filter(kwargs):
                continue
            active_agent = agent
            break

        observation = copy.deepcopy(kwargs)
        observation['__spy_point__'] = spy_point_name  # Use a key that should come first alphabetically
        # TODO: Why are we including this in the observation dictionary? GN: makes it easier to read the observations

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
        assert self.current_python_test, "Should not call deploy_agent unless you have called start_test first"
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
        return repr(obj) + " not JSON-serializable."

    def _finish_test(self):
        """
        Called internally when a test ends
        :return:
        """
        try:
            global TESTING
            TESTING = False

            # Were there failures and errors in this test?
            # TODO: this is specific to unittest. See issue #2
            failures_and_errors = (
                "\n".join([err for tst, err in
                           self.current_python_test._resultForDoCleanups.failures[self.start_count_failures:] +
                           self.current_python_test._resultForDoCleanups.errors[self.start_count_errors:]]))
            # Save the observations
            if failures_and_errors:
                # TODO: need to configure printing
                print("Not saving observations due to failed test")
            else:
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
                assert self._reconcile_observations(reference_file, current_file), \
                    'Reconciling observations for {}'.format(self.test_name)
        finally:
            # Mark that we are outside of a test
            self.current_python_test = None
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
        test_file = inspect.getfile(self.current_python_test.__class__)
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
                                current_file):
        settings = dict(merge=self._settings.get('merge'))
        return bond_reconcile.reconcile_observations(settings,
                                                     test_name=self.test_name,
                                                     reference_file=reference_file,
                                                     current_file=current_file)


class SpyAgent:
    """
    A spy agent applies to a particular spy_point_name, has
    some optional filters to select only certain observations,
    and has optional mocking parameters.
    See documentation for the deploy_agent top-level function.
    """

    def __init__(self, spy_point_name, **kwargs):
        self.spy_point_name = spy_point_name
        self.result_spec = None
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
        if r is not None:
            if hasattr(r, '__call__'):
                return r(observation)
            else:
                return r
        else:
            return AGENT_RESULT_NONE


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


# Import bond_reconcile at the end, because we import bond from bond_reconcile, and
# we need at least the spy_point to be defined
import bond_reconcile