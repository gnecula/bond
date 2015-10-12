import unittest

import setup_paths_test
from bond import bond

class AnnotationTests(unittest.TestCase):

    class BondMock:

        def __init__(self):
            self.nextReturnValue = None

        def spy(self, spyPointName, formatter=None, **kwargs):
            self.spyPointName = spyPointName
            self.observationDictionary = kwargs
            if self.nextReturnValue is not None:
                value = self.nextReturnValue
                self.nextReturnValue = None
                return value
            return bond.Bond.NO_MOCK_RESPONSE

        def setNextReturnValue(self, value):
            self.nextReturnValue = value

        def getLastSpyPointName(self):
            return self.spyPointName

        def getLastObservationDictionary(self):
            return self.observationDictionary
    bondMock = BondMock()

    @bond.spy_point(bond=bondMock)
    def annotatedStandardMethod(self, arg1, arg2):
        return 'return value'

    def testStandardAnnotation(self):
        self.assertEquals('return value', self.annotatedStandardMethod(1, 2))
        # These tests later can be replaced with bond.observe but for now use asserts
        self.assertEqual('annotatedStandardMethod', AnnotationTests.bondMock.getLastSpyPointName())
        self.assertDictEqual({'arg1': 1, 'arg2': 2}, AnnotationTests.bondMock.getLastObservationDictionary())

    def testWithMocking(self):
        AnnotationTests.bondMock.setNextReturnValue('mocked value')
        self.assertEquals('mocked value', self.annotatedStandardMethod(arg1=1, arg2=2))
        self.assertEqual('annotatedStandardMethod', AnnotationTests.bondMock.getLastSpyPointName())
        self.assertDictEqual({'arg1': 1, 'arg2': 2}, AnnotationTests.bondMock.getLastObservationDictionary())

    @staticmethod
    @bond.spy_point(bond=bondMock)
    def annotatedStaticMethod(arg1='foo', arg2='bar'):
        pass

    def testStaticAnnotation(self):
        self.annotatedStaticMethod(arg2='bar2')
        # These tests later can be replaced with bond.observe but for now use asserts
        self.assertEqual('annotatedStaticMethod', AnnotationTests.bondMock.getLastSpyPointName())
        self.assertDictEqual({'arg2': 'bar2'}, AnnotationTests.bondMock.getLastObservationDictionary())

    @bond.spy_point(spy_point_name='testPointName', bond=bondMock)
    def annotatedWithNewName(self):
        pass

    def testNewPointName(self):
        self.annotatedWithNewName()
        self.assertEqual('testPointName', AnnotationTests.bondMock.getLastSpyPointName())

    @bond.spy_point(bond=bondMock, excludedKeys=('self', 'arg2', 'newArg'))
    def annotatedWithExclusion(self, arg1, arg2, **kwargs):
        pass

    def testWithExclusion(self):
        self.annotatedWithExclusion('foo', 'bar', newArg='disappears', newArg2='baz')
        self.assertDictEqual({'arg1': 'foo', 'newArg2': 'baz'}, AnnotationTests.bondMock.getLastObservationDictionary())

    @bond.spy_point(bond=bondMock, mock_mandatory=True)
    def annotatedWithMockingMandatory(self, arg1=None):
        return 'return value'

    def testWithMockingMandatoryApplied(self):
        AnnotationTests.bondMock.setNextReturnValue('mocked value')
        self.assertEqual('mocked value', self.annotatedWithMockingMandatory())

    def testWithMockingMandatoryNotApplied(self):
        try:
            self.annotatedWithMockingMandatory()
            self.fail()
        except AssertionError:
            pass

    @classmethod
    @bond.spy_point(bond=bondMock)
    def annotatedClassMethod(cls, arg1):
        pass

    def testClassMethod(self):
        AnnotationTests.annotatedClassMethod('foobar')
        # TODO do we want the class printed for a class method? Not sure
        self.assertEqual({'arg1': 'foobar', 'cls': AnnotationTests}, AnnotationTests.bondMock.getLastObservationDictionary())


if __name__ == '__main__':
    unittest.main()
