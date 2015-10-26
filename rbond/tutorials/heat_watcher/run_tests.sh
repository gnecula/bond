#!/usr/bin/env bash
this_dir=`dirname $0`
this_dir=`cd $this_dir && pwd`
BOND_MERGE=console RUBYLIB="$this_dir/../../lib" rspec heat_watcher_spec.rb
