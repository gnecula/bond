import unittest

import setup_paths_test
from bond import bond
from bond_tests import BondTest

class AnnotationTests(unittest.TestCase):


    # Some functions for introspecting internal Bond state
    def internal_last_observation(self):
        bond_instance = bond.Bond.instance()
        return bond_instance.observations[len(bond_instance.observations) - 1]

    def setUp(self):
        # For some of the tests, we specy spy_groups
        spy_groups = None
        if self._testMethodName == 'testWithGroups_1':
            spy_groups = ('group_other', 'group2')
        elif self._testMethodName == 'testWithGroups_2':
            spy_groups = ('group_other')

        BondTest.setupUpBondSelfTests(self,
                                      spy_groups=spy_groups)

    @bond.spy_point()
    def annotatedStandardMethod(self, arg1, arg2):
        return 'return value'

    @bond.spy_point(enabled_for_groups=('group1', 'group2'))
    def annotatedStandardMethodEnabledForGroups(self, arg1, arg2):
        return 'return value'

    def testStandardAnnotation(self):
        self.assertEquals('return value', self.annotatedStandardMethod(1, 2))

    def testWithGroupsEnabled(self):
        "Test annotations enabled for specific groups, when the group is enabled"
        self.annotatedStandardMethodEnabledForGroups(arg1=1, arg2=2)

    def testWithGroupsDisabled(self):
        "Test annotations enabled for specific groups, when the group is NOT enabled"
        self.annotatedStandardMethodEnabledForGroups(arg1=1, arg2=2)

    def testWithMocking(self):
        bond.deploy_agent('AnnotationTests.annotatedStandardMethod',
                          result='mocked value')

        self.assertEquals('mocked value', self.annotatedStandardMethod(arg1=1, arg2=2))


    @staticmethod
    @bond.spy_point()
    def annotatedStaticMethod(arg1='foo', arg2='bar'):
        pass

    def testStaticAnnotation(self):
        "Test annotations working on static methods"
        self.annotatedStaticMethod(arg2='bar2')


    @bond.spy_point(spy_point_name='testPointName')
    def annotatedWithNewName(self):
        pass

    def testNewPointName(self):
        self.annotatedWithNewName()


    @bond.spy_point(excluded_keys=('self', 'arg2', 'newArg'))
    def annotatedWithExclusion(self, arg1, arg2, **kwargs):
        pass

    def testWithExclusion(self):
        self.annotatedWithExclusion('foo', 'bar', newArg='disappears', newArg2='baz')


    @bond.spy_point(mock_mandatory=True,
                    spy_return=True)
    def annotatedWithMockingMandatory(self, arg1=None):
        return 'return value'

    def testWithMockingMandatoryApplied(self):
        bond.deploy_agent('AnnotationTests.annotatedWithMockingMandatory',
                          result='mocked value')
        self.assertEqual('mocked value', self.annotatedWithMockingMandatory())

    def testWithMockingMandatoryNotApplied(self):
        try:
            self.annotatedWithMockingMandatory()
            self.fail()
        except AssertionError:
            pass

    @classmethod
    @bond.spy_point(excluded_keys=('cls'))
    def annotatedClassMethod(cls, arg1):
        pass

    def testClassMethod(self):
        AnnotationTests.annotatedClassMethod('foobar')
        # TODO do we want the class printed for a class method? Not sure


    def testModuleMethod(self):
        annotatedModuleMethod(1)


@bond.spy_point(spy_return=True)
def annotatedModuleMethod(arg1, arg2='2'):
    return 'something'

if __name__ == '__main__':
    unittest.main()
