from functools import wraps
import inspect

TESTING = True # False

# Function annotation for observation
""" Internal Notes:
    - wouldn't make sense to pass an observe function here since that puts the 'mock' inside of prod code
      - maybe would make sense to have a single default return value to return during testing?
    - this MUST be the first decorator applied to your function (i.e. the bottommost one) or else
      the inspect.getargspec() won't work
"""
def observeFunction(spyPointName=None, mockMandatory=False, excludedKeys=(), formatter=None):
    assert TESTING, 'Must be in testing mode to use observations!'
    bond = Bond.instance()

    def wrap(fn):
        pointName = fn.__name__ if spyPointName is None else spyPointName
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

            response = bond.observe(pointName, observationDictionary, formatter=formatter)
            if mockMandatory:
                assert response != Bond.NO_MOCK_RESPONSE, 'You MUST mock out spypoint {}'.format(pointName)
            return response if response != Bond.NO_MOCK_RESPONSE else fn(*args, **kwargs)
        return fnWrapper
    return wrap

class Bond:

    NO_MOCK_RESPONSE = 'bond_no_mock_response' # TODO ?

    _instance = None
    @staticmethod
    def instance():
        if Bond._instance is None:
            Bond._instance = Bond()
        return Bond._instance

    def __init__(self):
        self.pointReturns = {}

    def pushObserverReturn(self, spyPointName, returnValue):
        self.pointReturns[spyPointName] = returnValue

    def observe(self, spyPointName, observationDictionary, formatter=None):
        if formatter is not None:
            observationDictionary = formatter(observationDictionary)
        print "{}: {}".format(spyPointName, observationDictionary)
        if spyPointName in self.pointReturns:
            return self.pointReturns[spyPointName]
        return Bond.NO_MOCK_RESPONSE
