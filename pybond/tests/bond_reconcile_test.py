import os
import shutil
import unittest
import re

import setup_paths_test
from bond import bond

from bond import bond_reconcile
from bond.bond_helpers import collect_directory_contents

from bond_test import setup_bond_self_test, teardown_bond_self_test

class ReconcileTest(unittest.TestCase):

    def setUp(self):
        self.testing_observation_dir = '/tmp/bondObservations Dir'  # Intentionally put a space in the path
        self.reference_file = os.path.join(self.testing_observation_dir, 'reference.json')
        self.current_file = os.path.join(self.testing_observation_dir, 'reference_now.json')

        self.reference_file_content = """
[
{
   "__spy_point__" : "point 1",
   val" : 12345
}
]
"""


        setup_bond_self_test(self)
        # By default allow diffs
        bond.deploy_agent('bond_reconcile._invoke_command',
                          cmd__startswith='diff ',
                          result=bond.AGENT_RESULT_CONTINUE)

        self.console_reply = []  # List of strings to reply when we try to read from console
        bond.deploy_agent('bond_reconcile._read_console',
                          result=lambda obs: self.console_reply.pop(0) if self.console_reply else bond.AGENT_RESULT_CONTINUE)


        self.kdiff3_result = 0
        def mock_kdiff3(obs):
            "Mock call to kdiff3"
            if not self.during_test:
                return bond.AGENT_RESULT_CONTINUE

            if self.kdiff3_result == 0:
                # Success, create the merged file
                # First, get the file name from the command line
                m = re.search(r'-o\s*"([^"]+)"', obs['cmd'])
                if m:
                    with open(m.group(1), 'w') as f:
                        f.write("The merge result")

            return self.kdiff3_result


        bond.deploy_agent('bond_reconcile._invoke_command',
                          cmd__startswith='kdiff3 ',
                          result=mock_kdiff3)


        # A special formatter for the printing of diffs
        def _print_formatter(obs):
            # We want to observe each line individually
            # And we remove dates, which diff insists on putting in
            res = []
            for l in obs['what'].split('\n'):
                l = re.sub(r'\d{4}-\d{2}-\d{2} \d{2}:.*', 'date', l)
                res.append(l)
            obs['what'] = res
        bond.deploy_agent('bond_reconcile._print',
                          what__contains='@@',
                          formatter=_print_formatter)

    def tearDown(self):
        # So that Bond mocking is turned off
        teardown_bond_self_test(self)

    def prepare_observations(self,
                             reference_file_content=None,
                             current_file_content=None):
        """
        Prepare reference and current files
        :param reference_file_content:
        :param current_file_content:
        :return:
        """
        if os.path.isdir(self.testing_observation_dir):
            shutil.rmtree(self.testing_observation_dir)
        os.makedirs(self.testing_observation_dir)

        if reference_file_content:
            with open(self.reference_file, 'w') as f:
                f.write(reference_file_content)

        if current_file_content:
            with open(self.current_file, 'w') as f:
                f.write(current_file_content)

    def invoke_top_reconcile(self,
                             reconcile=None,
                             no_save=None):
        """
        Helper function to invoke the top-level reconcile
        :param reconcile:
        :return:
        """
        result = bond_reconcile.reconcile_observations(dict(reconcile=reconcile),
                                                       'test 1',
                                                       self.reference_file,
                                                       self.current_file,
                                                       no_save=no_save)
        # Observe the directory
        bond.spy('invoke_top_reconcile_results',
                 result=result,
                 obsevation_dir=collect_directory_contents(self.testing_observation_dir,
                                                           collect_file_contents=True))


    def test_no_reference(self, no_save=None):
        "Test with no reference file"
        self.console_reply = ['y']  # Do accept
        self.prepare_observations(reference_file_content=None,
                                  current_file_content=self.reference_file_content)
        self.invoke_top_reconcile(reconcile='console',
                                  no_save=no_save)

    def test_no_reference_no_save(self):
        "Test with no reference file, and no_save"
        self.test_no_reference(no_save='I do not want to save')



    def test_same(self):
        "Test with reference and current the same"
        self.prepare_observations(reference_file_content=self.reference_file_content,
                                  current_file_content=self.reference_file_content)
        self.invoke_top_reconcile(reconcile='console')

    def helper_test_reconcile(self,
                              reconcile='',
                              no_save=None):
        "Test with reference and current the same"
        self.prepare_observations(reference_file_content=self.reference_file_content,
                                  current_file_content=self.reference_file_content.replace('12345', 'abcde'))
        self.invoke_top_reconcile(reconcile=reconcile,
                                  no_save=no_save)

    def test_reconcile_accept(self):
        "Test with the accept tool"
        self.helper_test_reconcile(reconcile='accept')

    def test_reconcile_accept_no_save(self):
        "Test with the accept tool and no_save"
        self.helper_test_reconcile(reconcile='accept',
                                   no_save="Don't want to save. Pretend there was an error")

    def test_reconcile_abort(self):
        self.helper_test_reconcile(reconcile='abort')

    def test_reconcile_console0(self):
        "Test with console tool, answer: y"
        self.console_reply = ['y']  # Do accept
        self.helper_test_reconcile(reconcile='console')

    def test_reconcile_console0_no_save(self):
        "Test with console tool, answer: y, but no_save"
        self.console_reply = ['y']  # Do accept
        self.helper_test_reconcile(reconcile='console',
                                   no_save='No saving, period.')


    def test_reconcile_console0_no_save_diff(self):
        "Test with console tool, answer: d, to get the diff, then y, but no_save"
        self.console_reply = ['d', 'e', 'y']  # Do accept
        self.helper_test_reconcile(reconcile='console',
                                   no_save='No saving, period.')


    def test_reconcile_console1(self):
        self.console_reply = ['n']  # Do not accept
        self.helper_test_reconcile(reconcile='console')


    def test_reconcile_console_k0(self):
        "Test with console tool, answer=k, then Kdiff3 merges"
        self.console_reply = ['k']  # Switch to kdiff3
        self.kdiff3_result = 0    # Kdiff3 is happy
        self.helper_test_reconcile(reconcile='console')

    def test_reconcile_console_k0_no_save(self):
        "Test with console tool, answer=k, then confirm, then Kdiff3 in diff mode"
        self.console_reply = ['k', 'y']  # Switch to kdiff3, then confirm
        self.kdiff3_result = 0    # Kdiff3 is happy
        self.helper_test_reconcile(reconcile='console', no_save='No saving, I say so')

    def test_reconcile_console_k0_no_save_deny(self):
        "Test with console tool, answer=k, then deny Kdiff3"
        self.console_reply = ['k', 'n']  # Switch to kdiff3, then confirm
        self.kdiff3_result = 0    # Kdiff3 is happy
        self.helper_test_reconcile(reconcile='console', no_save='No saving, I say so')


    def test_reconcile_console_k1(self):
        self.console_reply = ['k']  # Switch to kdiff3
        self.kdiff3_result = 1    # Kdiff3 is NOT happy
        self.helper_test_reconcile(reconcile='console')

    def test_reconcile_kdiff3_0(self):
        self.kdiff3_result = 0    # Kdiff3 is happy
        self.helper_test_reconcile(reconcile='kdiff3')

    def test_reconcile_kdiff3_0_no_save(self):
        self.console_reply = ['y']  # Confirm kdiff3
        self.kdiff3_result = 0    # Kdiff3 is happy
        self.helper_test_reconcile(reconcile='kdiff3', no_save='not needed')

    def test_reconcile_kdiff3_1(self):
        self.kdiff3_result = 1    # Kdiff3 is NOT happy
        self.helper_test_reconcile(reconcile='kdiff3')

    def test_reconcile_kdiff3_1_no_save(self):
        self.console_reply = ['y']  # Confirm kdiff3
        self.kdiff3_result = 1    # Kdiff3 is NOT happy
        self.helper_test_reconcile(reconcile='kdiff3', no_save='not needed')
