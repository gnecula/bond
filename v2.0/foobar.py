import bond
import os


@bond.observeFunction(spyPointName='foobarEntry', excludedKeys=('exkey',),
                      formatter=lambda args: {k: str(v).upper() for k,v in args.iteritems()})
def foobar(arg1, arg2='default', arg3="blah", **kwargs):
    print "Inside foobar: arg1={}, arg2={}, arg3={}, kwargs={}".format(arg1, arg2, arg3, kwargs)

foobar(10)
foobar(5, arg3='hello', mykey=70)
foobar(arg3='boo', arg1='foo', exkey='excluded')

# import unittest
# class BondTest(unittest.TestCase):
#     def setUp(self):
#         if not os.path.isdir('/tmp/fooTest'):
#             os.makedirs('/tmp/fooTest', 0777)
#         SetObservationSaveDirectory("/tmp/fooTest")
#         StartTest(self)
#         self.assertTrue(TESTING)
#
#     def testDoers(self):
#         results = [ ]
#
#         PushObserver('fun1', cmd__startswith="myfun",
#                      do__1=(lambda args: results.append(args['cmd']+"__1")),
#                      do=(lambda args: results.append(args['cmd'])))
#         # Now the actual code
#         Observe('fun1', cmd="myfun1")
#         Observe('nofun', cmd="myfun2")
#         Observe('fun1', cmd="myfun3")
#
#         self.assertSequenceEqual(['myfun1', 'myfun1__1', 'myfun3', 'myfun3__1'], results)

