================================================
Bond Demo - Testing Binary Search Trees
===============================================

This directory contains an implementation of binary search trees in the file ``bst.py`` and a couple
of unit tests in ``bst_test.py``.

Take a look at the two tests in ``bst_test.py``. They are written using two styles of assertions, without Bond,
and with Bond. The flavor is controlled by the environment variable ``BOND_RECONCILE`` (if defined, then use Bond).
If you run the tests with ``BOND_RECONCILE=console`` or ``BOND_RECONCILE=kdiff3``  then you will be using Bond-style assertions.

0. If you want to start in an intial state, delete the Bond test observations:

   .. code::

      rm -rf test_observations

#. Inspect the unit tests

   - You will see that the Bond-style is much less verbose because one call to the ``spy`` function essentially
     is equivalent to several calls to ``assertEquals`` 
  - The Bond-style testing uses the ``self.dumpTree`` function to turn the
    tree into a data structure using only lists and dictionaries, that can be
    serialized to JSON.
    
#. Run the unit tests without Bond

   .. code::

      ./run_tests.sh 
      
#. Run the unit tests with Bond. Since this is the first run, you will be
   asked to accept the initial set of reference observations:

   .. code::

      BOND_RECONCILE=console ./run_tests.sh

   At the console prompt you should inspect the initial observations, and
   press "y" to accept them as reference. (You can also press "k" to start the
   ``kdiff3`` merging tool.)

#. Now let's pretend that there is a change to the code or to the tests. Edit
   the ``bond_test.py`` file and change the line that inserts 4 into the tree,
   to insert 7.

#. If you run the unit tests without Bond, you will see an assertion failure

      .. code::

         ./run_tests.sh
  
        ======================================================================
        FAIL: testAdd1 (bst_test.NodeTest)
        ----------------------------------------------------------------------
        Traceback (most recent call last):
          File "/Users/necula/Source/bond/pybond/tutorials/binary_search_tree/bst_test.py", line 42, in testAdd1
            self.assertEquals(4, tree.left.right.data)
        AssertionError: 4 != 7
      
        ======================================================================
        FAIL: testDelete (bst_test.NodeTest)
        ----------------------------------------------------------------------
        Traceback (most recent call last):
          File "/Users/necula/Source/bond/pybond/tutorials/binary_search_tree/bst_test.py", line 61, in testDelete
            self.assertEquals(6, tree.left.right.data)
        AssertionError: 6 != 7

      You'd have to make many manual changes to the assertions to fix the test
      now.

#. If you run the test with Bond, you will get an interactive menu that will
   allow you to control the updating of the assertions.

   .. code::

      BOND_RECONCILE=console ./run_tests.sh  
