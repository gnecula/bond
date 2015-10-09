import unittest
from bond import bond


class BondAnnotationTests(unittest.TestCase):

    class BondMock:

        def __init__(self):
            self.nextReturnValue = None

        def observe(self, spyPointName, formatter=None, **kwargs):
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

    @bond.observeFunction(bond=bondMock)
    def annotatedStandardMethod(self, arg1, arg2):
        return 'return value'

    def testStandardAnnotation(self):
        self.assertEquals('return value', self.annotatedStandardMethod(1, 2))
        # These tests later can be replaced with bond.observe but for now use asserts
        self.assertEqual('annotatedStandardMethod', BondAnnotationTests.bondMock.getLastSpyPointName())
        self.assertDictEqual({'arg1': 1, 'arg2': 2}, BondAnnotationTests.bondMock.getLastObservationDictionary())

    def testWithMocking(self):
        BondAnnotationTests.bondMock.setNextReturnValue('mocked value')
        self.assertEquals('mocked value', self.annotatedStandardMethod(arg1=1, arg2=2))
        self.assertEqual('annotatedStandardMethod', BondAnnotationTests.bondMock.getLastSpyPointName())
        self.assertDictEqual({'arg1': 1, 'arg2': 2}, BondAnnotationTests.bondMock.getLastObservationDictionary())

    @staticmethod
    @bond.observeFunction(bond=bondMock)
    def annotatedStaticMethod(arg1='foo', arg2='bar'):
        pass

    def testStaticAnnotation(self):
        self.annotatedStaticMethod(arg2='bar2')
        # These tests later can be replaced with bond.observe but for now use asserts
        self.assertEqual('annotatedStaticMethod', BondAnnotationTests.bondMock.getLastSpyPointName())
        self.assertDictEqual({'arg2': 'bar2'}, BondAnnotationTests.bondMock.getLastObservationDictionary())

    @bond.observeFunction(spyPointName='testPointName', bond=bondMock)
    def annotatedWithNewName(self):
        pass

    def testNewPointName(self):
        self.annotatedWithNewName()
        self.assertEqual('testPointName', BondAnnotationTests.bondMock.getLastSpyPointName())

    @bond.observeFunction(bond=bondMock, excludedKeys=('self', 'arg2', 'newArg'))
    def annotatedWithExclusion(self, arg1, arg2, **kwargs):
        pass

    def testWithExclusion(self):
        self.annotatedWithExclusion('foo', 'bar', newArg='disappears', newArg2='baz')
        self.assertDictEqual({'arg1': 'foo', 'newArg2': 'baz'}, BondAnnotationTests.bondMock.getLastObservationDictionary())

    @bond.observeFunction(bond=bondMock, mockMandatory=True)
    def annotatedWithMockingMandatory(self, arg1=None):
        return 'return value'

    def testWithMockingMandatoryApplied(self):
        BondAnnotationTests.bondMock.setNextReturnValue('mocked value')
        self.assertEqual('mocked value', self.annotatedWithMockingMandatory())

    def testWithMockingMandatoryNotApplied(self):
        try:
            self.annotatedWithMockingMandatory()
            self.fail()
        except AssertionError:
            pass

    @classmethod
    @bond.observeFunction(bond=bondMock)
    def annotatedClassMethod(cls, arg1):
        pass

    def testClassMethod(self):
        BondAnnotationTests.annotatedClassMethod('foobar')
        # TODO do we want the class printed for a class method? Not sure
        self.assertEqual({'arg1': 'foobar', 'cls': BondAnnotationTests}, BondAnnotationTests.bondMock.getLastObservationDictionary())


if __name__ == '__main__':
    unittest.main()
