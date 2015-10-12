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
def settings(observation_directory=None,
             merge=None):
    """
    Initialize Bond settings.
    @param observation_directory: the directory where the observation files are stored.
    @param merge: the method used to merge the observations
    @return:
    """
    Bond.instance().settings(observation_directory=observation_directory,
                             merge=merge)


def start_test(current_python_test,
               test_name=None,
               spy_groups=None):
    """
    This function should be called in a unittest.TestCase before any
    of the other Bond functions can be used. This will initialize the Bond
    module for the current test, and will ensure proper cleanup of Bond
    state when the test ends.

    @param current_python_test: the instance of TestCase that is running
    @param test_name: the name of the test. By default, it is TestCase.testName.
    @param spy_groups: the list of spy point groups that are enabled. By default,
                      enable all spy points that do not have an enable_for_groups
                      attribute.
    @return:
    """
    Bond.instance().start_test(current_python_test,
                               test_name=test_name,
                               spy_groups=spy_groups)


def spy(spy_point_name, **kwargs):
    """
    This is the most frequently used Bond function. It will collect the key-value pairs passed
    in the argument list and will emit them to the spy observation log.
    If there is an observer registered for the current spy point (see Bond.PushObserver), it will process the observer.

    The values are formatted to JSON using the json module, with sorted keys, and indentation, with
    one value per line, to streamline the observation comparison.
    For user-defined classes, the method toJSON is called on the instance before it is formatted.
    This method should return a JSON-serializable data structure.

    If the special key "format" is present, its value must be a function that is used on a copy of
    the arguments to produce a formatted dictionary.

    @param spyPointName: the spy point name, useful to distinguish among different observations
    @param kwargs: key-value pairs to be observed. There is a special key name:

    @return: the result from the agent, if any (see bond.deploy_agent)
    """
    return Bond.instance().spy(spy_point_name, **kwargs)


def deploy_agent(spy_point_name, **kwargs):
    # TODO: I think saying "agents for a point are processed in reverse order" is a little misleading;
    # to me it implies that all of the agents are processed, when really only one is processed (kind of...
    # we need to fully specify behavior of result and doers))
    # I think a better description might be "Agents are evaluated to see if they apply to the specific
    # spy invocation in reverse order" or something along those lines
    # Also you mix "observer" and "agent", we should be consistent
    # TODO: Right now it seems that the filters are applied in an AND fashion (all filter criteria must
    #       be met). Do we want to make this configurable (AND or OR)? Either way this should be documented
    """
    Create a new agent for the named spy point. Agents for a point are processed in reverse order
    of their introduction (last one is processed first).

    @param spyPointName: the point for which to create the observer. This observer will only be executed
        for invocations of Bond.Observe with the the same spyPointName.
    @param kwargs: key-value pairs that control the execution of the observer. The following keys are
        recognized:
        * Keys that restrict for which invocations of bond.spy this observer is processed:
             * key=val : only when the observed argument dictionary contains the 'key' with the given value
             * key__contains=substr : only when the observed argument dictionary contains the 'key' with a string value
                      that contains the given substr
             * key__startswith=substr : only when the observed argument dictionary contains the 'key' with a
                      string value that starts with the given substr
             * key__endswith=substr : only when the observed argument dictionary contains the 'key' with a string value
                      that ends with the given substr
             * filter=func : only when the given func returns true when passed observed argument dictionary.
                      Uses the observation before formatting.

        * Keys that control what the observer does when processed:
             * ignore=value : determines if the observation is ignored. If the value is a function,
                              it is invoked on the observation. The first processed agent that
                              specified an "ignore" value takes precendence. Uses the observation before
                              formatting.
             * formatter : if specified, a function that is given the observation and can update it in place.
                           The formatted observation is what gets saved, and used in 'exception' or 'result'
                           or 'do' functions.
             * do=func : executes the given function with the observed argument dictionary.
                         func can also be a list of functions, executed in order.


        * Keys that control what the corresponding spy returns (by default AGENT_RESULT_NONE):
             * exception=x : the call to bond.spy throws the given exception. If 'x' is a function
                             it is invoked on the observe argument dictionary to compute the exception to throw.
             * result=x : the call to bond.spy returns the given value. If 'x' is a function
                             it is invoked on the observe argument dictionary to compute the value to return.
    @return: either AGENT_RESULT_NONE if not agent matches or contains a "result", or the result from
             the first agent that matches.
    """
    # TODO: should we support deploying agents that are not reset when the test ends? MAybe not, esepcially
    #       if we will create one instance for each test.
    #       ERIK: I think probably not, if you want an agent to be present for all tests shouldn't you just put it
    #             in your test setUp() ?
    Bond.instance().deploy_agent(spy_point_name, **kwargs)


# Dependency injection for mocking out bond for testing?
# TODO right now excluding 'self' using excludedKeys, should attempt to find a better way?
def spy_point(spy_point_name=None,
              enabled_for_groups=None,
              require_agent_result=False,
              excluded_keys=('self'),
              spy_return=False):
    """
    Decorator for marking Bond spy points.
    Must be applied directly to a method or a function, not to another decorator.

    :param spy_point_name: An optional name to use for this spy point. Default is obtained from the name
                           of the decorated function.
    :param enabled_for_groups: An optional list or tuple of spy point groups to which this spy point belongs.
                           If missing then it is enabled for all groups.
    :param require_agent_result: if True, and if this spy point is enabled, then there must be an
                           agent that provides a result, or else the invocation of the function aborts.
                           The agent may still provide AGENT_RESULT_CONTINUE to tell the spy point
                           to continue the invocation of the underlying function.
    :param excluded_keys:
    :param spy_return:
    :return:
    """
    # TODO: Should we also have an excluded_from_groups parameter?
    # TODO: can we avoid the "bond" argument ?

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
        def fnWrapper(*args, **kwargs):
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
                        spy_point_name_local = args[0].__class__.__name__+'.'+fn.__name__
                    elif arginfo[0][0] == 'cls':
                        # A class method
                        spy_point_name_local = args[0].__name__+'.'+fn.__name__
                if spy_point_name_local is None:
                    # TODO We get here both for staticmethod and for module-level functions
                    # If we had the spy_point wrapper outside the @staticmethod we could tell
                    # more easily what kind of method this was !!
                    module_name = getattr(fn, '__module__')
                    # Keep only the last component of the name
                    module_name = module_name.split('.')[-1]
                    spy_point_name_local = module_name+'.'+fn.__name__
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

            if spy_return:
                # TODO would be nice for these two observations to be on the same output. maybe could have
                # some sort of 'observePartial' that keeps saves info but doesn't log it yet, waits for an
                # 'observeComplete' or something
                the_bond.spy(spy_point_name_local+'.return', result=return_val)
            return return_val

        return fnWrapper

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
        #       test runner. Though it's slightly less convenient since module-level functions won't work
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
        self.current_python_test = current_python_test
        self.test_name = (kwargs.get('test_name') or
                          current_python_test.__class__.__name__ + "." + current_python_test._testMethodName)
        spy_groups = kwargs.get('spy_groups')
        if isinstance(spy_groups, basestring):
            spy_groups = (spy_groups,)
        else:
            assert isinstance(spy_groups, (list, tuple))
        self.spy_groups = {}
        for sg in spy_groups:
            self.spy_groups[sg] = True


        # TODO: the rest is specific to unittest. We need to factor it out to allow other frameworks
        #       (the use of current_python_test._testMethodName above is unittest specific as well)
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

        # Does at least one agent tell us to ignore this
        # Process the agents in order
        applicable_agents = []
        ignore_result = None

        for agent in self.spy_agents.get(spy_point_name, []):
            if not agent.filter(kwargs):
                continue
            applicable_agents.append(agent)

            # TODO: Is this really the correct behavior? If I push an agent that says ignore=True,
            #       then later push one with no specification of ignore, based on the logic that more recent
            #       agents take precedence I would expect the second one (with the default behavior of
            #       not ignoring) to win
            if ignore_result is None:
                agent_ignore = agent.ignore(kwargs)
                if agent_ignore is not None:
                    if agent_ignore:
                        # This agent says "ignore", don't even bother anymore
                        return AGENT_RESULT_NONE
                    else:
                        # This agent says "do not ignore", don't ask others
                        ignore_result = False

        observation = copy.deepcopy(kwargs)
        observation['__spy_point_name'] = spy_point_name  # Use a key that should come first alphabetically
        # TODO: Why are we including this in the observation dictionary?

        formatted = self._format_observation(observation,
                                             agents=applicable_agents)

        # TODO: This is not nice in general, we need a way to control this from settings
        print("Observing: " + formatted + "\n")
        self.observations.append(formatted)

        # Now process the agents
        for agent in applicable_agents:
            agent.do(observation)
            res = agent.result(observation)  # This may throw an exception
            if res != AGENT_RESULT_NONE:
                # If an agent says return, we have our return
                # TODO: should we try the "doers" of the other agents?
                # ERIK: Excellent question - I think we should discuss the exact behavior here
                print("   Returned "+repr(res))
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

    def _format_observation(self,
                            observation,
                            agents=None):
        # TODO: I do not quite like how formatters work. Right now, they modify the observation dictionary in place
        # This makes it easy for the formatter, but has unpleasant side-effects, which are visible to the
        # downstream functions (result, doers). This also make it impossible to write
        # a one-line formatter using 'lambda' (but that is really a weakness in Python).
        # Also it is still pretty annoying to write these
        # formatters. What I would really like is to override how particular keys are formatted, in depth.
        # For example, I want to split the lines for key1[*].foo, or I want to replace all strings
        # that match a date in key2.dates[*]. But that seems to require a custom implementation of
        # the JSON serializer.
        if agents:
            for agent in agents:
                agent.formatter(observation)

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
            # TODO: this is specific to unittest
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
                if not os.path.isfile(reference_file):
                    print("Saved observations in file " + reference_file)
                    self._save_observations(reference_file)
                else:
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
        return self._settings.get('observation_directory', Bond.DEFAULT_OBSERVATION_DIRECTORY)

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
        self.ignore_spec = None
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
            elif k == 'ignore':
                self.ignore_spec = kwargs['ignore']
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

    def ignore(self, observation):
        """
        See if we need to ignore
        @return None for 'don't care', True/False otherwise
        """
        if self.ignore_spec is not None:
            if hasattr(self.ignore_spec, '__call__'):
                return self.ignore_spec(observation)
            else:
                return self.ignore_spec
        else:
            return None

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
                raise es()
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
        self.field_name = None   # The observation field name the filter applies to
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
                assert False, "Unknown operator: "+cmp_spec
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