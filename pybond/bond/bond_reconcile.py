
import os, shutil

"""
Reconcile the newly saved observations with the previously saved ones
"""

class MergeTool:
    """
    Base class for merge tools
    """

    TOOL_NAMES = ['ignore', 'accept', 'abort', 'diff', 'kdiff3']

    TOOLS = {}  # Will be initialized

    @staticmethod
    def select(merge_tool='console'):

        if merge_tool == 'ignore':
            return MergeToolIgnore()

        if merge_tool == 'accept':
            return MergeToolAccept()

        if merge_tool == 'abort':
            return MergeToolAbort()

        if merge_tool == 'console':
            return MergeToolDiff()

        if merge_tool == 'kdiff3':
            return MergeToolKdiff3()


    @staticmethod
    def _invoke_command(cmd):
        pass

    def reconcile(self,
                  test_name,
                  reference_file,
                  current_file):
        """
        Reconcile the differences
        """

        # if we do not have reference file:
        #    save reference
        #    return true
        # run regular diff and save differences
        # if no differences:
        #    return true
        # if there are differences
        #   invoke_tool(test_name, reference_file, current_file, differences, merged_file)
        #    - returns: fail, success (save file), prompt


class MergeToolIgnore(MergeTool):
    """
    Ignore all merges
    """
    pass

class MergeToolAbort(MergeTool):
    """
    Abort all merges
    """
    pass

 class MergeToolAccept(MergeTool):
    """
    Accept all changes
    """
    pass


class MergeToolDiff(MergeTool):
    """
    Uses diff and console prompts to accept the changes
    """
    pass


    def format_cmd(self, test_name, ):
        return "diff -u -w "+fname+".json "+fname+"_now.json >"+fname+".diff"


class MergeToolKdiff3(MergeTool):
    """
    Merge with kdiff3
    """
    pass


def reconcile_observations(settings,
                           reference_file,
                           current_file):
    """
    Reconcile the observations
    :param settings: a settings object
    :param reference_file: the reference file
    :param current_file: the current file with observations
    :return:
    """

    merge_tool = MergeTool.select(settings['merge_tool'])

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




if __name__ == '__main__':
    main()
