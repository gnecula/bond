#!/usr/bin/env bash
this_dir=`dirname $0`
this_dir=`cd $this_dir && pwd`
BOND_RECONCILE=console RUBYLIB="$this_dir/../../lib" rspec bst_spec.rb
