import unittest

import setup_paths_test
from bond import bond
from bond_tests import BondTest


class AnnotationTests(unittest.TestCase):

    def setUp(self):
        BondTest.setup_bond_self_tests(self)

    @bond.spy_point()
    def annotated_standard_method(self, arg1, arg2):
        return 'return value'

    @bond.spy_point(enabled_for_groups=('group1', 'group2'))
    def annotated_standard_method_enabled_for_groups(self, arg1, arg2):
        return 'return value'

    @bond.spy_point(enabled_for_groups='group2')
    def annotated_standard_method_enabled_for_single_group(self, arg1, arg2):
        return 'return value'

    def test_standard_annotation(self):
        self.assertEquals('return value', self.annotated_standard_method(1, 2))


    def test_with_formatter(self):
        "Test the standard annotation with a formatter"
        def my_format(obs):
            obs['arg1'] += 1000

        bond.deploy_agent('AnnotationTests.annotated_standard_method',
                          formatter=my_format)
        self.annotated_standard_method(8, 9)

    def test_with_groups_enabled(self):     
        "Test annotations enabled for specific groups, when the group is enabled, as a tuple"
        bond.settings(spy_groups=('group_other', 'group2'))
        self.annotated_standard_method_enabled_for_groups(arg1=1, arg2=2)

    def test_with_groups_enabled2(self):
        "Test annotations enabled for specific groups, when the group is enabled, as a string"
        bond.settings(spy_groups=('group_other', 'group2'))
        self.annotated_standard_method_enabled_for_single_group(arg1=1, arg2=2)

    def test_with_groups_disabled(self):
        "Test annotations enabled for specific groups, when the group is NOT enabled"
        bond.settings(spy_groups='group_other')
        self.annotated_standard_method_enabled_for_groups(arg1=1, arg2=2)

    def test_with_mocking(self):
        bond.deploy_agent('AnnotationTests.annotated_standard_method',
                          result='mocked value')

        self.assertEquals('mocked value', self.annotated_standard_method(arg1=1, arg2=2))

    @staticmethod
    @bond.spy_point()
    def annotated_static_method(arg1='foo', arg2='bar'):
        pass

    def test_static_annotation(self):
        "Test annotations working on static methods"
        self.annotated_static_method(arg2='bar2')

    @bond.spy_point(spy_point_name='testPointName')
    def annotated_with_new_name(self):
        pass

    def test_new_point_name(self):
        self.annotated_with_new_name()

    @bond.spy_point(excluded_keys=('self', 'arg2', 'newArg'))
    def annotated_with_exclusion(self, arg1, arg2, **kwargs):
        pass

    def test_with_exclusion(self):
        self.annotated_with_exclusion('foo', 'bar', newArg='disappears', newArg2='baz')

    @bond.spy_point(require_agent_result=True, spy_return=True)
    def annotated_with_mocking_mandatory(self, arg1=None):
        return 'return value'

    def test_with_mocking_mandatory_applied(self):
        bond.deploy_agent('AnnotationTests.annotated_with_mocking_mandatory',
                          result='mocked value')
        self.assertEqual('mocked value', self.annotated_with_mocking_mandatory())

    def test_with_mocking_mandatory_not_applied(self):
        try:
            self.annotated_with_mocking_mandatory()
            self.fail()
        except AssertionError:
            pass

    @classmethod
    @bond.spy_point(excluded_keys=('cls'))
    def annotated_class_method(cls, arg1):
        pass

    def test_class_method(self):
        AnnotationTests.annotated_class_method('foobar')
        # TODO do we want the class printed for a class method? Not sure

    def test_module_method(self):
        annotated_module_method(1)


@bond.spy_point(spy_return=True)
def annotated_module_method(arg1, arg2='2'):
    return 'something'

if __name__ == '__main__':
    unittest.main()
