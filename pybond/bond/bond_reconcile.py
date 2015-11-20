#!/usr/bin/env python

from __future__ import print_function

import os
import re
import shutil
import sys

try:
    # Import bond safely
    from bond import spy_point
except ImportError:
    spy_point = lambda **kw: lambda f: f


"""
Reconcile the reference observations with the current ones
"""


class ReconcileTool:
    """
    Base class for reconcile tools
    """

    @staticmethod
    def select(reconcile_tool=None):

        if reconcile_tool == 'accept':
            return ReconcileToolAccept()

        if reconcile_tool == 'abort':
            return ReconcileToolAbort()

        if reconcile_tool == 'console':
            return ReconcileToolConsole()

        if reconcile_tool == 'kdiff3':
            return ReconcileToolKdiff3()

        if reconcile_tool is None:
            # Look at the environment variable BOND_RECONCILE
            reconcile_tool = os.environ.get('BOND_RECONCILE', 'abort')
            if reconcile_tool is not None:
                return ReconcileTool.select(reconcile_tool)

        assert False, 'Unrecognized bond_reconcile tool name: {}'.format(reconcile_tool)


    @staticmethod
    @spy_point(enabled_for_groups='bond_self_test',
               require_agent_result=True)
    def _invoke_command(cmd):
        """
        Invoke a shell command. Return the exit code.
        :param cmd:
        :return:
        """
        return os.system(cmd)

    @staticmethod
    @spy_point(enabled_for_groups='bond_self_test',
               require_agent_result=True,
               spy_result=True)
    def _read_console(prompt):
        """
        A function to read from the console
        """
        return raw_input(prompt)


    @staticmethod
    @spy_point(enabled_for_groups='bond_self_test')
    def _print(what):
        """
        A function to do the printing, so we can spy on it
        :param what:
        :return:
        """
        print(what)

    @staticmethod
    def _quick_diff(reference_file, current_file, diff_file):
        """
        Compute a diff between files and save it into a diff_file.
        Return True if there are no diffs
        """
        # TODO: implicit dependency on a 'diff' command line tool with the same usage syntax that you're expecting
        return (0 == ReconcileTool._invoke_command('diff -u -b "{0}" "{1}" >"{2}"'.format(reference_file,
                                                                                      current_file,
                                                                                      diff_file)))

    @staticmethod
    def _aux_file_name(current_file,
                       flavor):
        """
        The name of the auxiliary (e.g., diff, merged) file to use
        """
        return current_file + "." + flavor

    def __init__(self):
        pass

    def reconcile(self,
                  test_name,
                  reference_file,
                  current_file,
                  no_save=None):
        """
        Reconcile the differences
        @param test_name: the name of the test (for messages)
        @param reference_file: the name of the reference observation file
        @param current_file: the name of the current observation file
        :param no_save: if present, then disallows saving a new reference file.
               This parameter should be a string explaining why saving is disallowed.
        """

        if not os.path.isfile(reference_file):
            # if we do not have the reference file, pretend we have an empty one
            ReconcileTool._print('WARNING: No reference observation file found for {}: {}'.format(test_name, reference_file))

            with open(reference_file, 'w') as f:
                pass
            # We continue

        # Compute a quick difference
        diff_file = self._aux_file_name(current_file, "diff")
        try:
            if self._quick_diff(reference_file, current_file, diff_file):
                # There are no differences
                return True

            # There are differences
            merged_file = self.invoke_tool(test_name,
                                           reference_file,
                                           current_file,
                                           diff_file,
                                           no_save=no_save)
            if merged_file is not None:
                # Accepted differences
                if no_save:
                    ReconcileTool._print("Not saving reference observation file for {}: {}".format(test_name,
                                                                                                   no_save))
                else:
                    ReconcileTool._print('Saving updated reference observation file for {}'.format(test_name))
                    shutil.move(merged_file, reference_file)
                return True
            else:
                return False

        finally:
            # Delete the files
            if os.path.isfile(diff_file):
                os.unlink(diff_file)
            if os.path.isfile(current_file):
                os.unlink(current_file)

    def show_diff(self,
                  test_name,
                  diff_file):
        """
        Show the diff, and return it
        """
        diffs = ''
        with open(diff_file, 'r') as f:
            diffs = f.read()
        if diffs:
            ReconcileTool._print('There were differences in observations for {}: '.format(test_name))
            ReconcileTool._print(diffs)
            # Print it again at the end; makes it easy to see in the console what test just failed
            ReconcileTool._print('There were differences in observations for {}: '.format(test_name))
        else:
            ReconcileTool._print('No differences in observations for {}: '.format(test_name))
        return diffs

    def invoke_tool(self, test_name,
                    reference_file,
                    current_file,
                    diff_file,
                    no_save=None):
        """
        Invoke the actual tool
        @param
        @return either False, for a failed merge, or the name of the file to use as the new reference
        """
        assert False, 'Must override'


class ReconcileToolAbort(ReconcileTool):
    """
    Abort all merges
    """

    def invoke_tool(self,
                    test_name,
                    reference_file,
                    current_file,
                    diff_file,
                    no_save=None):
        if not no_save:
            self.show_diff(test_name, diff_file)
        ReconcileTool._print('Aborting (reconcile=abort) due to differences for test {}'.format(test_name))
        return None


class ReconcileToolAccept(ReconcileTool):
    """
    Accept all changes
    """

    def invoke_tool(self,
                    test_name,
                    reference_file,
                    current_file,
                    diff_file,
                    no_save=None):
        diffs = self.show_diff(test_name, diff_file)
        if not no_save:
            ReconcileTool._print('Accepting (reconcile=accept) differences for test {}'.format(test_name))
        return current_file


class ReconcileToolConsole(ReconcileTool):
    """
    Uses diff and console prompts to accept the changes
    """

    def invoke_tool(self,
                    test_name,
                    reference_file,
                    current_file,
                    diff_file,
                    no_save=None):

        while True:
            if no_save:
                prompt = 'Observations are shown for {}. Saving them not allowed because test failed.'.format(
                    test_name,
                ) + ' Use the diff option to show the differences. ([k]diff3 | [d]iff | [e] errors | *): '
            else:
                # Show the diff
                self.show_diff(test_name, diff_file)
                prompt = 'Do you want to accept the changes ({}) ? ( [y]es | [k]diff3 | *): '.format(test_name)

            response = ReconcileTool._read_console(prompt)

            if response == 'k':
                return ReconcileToolKdiff3().invoke_tool(test_name, reference_file, current_file, diff_file,
                                                         no_save=no_save)

            if response == 'd' and no_save:
                self.show_diff(test_name, diff_file)
                continue

            if response == 'e' and no_save:
                ReconcileTool._print("Test {} had errors:\n{}".format(test_name, no_save))
                continue

            if response == 'y' and not no_save:
                ReconcileTool._print('Accepting differences for test {}'.format(test_name))
                return current_file

            if not no_save:
                ReconcileTool._print('Rejecting differences for test {}'.format(test_name))
            return None



class ReconcileToolKdiff3(ReconcileTool):
    """
    Merge with kdiff3
    """

    def invoke_tool(self,
                    test_name,
                    reference_file,
                    current_file,
                    diff_file,
                    no_save=None):

        if no_save:
            response = ReconcileTool._read_console(
                "\n!!! MERGING NOT ALLOWED for {}: {}. Want to start kdiff3? ([y] | *): ".format(test_name,
                                                                                                 no_save))
            if response != "y":
                return None

            cmd = ('kdiff3 "{reference_file}" --L1 "{test_name}_REFERENCE" '
                   '"{current_file}" --L2 "{test_name}_CURRENT" ').format(
                reference_file=reference_file,
                current_file=current_file,
                test_name=test_name)
        else:
            merged_file = self._aux_file_name(current_file, 'merged')

            cmd = ('kdiff3 -m "{reference_file}" --L1 "{test_name}_REFERENCE" '
                   '"{current_file}" --L2 "{test_name}_CURRENT" '
                   ' -o "{merged_file}"').format(reference_file=reference_file,
                                                 current_file=current_file,
                                                 merged_file=merged_file,
                                                 test_name=test_name)

        print(cmd)
        code = ReconcileTool._invoke_command(cmd)
        if no_save:
            return None
        else:
            if code == 0 :
                # Merged ok
                return merged_file
            else:
                if os.path.isfile(merged_file):
                    os.unlink(merged_file)
                return None


def reconcile_observations(settings,
                           test_name,
                           reference_file,
                           current_file,
                           no_save=None):
    """
    Reconcile the observations
    :param settings: a settings object
    :param reference_file: the reference file
    :param current_file: the current file with observations
    :param no_save: If present, then saving of new references is not allowed. This parameter
            should be a short string explaining why saving is not allowed.
    :return:
    """

    reconcile_tool = ReconcileTool.select(settings.get('reconcile'))
    return reconcile_tool.reconcile(test_name,
                                    reference_file,
                                    current_file,
                                    no_save=no_save)


if __name__ == '__main__':
    import optparse

    optParser = optparse.OptionParser(usage=os.path.basename(__file__),
                                      description='Compare and reconciles differences in Bond observation files')

    optParser.add_option('--reconcile', dest='reconcile', action='store', default=None,
                         help='The reconcile tool to use. Available: accept, abort, console, kdiff3.')
    optParser.add_option('--reference', dest='reference', action='store', default=None,
                         help='The reference observation file')
    optParser.add_option('--current', dest='current', action='store', default=None,
                         help='The current observation file')
    optParser.add_option('--test', dest='test', action='store', default=None,
                         help='The name of the test (for UI). Default is to extract from --current')
    optParser.add_option('--no-save', dest='no_save', action='store', default=None,
                         help='If given, the reason why saving of new references is not allowed')
    (opts, args) = optParser.parse_args()
    if opts.reference is None:
        sys.exit(1)

    if opts.current is None or not os.path.isfile(opts.current):
        print('The current file does not exist: {}'.format(opts.current), file=sys.stderr)
        sys.exit(1)


    # Guess the test name from the current file
    if opts.test:
        main_test_name = opts.test
    else:
        main_test_name = os.path.splitext(os.path.basename(opts.current))[0]

    if opts.reconcile:
        main_reconcile = opts.reconcile
    else:
        main_reconcile = os.environ.get('BOND_RECONCILE', 'console')

    main_settings = dict(reconcile=main_reconcile)

    if reconcile_observations(main_settings,
                              main_test_name,
                              opts.reference,
                              opts.current,
                              no_save=opts.no_save):
        sys.exit(0)
    else:
        sys.exit(1)
