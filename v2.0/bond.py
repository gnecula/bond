from functools import wraps
import inspect


# Function annotation for observation
def observeFunction(spyPointName = None, mockMandatory = False):
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

            response = bond.observe(pointName, observationDictionary)
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
        pass

    def observe(self, spyPointName, observationDictionary):
        print "{}: {}".format(spyPointName, observationDictionary)
        return Bond.NO_MOCK_RESPONSE
