#!/usr/bin/env bash
this_dir=`dirname $0`
this_dir=`cd $this_dir && pwd`
BOND_MERGE=console PYTHONPATH="$this_dir/../.." python -m unittest discover -p '*tests.py' -v
