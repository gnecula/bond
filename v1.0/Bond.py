#-- Author: George Necula
#--
"""
Library for spy-based testing.

Spies address several difficulties of writing tests:
* How to monitor calls your *system under test* (SUT) is making to *depended-on components* (DOC)
* How to access and check key intermediate state values in SUT, and
* How to check state values that they have the expected values in a way that makes it
    very easy to update the expected values as the code or the test changes.

With spies, you add spy observations in the production code (predicated on a Bond.TESTING predicate). The observer collects the list
of observed values and saves them for later comparisons. At the end of a test you have the option to run "queries" on the observations, or
to save them to a text file so that you can use standard "diff-and-merge" tools to compare the observations to previously saved observations.

See the documentation of utils.Bond.

"""

import copy
import re
import os,shutil
import json
import logging
import time

"""A global that you can use to check that you are in testing mode"""
TESTING = False

# We export some module-functions for more convenient use
def SetObservationSaveDirectory(directory):
    """
    Set the directory where we save the test observations. This function must be called
    before Bond can be used.
    @param directory:
    @return:
    """
    Bond.getInstance().SetObservationSaveDirectory(directory)

def GetObservationFilename():
    """
    Get the file (no extension) where the observations are stored for the current test
    @return:
    """
    return Bond.getInstance().GetObservationFilename()

def GetTestName():
    """
    Get the current test name (in format class.method)
    @return:
    """
    return Bond.getInstance().GetTestName()


def StartTest(currentPythonTest):
    """
    This function should be called in a unittest.TestCase before any
    of the other Bond functions can be used. This will initialize the Bond
    module for the current test, and will ensure proper cleanup of Bond
    state when the test ends.

    @param currentPythonTest: the instance of TestCase that is running
    @return:
    """
    Bond.getInstance().StartTest(currentPythonTest)

def AddStartTestHook(func):
    """
    Add a function to run before a test starts (on Bond.StartTest).
    TODO: there is no way to remove this function
    @param func:
    @return:
    """
    Bond.getInstance().AddStartTestHook(func)



def Observe(spyPointName, **kwargs):
    """
    This is the most frequently used Bond function. It will collect the key-value pairs passed
    in the argument list and will emit them to the spy log..
    If there is an observer registered for the current spy point (see Bond.PushObserver), it will process the observer.

    The values are formatted to JSON using the json module, with sorted keys.
    For user-defined classes, the method toJSON is called on the instance before it is formatted.
    This method should return a JSON-serializable data structure.

    If the special key "format" is present, its value must be a function that is used on a copy of
    the arguments to produce a formatted dictionary.

    @param spyPointName: the spy point name, useful to distinguish among different observations
    @param kwargs: key-value pairs to be observed. There is a special key name:
       * ''format'' : the value must be a function that is used on a copy of the arguments to produce a dictionary to be
                      emitted.
    @return: the result from the observer, if any (see Bond.PushObserver)
    """
    return Bond.getInstance().Observe(spyPointName, **kwargs)


def PushObserver(spyPointName, **kwargs):
    """
    Create a new observer for the named spy point. Observers for a point are processed in reverse order
    of their introduction (last one is processed first).

    @param spyPointName: the point for which to create the observer. This observer will only be executed
        for invocations of Bond.Observe with the the same spyPointName.
    @param kwargs: key-value pairs that control the execution of the observer. The following keys are
        recognized:
        * Keys that restrict for which invocations of Bond.Observe this observer is processed:
             * key=val : only when the observed argument dictionary contains the 'key' with the given value
             * key__contains=substr : only when the observed argument dictionary contains the 'key' with a string value
                      that contains the given substr
             * key__startswith=substr : only when the observed argument dictionary contains the 'key' with a string value
                      that starts with the given substr
             * key__endswith=substr : only when the observed argument dictionary contains the 'key' with a string value
                      that ends with the given substr
             * filter=func : only when the given func returns true when passed observed argument dictionary
        * Keys that control what the observer does when processed:
             * do=func : executes the given function with the observed argument dictionary
             * do__xxx=func : multiple functions can be given and they will be processed in the alphabetical order
                       of the 'xxx' name

        * Keys that control what the corresponding Bond.Observe returns (by default None):
             * exception=x : the call to Bond.Observe throws the given exception. If 'x' is a function
                             it is invoked on the observe argument dictionary to compute the exception to throw.
             * result=x : the call to Bond.Observe returns the given value. If 'x' is a function
                             it is invoked on the observe argument dictionary to compute the value to return.
    @return:
    """
    Bond.getInstance().PushObserver(spyPointName, **kwargs)

def PushFormatter(spyPointName, formatFunc):
    """
    Create a formatting function for the named spy point.

    @param spyPointName: the point for which to create the observer.
    @param formatFunc:
    @return:
    """
    Bond.getInstance().PushFormatter(spyPointName, formatFunc)

def CurrentPythonTest():
    """
    The current instance of unittest.TestCase
    @return:
    """
    return Bond.getInstance().CurrentPythonTest()

def PushLocalValue(rexp_str, name):
    """
    Use this function to report to Bond a regular expression of a string that may occur in the
    test observations and may be different from run to run (an IP address, a PID, a random number).

    Bond will remember these regular expressions and if the test fails because of mismatched observations
    it will take the lines that are different and will try to replace the occurrences of the given regular expressions
    in those lines with the given name, and will then try the match again.
    """
    Bond.getInstance().PushLocalValue(rexp_str, name)


def ReportError(errorMsg):
    """
    Use this function to report to Bond that an error has happened. Normally you would add this function
    to your global error reporting function for the app. By default, a test that has received an error
    this way will fail (but see Bond.ExpectError)
    """
    return Bond.getInstance().ReportError(errorMsg)

def ExpectError(errmsgSubstr):
    """
    If you have tests that expect to see an error, use this function to declare a substring that must appear in the
    error. The test will fail if it ends without seeing any matching error.
    """
    return Bond.getInstance().ExpectError(errmsgSubstr)

def Seen(**kwargs):
    return Bond.getInstance().Seen(**kwargs)

def NextId(idSpace="global"):
    """
    Generate a deterministic new numeric id obtained by incrementing a global counter.
    @param idSpace: If given, then the id is from the given name space. Each space has its own
                    global counter.
    @return:
    """
    return Bond.getInstance().NextId (idSpace=idSpace)

######
######
######
class Bond:
    """
    Encapsulate all functionality in a class, that is meant to be used as a singleton. We also provide module-global accessors
    """
    _instance = None
    @staticmethod
    def getInstance():
        if Bond._instance is not None: return Bond._instance
        Bond._instance = Bond ()
        return Bond._instance

    def __init__(self):
        self._TESTHOOKS = { } # A bunch of test hooks functions, indexed by pointName
        self._ABSTRACTIONS = [ ] # A bunch of functions per type
        self._FORMATS = { } # A bunch of functions to format the parameters for Seen
        self._COLLECTED = [ ]

        self._NEXTID = { }
        self._ERRORS = [ ]
        
        self._EXPECTED_ERRORS = [ ]
        self._EXPECTED_ERRORS_USED = {}  # Among the _EXPECTED_ERRORS which ones were used
        
        self.CURRENT_PYTHON_TEST = None

        # Functions to be called on each StartTest
        self._START_TEST_HOOKS = [ ]

        # The directory where we will save the test observations
        self.SAVE_DIRECTORY = None
        
        # A stack of local values (pair of regexp and name)
        # The are used as substitutions applied with last first
        self._LOCAL_VALUES_STACK = [ ]
        
        ### Manage the UNDO list
        self._UNDO_HISTORY = [ ] # A list of functions to revert the changes to the global variables

        """The value of the BOND environment variable"""
        bond = os.environ["BOND"] if "BOND" in os.environ else ""
        bondExpectedValues = [ "nop", "kdiff3", "console", "console_overwrite", "fail" , "" ]
        if bond not in bondExpectedValues:
            assert False, "Invalid value of BOND. Can be one of "+str(bondExpectedValues)
        self._BOND_KIND = bond


    def AddStartTestHook(self, func):
        """
        Add a function to run on each StartTest. The function will be invoked with the current Python test.
        """
        self._START_TEST_HOOKS.append(func)


    def SetObservationSaveDirectory(self, directory):
        """
        Set the directory where the observations will be saved. This directory will contain files
            testcase/testname.json
        """
        assert os.path.isdir(directory), "SetObservationSaveDirectory must be passed a valid directory"
        self.SAVE_DIRECTORY = directory

    def GetObservationFilename(self):
        fname = self.SAVE_DIRECTORY+"/"+ self.GetTestName().replace('.', '/')
        return fname

    def GetTestName(self):
        return self.CURRENT_PYTHON_TEST.__class__.__name__+"."+ self.CURRENT_PYTHON_TEST._testMethodName

    def StartTest(self, currentPythonTest):
        """
        Call this function to start a test
        @param currentPythonTest - an instance of a unittest.TestCase. This is used to
        """

        self.EnsureTESTING()
        if self.CURRENT_PYTHON_TEST is not None:
            assert False, "Calling StartTest (for "+currentPythonTest.__name__+") but there is a test already started ("+self.CURRENT_PYTHON_TEST.__name__+")"
        self.CURRENT_PYTHON_TEST = currentPythonTest

        assert self.SAVE_DIRECTORY is not None, "Must call SetObservationSaveDirectory before using Bond"

        self.nrUndoHistory = len(self._UNDO_HISTORY)
        self._COLLECTED = [ ]
        self._LOCAL_VALUES_STACK = [ ]
        self._NEXTID =  { } # map from idSpace to next id
        self._ERRORS = [ ]
        self._EXPECTED_ERRORS = [ ]
        self._EXPECTED_ERRORS_USED = {}

        # We remember the start counter for failures and errors
        # This is the best way I know how to tell that a test has failed
        self.startCountFailures = len(currentPythonTest._resultForDoCleanups.failures)
        self.startCountErrors = len(currentPythonTest._resultForDoCleanups.errors)
        currentPythonTest.addCleanup(self._FinishTest)

        # Run the start hooks
        for f in self._START_TEST_HOOKS:
            f(currentPythonTest)

    def CurrentPythonTest(self):
        return self.CURRENT_PYTHON_TEST

    def PushLocalValue(self, rexp_str, name):
        self._LOCAL_VALUES_STACK.append((re.compile(rexp_str), name))


    def PushFormatter(self, pointName, formatFunc):
        if not pointName in self._FORMATS:
            self.DictSetWithUndo(self._FORMATS, pointName, [ formatFunc ])
        else:
            self.ListPushWithUndo(self._FORMATS[pointName], formatFunc)

    def PushObserver(self, pointName, **kwargs):
        self.EnsureTESTING()

        th = _TestHook(pointName, **kwargs)
        if not pointName in self._TESTHOOKS:
            self.DictSetWithUndo(self._TESTHOOKS, pointName, [ th ])
        else:
            self.ListPushWithUndo(self._TESTHOOKS[pointName], th)



    def Observe(self, pointName, **kwargs):
        global TESTING
        assert TESTING, "Should not call Observe unless TESTING == True"

        data = copy.deepcopy(kwargs)
        data['pointName'] = pointName

        # Format it right away, before the mutable data changes
        formatted = self.formatObservation(pointName, data)
        if self._BOND_KIND != "nop":
            print "Observing: "+formatted+"\n"
        data['formatted'] = formatted
        self._COLLECTED.append(data)

        # Run the test hooks
        if self._TESTHOOKS and pointName in self._TESTHOOKS:
            theHooks = self._TESTHOOKS[pointName]
            for i in range(len(theHooks) - 1, -1, -1):
                th = theHooks[i]
                if th.filter(data):
                    th.do(data)
                    res = th.result(data)
                    print "   Returned "+repr(res)
                    return res

        return None


    def Seen(self, **kwargs):
        data = copy.copy(kwargs)
        format = None
        if 'format' in data:
            format = data['format']
            if format is None:
                format=lambda args: args
            del data['format']

        since = 0
        if 'since' in data:
            since = data['since']
            del data['since']

        allSeen = [ ]
        th = _TestHook(**data)
        cIdx = -1
        for c in self._COLLECTED:
            cIdx += 1
            if cIdx < since:
                continue

            if th.filter(c):
                # See if we need to format
                formatted = None
                if format is not None:
                    formatted = format(c)
                elif c['pointName'] in self._FORMATS:
                    formats = self._FORMATS[c['pointName']]
                    formatted = formats[len(formats) - 1](c)
                elif 'format' in c:
                    assert type(c['format']) == type(lambda : 0)
                    formatted = c['format'](c)
                else:
                    c = copy.copy(c)
                    del c['formatted']
                    formatted = c

                allSeen.append(formatted)

        return allSeen

    def CurrentObservationPointer(self):
        return len(self._COLLECTED)


    def NextId(self, idSpace="global"):
        if idSpace not in self._NEXTID:
            self._NEXTID[idSpace] = 1

        res = self._NEXTID[idSpace]
        self._NEXTID[idSpace] += 1
        return res



    ###
    ### Helper functions
    ###
    def customSerializer(self, o):
        if 'toJSON' in o.__class__.__dict__:
            return o.__class__.toJSON(o)
        if hasattr(o, 'toJSON'):
            return o.toJSON()
        if type(o) == type(lambda : 0):
            return "\"<lambda>\""
    
        return repr(o)+" not JSON serializable. Add a 'toJSON' method in the class"
    
    def formatObservation(self, pointName, obs):
        if 'format' in obs:
            formatted = obs['format'](obs)
        else:
            formatters = self._FORMATS.get(pointName)

            if formatters:
                formatted = formatters[len(formatters) - 1](pointName, obs)
            else:
                formatted = None
            if formatted is None:
                formatted = { }
                for key in obs:
                    if key == 'pointName': continue
                    formatted[key] = obs[key]
        res = "{\""+obs['pointName']+"\": "
        res += json.dumps(formatted, indent=4, default=self.customSerializer, sort_keys=True)
        res += "}"
        return res
    
    def saveObservations(self, fname):
        """Save the observations to a file"""
        f = open(fname, "w")
        f.write("[\n")
        isFirst = True
        for c in self._COLLECTED:
            if not isFirst:
                f.write(",\n\n")
            else:
                isFirst = False
            f.write(c['formatted'])
        f.write("\n]\n")
        f.close ()

    hunkHeaderRe = re.compile(r'^@@\s+-\d+,\d+\s+\+(\d+),\d+\s+@@')
    
    def adjustAndShowDifferences(self, fname):
        """
        Commpare fname+"_now.json" and fname+".json" using diff. If there are
        differences, try to apply the LOCAL_VALUES abstraction. If there are no more differences,
        return True. Otherwise, show the differences and return False. 
        """
        # First compute the straight diff
        code = os.system("diff -u -w "+fname+".json "+fname+"_now.json >"+fname+".diff")
        if code == 0:
            # No differences
            os.unlink(fname+".diff")
            return True
    
        if len(self._LOCAL_VALUES_STACK) == 0:
            os.system("cat "+fname+".diff")
            return False
    
        # Get the line numbers in the new file where we need to make changes
        linesToChange = [ ]
        fdiff = open(fname+".diff", "r")
        currentLine = 0
        inHunk = False
        for l in fdiff.readlines():
            m = Bond.hunkHeaderRe.match(l)
            if m:
                # print "Found hunk header "+l
                currentLine = int (m.group(1))
                inHunk = True
                continue
            if inHunk:
                if l[0] == '-':
                    # This is a line only in old file
                    continue
                elif l[0] == '+':
                    # This is a line only in the new file
                    linesToChange.append(currentLine)
                    currentLine += 1
                else:
                    currentLine += 1 # this is a line in both
        fdiff.close ()
        os.unlink(fname+".diff")
    
        # print "Found linesChanged: "+str(linesToChange)
        # print "Replacing "+str(_LOCAL_VALUES)
        # Now read in the new file
        fnew = open(fname+"_now.json", "r")
        newlines = fnew.readlines()
        fnew.close()
    
        fold = open(fname+".json", "r")
        oldlines = fold.readlines()
        fold.close ()
    
        for lc in linesToChange:
            l = newlines[lc - 1]
            for i in range(len(self._LOCAL_VALUES_STACK) - 1, -1, -1):
                lv_re, lv_name = self._LOCAL_VALUES_STACK[i]
                l = lv_re.sub(lv_name, l)
            newlines[lc - 1 ] = l
    
        if oldlines != newlines:
            fnew = open(fname+"_now.json", "w")
            for l in newlines:
                fnew.write(l)
            fnew.close ()
            return 0 == os.system("diff -u -w "+fname+".json "+fname+"_now.json")
        else:
            return True
    
    
    def compareObservations(self, failuresAndErrors, fname):
        """
        Save the current observations to a file and compare them with previously-stored observations.
        @param failuresAndErrors - a string describing current test failures and errors
        @param fname -
        @return the error message, or empty if no error
        """
        bondMerge = self._BOND_KIND
    
        if bondMerge == "nop":
            return ""
    
        # First save the observations in fname_now.json
        self.saveObservations(fname+"_now.json")
        if failuresAndErrors:
            print "Test failed:\n"+failuresAndErrors
            return ""
    
        if self.adjustAndShowDifferences(fname):
            os.unlink(fname+"_now.json")
            return ""
    
        if bondMerge == "kdiff3":
            code = os.system("kdiff3  -m -o "+fname+".out "+fname+".json "+fname+"_now.json")
            if code == 0:
                print "Saving new "+fname+".json"
                os.unlink(fname+".json")
                shutil.move(fname+".out", fname+".json")
                os.unlink(fname+"_now.json")
                return ""
        elif bondMerge == "console" or bondMerge == "console_overwrite":
            print "There were differences between the saved observations and the current ones (%s)." % fname
            overwrite = False
            if bondMerge == "console_overwrite":
                overwrite = True
            else:
                response = raw_input("Do you want to accept the current ones? (y/*): ")
                overwrite = (response == "yes" or response == "y")
            if overwrite:
                print "Saving new "+fname+".json"
                os.unlink(fname+".json")
                shutil.move(fname+"_now.json", fname+".json")
                return ""
    
        # We get here only if we have differences and we have not merged them
        return "BOND_FAIL. Pass BOND=[kdiff3|console|console_overwrite] environment variable to merge the observations."




    def _FinishTest(self):

        for i in range(len(self._UNDO_HISTORY) - 1, self.nrUndoHistory - 1, -1):
            self._UNDO_HISTORY[i] ()
        self._UNDO_HISTORY = self._UNDO_HISTORY[0:self.nrUndoHistory]

        # Add the errors that we got
        failuresAndErrors = "\n".join([ err for tst, err in
                                        self.CURRENT_PYTHON_TEST._resultForDoCleanups.failures[self.startCountFailures:]
                                         +  self.CURRENT_PYTHON_TEST._resultForDoCleanups.errors[self.startCountErrors:]])
        # Perhaps we must signal errors
        signalErrors = False
        if not failuresAndErrors and self._ERRORS:
            signalErrors = True
        failuresAndErrors += "\n".join(self._ERRORS)

        assertionMessage = ""
        if len(self._COLLECTED) > 0:
            # Now save the observations
            fname = self.GetObservationFilename()
            fdir = os.path.dirname(fname)
            if not os.path.isdir(fdir):
                os.makedirs(fdir)

            if not os.path.isfile(fname+".json"):
                if not failuresAndErrors:
                    fname = fname + ".json"
                    print "Saved observations in file "+fname
                    self.saveObservations(fname)
                else:
                    print "Not saving the observations due to failed test: "+failuresAndErrors
            else:
                # Save the obser
                assertionMessage += self.compareObservations(failuresAndErrors, fname)


        self._COLLECTED = [ ]
        self._LOCAL_VALUES_STACK = [ ]
        self._NEXTID.clear ()

        if signalErrors:
            assertionMessage += "We had errors:\n* "+"\n* ".join(self._ERRORS)+"\n"
        self._ERRORS = [ ]

        # See which errors we have not used
        errorsNotUsed = [ ]
        for ee in self._EXPECTED_ERRORS:
            if not ee in self._EXPECTED_ERRORS_USED:
                errorsNotUsed.append(ee)
        if errorsNotUsed:
            assertionMessage += "We did not see expected errors:\n* "+"\n* ".join(errorsNotUsed)

        self._EXPECTED_ERRORS = [ ]
        self._EXPECTED_ERRORS_USED = {}

        self.CURRENT_PYTHON_TEST = None
        if assertionMessage:
            assert False, assertionMessage



    def AddCleanup(self, closure):
        """
        Add a cleanup to be done on the current test exit
        """
        if self.CURRENT_PYTHON_TEST is None:
            assert False, "You must call StartTest(self) before using Bond"
        self, self.CURRENT_PYTHON_TEST.addCleanup(closure)



    def abstractValue(self, v):
        tv = type(v)
        if tv == str or tv == int or tv == float or tv == bool:
            return v
        if tv == type({}):
            res = { }
            for k in v:
                res[k] = self.abstractValue(v[k])
            return res
        if tv == type([]):
            res = [ self.abstractValue(v1) for v1 in v]
            return res

            # TODO:
        assert False, "Don't know how to abstract type "+str(tv)

    def DictSetWithUndo(self, dict, key, val):
        oldValPresent = False
        oldVal = None
        if key in dict:
            oldValPresent = True
            oldVal = dict[key]

        def undo():
            if oldValPresent:
                dict[key] = oldVal
            else:
                del dict[key]
        self._UNDO_HISTORY.append(undo)
        dict[key] = val

    def ListPushWithUndo(self, stack, val):
        self._UNDO_HISTORY.append(lambda : stack.pop())
        stack.append(val)

    def EnsureTESTING(self):
        global TESTING
        if not TESTING:
            def cleanup():
                global TESTING
                TESTING = False
            self._UNDO_HISTORY.append(cleanup)

        TESTING = True

    def ExpectError(self, errmsgSubstr):
        self._EXPECTED_ERRORS.append(errmsgSubstr)

    def ReportError(self, errmsg):
        for ee in self._EXPECTED_ERRORS:
            if errmsg.find(ee) >= 0:
                self._EXPECTED_ERRORS_USED[ee] = True
                return
        self._ERRORS.append(errmsg)




class BondLoggingHandler(logging.Handler):
    """
    A logging handler to be used if you want to spy the logging calls.
    """
    def emit(self, record):
        formatted = self.format(record)
        Observe("Bond.log", msg=formatted)


#! \cond PRIVATE

class _FilterOperator:
    def __init__(self, filterSpec, filterValue):
        self.filterSpec = filterSpec
        if filterSpec == 'filter':
            assert type(filterValue) == type(lambda :0)
            self.fieldName = None
            self.filterFunc = filterValue
            return

        parts = filterSpec.split("__")
        if len(parts) == 1:
            self.fieldName = parts[0]
            self.filterFunc = (lambda f: f == filterValue)
        elif len(parts) == 2:
            self.fieldName = parts[0]
            cmpSpec = parts[1]
            if cmpSpec == 'exact':
                self.filterFunc = (lambda f: f == filterValue)
            elif cmpSpec == 'eq':
                self.filterFunc = (lambda f: f == filterValue)
            elif cmpSpec == 'startswith':
                self.filterFunc = (lambda f: f.find(filterValue) == 0)
            elif cmpSpec == 'endswith':
                self.filterFunc = (lambda f: f.find(filterValue) == len(f) - len(filterValue))
            elif cmpSpec == 'contains':
                self.filterFunc = (lambda f: f.find(filterValue) >= 0)
            else:
                assert False, "Unknown operator: "+cmpSpec
        else:
            assert False


    def filter(self, args):
        if self.fieldName:
            return (self.fieldName in args and self.filterFunc(args[self.fieldName]))
        else:
            return self.filterFunc(args)



class _TestHook:
    def __init__(self, pointName, **kwargs):
        self.pointName = pointName
        self.resultSpec = None
        self.formatSpec = None
        self.exceptionSpec = None
        self.doers =  [ ] # A list of things to do
        self.pointNameFilter = None # The filter for pointName, if present
        self.filters = [ ] # The generic filters

        unsortedDoers = [ ]
        for k in kwargs:
            if k == 'result':
                self.resultSpec = kwargs['result']
            elif k == 'exception':
                self.exceptionSpec = kwargs['exception']
            elif k == 'format':
                self.formatSpec = kwargs['format']
            elif k == 'do' or k.find('do__') == 0:
                unsortedDoers.append(k)
            else:
                fo = _FilterOperator(k, kwargs[k])
                if k == 'pointName':
                    self.pointNameFilter = fo
                else:
                    self.filters.append(fo)

        # Process the doers
        for k in sorted(unsortedDoers):
            assert type(kwargs[k]) == type(lambda : 0), "'do' specification must carry a function"
            self.doers.append(kwargs[k])

    def filter(self, args):
        """Run the filter"""
        # Run first the pointName filter, to ensure that it is well-defined to evaluate the other filters
        if self.pointNameFilter:
            if not self.pointNameFilter.filter(args):
                return False

        for f in self.filters:
            if not f.filter(args): return False
        return True

    def do(self, args):
        for d in self.doers:
            d(args)

    def result(self, args):
        """Compute the result"""
        es = self.exceptionSpec
        if es is not None:
            if type(es) == type(lambda :0):
                raise es ()
            else:
                raise es

        r = self.resultSpec
        if r is not None:
            if  hasattr(r, '__call__'):
                return r(args)
            else:
                return r
        else:
            return None



####
#### TESTING
####
import unittest
class BondTest(unittest.TestCase):
    def setUp(self):
        if not os.path.isdir('/tmp/bondTest'):
            os.makedirs('/tmp/bondTest', 0777)
        SetObservationSaveDirectory("/tmp/bondTest")
        StartTest(self)
        self.assertTrue(TESTING)

    def testDoers(self):
        results = [ ]

        PushObserver('fun1', cmd__startswith="myfun",
                     do__1=(lambda args: results.append(args['cmd']+"__1")),
                     do=(lambda args: results.append(args['cmd'])))
        # Now the actual code
        Observe('fun1', cmd="myfun1")
        Observe('nofun', cmd="myfun2")
        Observe('fun1', cmd="myfun3")

        self.assertSequenceEqual(['myfun1', 'myfun1__1', 'myfun3', 'myfun3__1'], results)

    def testException(self):
        results = [ ]

        PushObserver('fun1', cmd__startswith="myfun", result=1, exception=Exception("some exception"))
        self.assertRaises(Exception, lambda :  Observe('fun1', cmd="myfun1"))


        PushObserver('fun1', cmd="myfun2", result=1, exception=lambda : Exception("some exception"))
        self.assertRaises(Exception, lambda :  Observe('fun1', cmd="myfun2"))


    def __testSeen(self):
        PushObserver("blah")
        Observe('fun1', cmd="command1", arg1="arg1")
        Observe('fun2', cmd="command2", args="arg2")

        self.assertTrue(Seen())
        self.assertTrue(Seen(pointName="fun1"))
        self.assertFalse(Seen(pointName="fun3"))
        self.assertTrue(Seen(pointName__startswith="fun", cmd="command1"))

        self.assertSequenceEqual([ {'arg1': 'arg1', 'cmd': 'command1', 'pointName': 'fun1'},
                                   {'args': 'arg2', 'cmd': 'command2', 'pointName': 'fun2'}],
                                 Seen(pointName__startswith="fun"))

        PushFormatter('fun1', lambda args: args['pointName']+":"+args['cmd'])
        self.assertSequenceEqual([ 'fun1:command1',
                                   {'args': 'arg2', 'cmd': 'command2', 'pointName': 'fun2'}],
                                 Seen(pointName__startswith="fun"))

        Observe('fun3', cmd="command3", args="arg2", format=lambda args:args['cmd'])
        self.assertSequenceEqual([ 'fun1:command1', # The PushFormat specification takes precedence
                                    {'args': 'arg2', 'cmd': 'command2', 'pointName': 'fun2'},
                                    'command3'], Seen(pointName__startswith="fun"))


#! \endcond PRIVATE

