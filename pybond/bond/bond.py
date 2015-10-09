from functools import wraps
import inspect

TESTING = True # False # should be False by default but leaving as True during initial development

class Bond:

    NO_MOCK_RESPONSE = '_bond_no_mock_response' # TODO ?

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

    def observe(self, spyPointName, formatter=None, **kwargs):
        observationDictionary = kwargs if formatter is None else formatter(kwargs)
        print "{}: {}".format(spyPointName, observationDictionary)
        if spyPointName in self.pointReturns:
            return self.pointReturns[spyPointName]
        return Bond.NO_MOCK_RESPONSE
    
    def getOutputDirectoryPath(self):
        return '/tmp/bondOutput'

# Function annotation for observation
""" Internal Notes:
    - wouldn't make sense to pass an observe function here since that puts the 'mock' inside of prod code
      - maybe would make sense to have a single default return value to return during testing?
    - this MUST be the first decorator applied to your function (i.e. the bottommost one) or else
      the inspect.getargspec() won't work
"""
# Dependency injection for mocking out bond for testing?
def observeFunction(spyPointName=None, mockMandatory=False, excludedKeys=(), formatter=None, observeReturn=False, bond=Bond.instance()):
    assert TESTING, 'Must be in testing mode to use observations!'

    def wrap(fn):
        pointName = fn.__name__ if spyPointName is None else spyPointName
        arginfo = inspect.getargspec(fn)
        # print arginfo

        @wraps(fn)
        def fnWrapper(*args, **kwargs):
            observationDictionary = {}
            for idx, arg in enumerate(args):
                if idx != 0 or arginfo.args[idx] != 'self':  # TODO more rigorous check?
                    observationDictionary[arginfo.args[idx]] = arg
            for key, val in kwargs.iteritems():
                observationDictionary[key] = val
            observationDictionary = {key: val for (key, val) in observationDictionary.iteritems()
                                     if key not in excludedKeys and key != 'self'}

            response = bond.observe(pointName, formatter=formatter, **observationDictionary)
            if mockMandatory:
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