from __future__ import print_function
from functools import wraps
import inspect
import unittest
import types
import copy
import os
import json


TESTING = True  # False # should be False by default but leaving as True during initial development


# Function annotation for observation
""" Internal Notes:
    - wouldn't make sense to pass an observe function here since that puts the 'mock' inside of prod code
      - maybe would make sense to have a single default return value to return during testing?
    - this MUST be the first decorator applied to your function (i.e. the bottommost one) or else
      the inspect.getargspec() won't work
"""


# Dependency injection for mocking out bond for testing?
# TODO right now excluding 'self' using excludedKeys, should attempt to find a better way?
def spy_point(spy_point_name=None,
              mock_mandatory=False,
              excludedKeys=('self'),
              formatter=None,
              observeReturn=False,
              bond=None):
    # TODO: can we avoid the "bond" argument ?

    def wrap(fn):
        # TODO: we get an error here if we do not specify spy_point_name and this is a staticmethod
        # The is if we have @spy_point() @staticmethod
        pointName = fn.__name__ if spy_point_name is None else spy_point_name
        if not inspect.isfunction(fn):
            raise TypeError('The observeFunction decorator may only be applied to functions/methods!')
        arginfo = inspect.getargspec(fn)
        # print arginfo

        @wraps(fn)
        def fnWrapper(*args, **kwargs):
            observationDictionary = {}
            for idx, arg in enumerate(args):
                observationDictionary[arginfo.args[idx]] = arg
            for key, val in kwargs.iteritems():
                observationDictionary[key] = val
            observationDictionary = {key: val for (key, val) in observationDictionary.iteritems()
                                     if key not in excludedKeys}
            the_bond = bond or Bond.instance()
            response = the_bond.spy(pointName, formatter=formatter, **observationDictionary)
            if mock_mandatory:
                assert response is not Bond.NO_MOCK_RESPONSE, 'You MUST mock out spypoint {}'.format(pointName)
            if response is Bond.NO_MOCK_RESPONSE:
                retVal = fn(*args, **kwargs)
                # if observeReturn:
                # TODO would be nice for these two observations to be on the same output. maybe could have
                # some sort of 'observePartial' that keeps saves info but doesn't log it yet, waits for an
                # 'observeComplete' or something
                # bond.observe(pointName + '.return', retVal)
                return retVal
            else:
                return response

        return fnWrapper

    return wrap

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
               test_name=None):
    """
    This function should be called in a unittest.TestCase before any
    of the other Bond functions can be used. This will initialize the Bond
    module for the current test, and will ensure proper cleanup of Bond
    state when the test ends.

    @param current_python_test: the instance of TestCase that is running
    @param test_name: the name of the test. By default, it is TestCase.testName.
    @return:
    """
    Bond.instance().start_test(current_python_test,
                               test_name=test_name)


def spy(spy_point_name, **kwargs):
    """
    This is the most frequently used Bond function. It will collect the key-value pairs passed
    in the argument list and will emit them to the spy observation log..
    If there is an observer registered for the current spy point (see Bond.PushObserver), it will process the observer.

    The values are formatted to JSON using the json module, with sorted keys, and indentation, with
    one value per line, to streamline the observation comparison.
    For user-defined classes, the method toJSON is called on the instance before it is formatted.
    This method should return a JSON-serializable data structure.

    If the special key "format" is present, its value must be a function that is used on a copy of
    the arguments to produce a formatted dictionary.

    @param spyPointName: the spy point name, useful to distinguish among different observations
    @param kwargs: key-value pairs to be observed. There is a special key name:

    @return: the result from the observer, if any (see bond.push_observer)
    """
    return Bond.instance().spy(spy_point_name, **kwargs)


def deploy_agent(spy_point_name, **kwargs):
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
             * filter=func : only when the given func returns true when passed observed argument dictionary
        * Keys that control what the observer does when processed:
             * do=func : executes the given function with the observed argument dictionary.
                         func can also be a list of functions, executed in order.

        * Keys that control what the corresponding Bond.Observe returns (by default None):
             * exception=x : the call to bond.spy throws the given exception. If 'x' is a function
                             it is invoked on the observe argument dictionary to compute the exception to throw.
             * result=x : the call to bond.spy returns the given value. If 'x' is a function
                             it is invoked on the observe argument dictionary to compute the value to return.
    @return:
    """
    # TODO: should we call this deploy_agent ?
    # TODO: should we support deploying agents that are not reset when the test ends? MAybe not, esepcially
    #       if we will create one instance for each test.
    Bond.instance().deploy_agent(spy_point_name, **kwargs)


class Bond:
    NO_MOCK_RESPONSE = '_bond_no_mock_response'  # TODO ?
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
        self.current_python_test = None
        self.start_count_failures = None
        self.start_count_errors = None
        self.test_name = None
        self.observations = []  # Here we will collect the observations

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

        self.current_python_test = current_python_test
        self.test_name = (kwargs.get('test_name') or
                          current_python_test.__class__.__name__ + "." + current_python_test._testMethodName)
        # TODO: the rest is specific to unittest. We need to factor it out to allow other frameworks
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

    def pushObserverReturn(self, spyPointName, returnValue):
        self.pointReturns[spyPointName] = returnValue

    def spy(self, spy_point_name, formatter=None, **kwargs):
        assert self.current_python_test, "Should not call spy unless you have called start_test first"
        assert isinstance(spy_point_name, basestring), "spy_point_name must be a string"

        observation = copy.deepcopy(kwargs)
        observation['__spy_point_name'] = spy_point_name  # Use a key that should come first alphabetically
        formatted = self._format_observation(spy_point_name, observation, formatter=formatter)

        # TODO: This is not nice in general, we need a way to control this from settings
        print("Observing: " + formatted + "\n")
        self.observations.append(formatted)

        if spy_point_name in self.pointReturns:
            return self.pointReturns[spy_point_name]
        return Bond.NO_MOCK_RESPONSE


    def deploy_agent(self,
                   spy_point_name,
                   **kwargs):
        """
        Deploy an agent for a spy point.
        See documentation for the top-level push_agent function.
        :param spy_point_name:
        :param kwargs:
        :return:
        """
        assert isinstance(spy_point_name, basestring), "spy_point_name must be a string"
        assert False, 'Not implemented'


    def _format_observation(self,
                            spy_point_name,
                            observation,
                            formatter=None):
        if formatter is not None:
            formatter(observation)
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


# Import bond_reconcile at the end, because we import bond from bond_reconcile, and
# we need at least the spy_point to be defined
import bond_reconcile