#!/usr/bin/env python

from __future__ import print_function

import os
import shutil
import sys

"""
Reconcile the reference observations with the current once
"""


class MergeTool:
    """
    Base class for merge tools
    """

    TOOL_NAMES = ['accept', 'abort', 'console', 'kdiff3']

    @staticmethod
    def select(merge_tool='console'):

        if merge_tool == 'accept':
            return MergeToolAccept()

        if merge_tool == 'abort':
            return MergeToolAbort()

        if merge_tool == 'console':
            return MergeToolConsole()

        if merge_tool == 'kdiff3':
            return MergeToolKdiff3()

    @staticmethod
    def _invoke_command(cmd):
        pass

    @staticmethod
    def _quick_diff(reference_file, current_file, diff_file):
        """
        Compute a diff between files and save it into a diff_file.
        Return True if there are no diffs
        """
        return (0 == os.system('diff -u -w "{0}" "{1}" >"{2}"'.format(reference_file,
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
                  current_file):
        """
        Reconcile the differences
        """

        if not os.path.isfile(reference_file):
            # if we do not have the reference file
            print('Creating refererence observation file for {}: {}'.format(test_name, reference_file))
            shutil.move(current_file, reference_file)
            return True

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
                                           diff_file)
            if merged_file is not None:
                # Accepted differences
                print('Saving updated reference observation file for {}'.format(test_name))
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

    def invoke_tool(self, test_name,
                    reference_file,
                    current_file,
                    diff_file):
        """
        Invoke the actual tool
        @param
        @return either False, for a failed merge, or the name of the file to use as the new reference
        """
        assert False, 'Must override'


class MergeToolAbort(MergeTool):
    """
    Abort all merges
    """

    def invoke_tool(self,
                    test_name,
                    reference_file,
                    current_file,
                    diff_file):
        print('Aborting due to differences for test {}'.format(test_name))
        return None


class MergeToolAccept(MergeTool):
    """
    Accept all changes
    """

    def invoke_tool(self,
                    test_name,
                    reference_file,
                    current_file,
                    diff_file):
        print('Accepting differences for test {}'.format(test_name))
        return current_file


class MergeToolConsole(MergeTool):
    """
    Uses diff and console prompts to accept the changes
    """

    def invoke_tool(self,
                    test_name,
                    reference_file,
                    current_file,
                    diff_file):

        # Show the diff
        print('There were differences in observations for {}: '.format(test_name))
        with open(diff_file, 'r') as f:
            print(f.read())

        prompt = 'Do you want to accept the changes ({}) ? ( [y]es | [k]diff3 | *): '.format(test_name)
        response = raw_input(prompt)
        if response == 'y':
            print('Accepting differences for test {}'.format(test_name))
            return current_file

        if response == 'k':
            return MergeToolKdiff3().invoke_tool(test_name, reference_file, current_file, diff_file)

        print('Rejecting differences for test {}'.format(test_name))
        return None


class MergeToolKdiff3(MergeTool):
    """
    Merge with kdiff3
    """

    def invoke_tool(self,
                    test_name,
                    reference_file,
                    current_file,
                    diff_file):
        merged_file = self._aux_file_name(current_file, 'merged')

        cmd = ('kdiff3 -m "{reference_file}" --L1 "{test_name}_REFERENCE" '
               '"{current_file}" --L2 "{test_name}_CURRENT" '
               ' -o "{merged_file}"').format(reference_file=reference_file,
                                             current_file=current_file,
                                             merged_file=merged_file,
                                             test_name=test_name)

        if 0 == os.system(cmd):
            # Merged ok
            return merged_file
        else:
            if os.path.isfile(merged_file):
                os.unlink(merged_file)
            return None


def reconcile_observations(settings,
                           test_name,
                           reference_file,
                           current_file):
    """
    Reconcile the observations
    :param settings: a settings object
    :param reference_file: the reference file
    :param current_file: the current file with observations
    :return:
    """

    merge_tool = MergeTool.select(settings['merge'])
    return merge_tool.reconcile(test_name,
                                reference_file,
                                current_file)


if __name__ == '__main__':
    import optparse

    optParser = optparse.OptionParser(usage=os.path.basename(__file__),
                                      description='Compare and reconciles differences in Bond observation files')

    optParser.add_option('--merge', dest='merge', action='store', default='console',
                         help='The merge tool to use. Available: ')
    optParser.add_option('--reference', dest='reference', action='store', default=None,
                         help='The reference observation file')
    optParser.add_option('--current', dest='current', action='store', default=None,
                         help='The current observation file')
    optParser.add_option('--test', dest='test', action='store', default=None,
                         help='The name of the test (for UI). Default is to extract from --current')

    (opts, args) = optParser.parse_args()
    if opts.reference is None or not os.path.isfile(opts.reference):
        print('The reference file does not exist: {}'.format(opts.reference), file=sys.stderr)
        sys.exit(1)

    if opts.current is None or not os.path.isfile(opts.current):
        print('The current file does not exist: {}'.format(opts.current), file=sys.stderr)
        sys.exit(1)


    # Guess the test name from the current file
    if opts.test:
        main_test_name = opts.test
    else:
        main_test_name = os.path.splitext(os.path.basename(opts.current))[0]

    if opts.merge:
        main_merge = opts.merge
    else:
        main_merge = os.environ.get('BOND_MERGE', 'console')

    main_settings = dict(merge=main_merge)

    if reconcile_observations(main_settings,
                              main_test_name,
                              opts.reference,
                              opts.current):
        sys.exit(0)
    else:
        sys.exit(1)
