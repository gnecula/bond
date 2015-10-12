# Helper functions to observe files and directories
import os
import re

def collect_directory_contents(directory,
                               file_filter=None,
                               collect_file_contents=False):
    """
    Collect an object reflecting the contents of a directory
    :param directory: the directory where to start the traversal
    :param file_filter: either a string representing a regular expression on the name of the files
           and directories to be included, or a function that given the directory and the filename
           returns true or false, whether the directory or file should be included.
    :param collect_file_contents: indicates whether to collect the contents of files.
           True means to include contents of all files,
    :return: a dictionary with keys corresponding to basename of files and subdirectories.
          Only files that are allowed by the file_filter are included.
          If the file contents is collected then the dictionary contains a list of lines.
    """
    # TODO: figure out a more general form for this, perhaps using
    #       a configurable visitor to define how to visit each file
    result = { }  # map from file name to file data.
                  # file data is either None (if the contents is not spied),
                  # or an array of lines
    # Prepare the file filter
    file_filter_func = None
    if file_filter:
        if isinstance(file_filter, basestring):
            file_filter_regexp = re.compile(file_filter)
            file_filter_func = lambda rel_file: file_filter_regexp.match(rel_file)
        else:
            # TODO: assert that it is a function
            file_filter_func = file_filter

    collect_file_contents_func = None
    if collect_file_contents:
        if isinstance(collect_file_contents, bool):
            if collect_file_contents:
                collect_file_contents_func = lambda rel_file: True

        elif isinstance(collect_file_contents, basestring):
            include_file_contents_regexp = re.compile(collect_file_contents)
            collect_file_contents_func = lambda rel_file: include_file_contents_regexp.match(rel_file)

        else:
            # TODO: assert that it is a function
            collect_file_contents_func = collect_file_contents

    def recurse(rel_subdir, result_data):
        name_subdir = os.path.join(directory, rel_subdir)
        for basename in os.listdir(name_subdir):
            rel_file = os.path.join(rel_subdir, basename)
            file = os.path.join(directory, rel_file)
            if file_filter_func and not file_filter_func(rel_file):
                continue

            if os.path.isdir(file):
                subresult_data = {}
                result_data[basename] = subresult_data
                recurse(rel_file, subresult_data)
            else:
                if collect_file_contents_func and collect_file_contents_func(rel_file):
                    with open(file, 'r') as f:
                        lines = f.readlines ()
                    result_data[basename] = [l.rstrip() for l in lines ]
                else:
                    result_data[basename] = None


    recurse('', result)
    return result
