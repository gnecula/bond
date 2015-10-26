#!/usr/bin/env bash
this_dir=`dirname $0`
this_dir=`cd $this_dir && pwd`

PYTHONPATH="$this_dir/../.." python -m unittest bst_test
