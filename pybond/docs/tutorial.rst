.. raw:: html

    <script type='text/javascript' src='_static/bond_doc.js'></script>
    <link rel='stylesheet' href='_static/bond_doc.css' type='text/css'></link>

==========================
Bond Tutorial
==========================

Bond was designed to simplify the development **and maintenance** of automated tests. There are two main uses
of Bond: spying and mocking. Each of these uses is supported by just two Bond functions. We'll discuss spying first.


Part 1: Spying with Bond
----------------------------------

Spying with Bond is meant to replace writting the common ``assertEquals`` calls in your tests, i.e., the validation
that some state variable has some expected value. These assertions are tedious to write, and even more tedious to
update when your code or your test fixture change and you need to update the test. For this reason, people
tend to write fewer ``assertEquals`` than they should.

Consider that you have just implemented binary-search-trees (BST) and want to write tests. You may write the following
code:

.. container:: code-examples

    .. code-block:: python
        :emphasize-lines: 11-15

        def test_bst(self):
            tree = BST()
            tree.insert(8)
            tree.insert(12)
            tree.insert(3)
            tree.insert(4)
            tree.insert(6)

            # WITHOUT BOND: Add self.assertEquals here to verify the position in the tree
            # of all the data points, in the order in which they were inserted
            self.assertEquals(8, tree.data)
            self.assertEquals(12, tree.right.data)
            self.assertEquals(3, tree.left.data)
            self.assertEquals(4, tree.left.right.data)
            self.assertEquals(6, tree.left.right.right.data)

    .. code-block:: ruby

        some ruby code { here }


That is a lot of ``assertEquals``, and in fact, it is not even a complete test, because you'd have to
test, e.g., that 6 is a leaf node.

The alternative with Bond is as follows:

.. container:: code-examples

    .. code-block:: python
        :emphasize-lines: 2, 13

        def test_bst(self):
            bond.start_test(self)

            tree = BST()
            tree.insert(8)
            tree.insert(12)
            tree.insert(3)
            tree.insert(4)
            tree.insert(6)

            # WITH BOND: record the value of the tree variable, and compare it
            # with previous recordings.
            bond.spy('first test', tree=tree)

What is happening there is that we call the ``bond.spy`` function to tell Bond to record the value of the
``tree`` variable. This value is recorded in a file saved by default in a subdirectory called ``test_observations``.
This file should be checked in your repository along with your sources. Next time Bond runs the same test it will
compare the current observation with the reference one. If there is a difference, you will get the opportunity
to interact with Bond to select what you want to be the new reference.

.. code-block:: javascript

    [
    {
        "__spy_point__": "first test",
        "tree": {
            "data": 8,
            "left": {
                "data": 3,
                "right": {
                    "data": 4,
                    "right": {
                        "data": 6
                    }
                }
            },
            "right": {
                "data": 12
            }
        }
    }
    ]


Note that this observation acts implicitly as 11 equality assertions (5 for the data values, and 6 more for
the null pointers on the leaves). Furthermore, the assertions are presented in a structure that is much easier
to read that a sequence of equality assertions. Finally, with Bond your test code contains only the names
of the variables you want to assert on; the values they are equal to are saved separated from your test.
This will turn out to be crucial next.

If you need to make a change in the code, or in the testing setup, it is very tedious to fix the ``assertEquals``.
Let's say that you decide that you get a better test coverage with a different tree where instead of 4 you want to
insert 7 in the tree. If you run the traditional test, you will see the familiar test failure:

.. code-block:: diff

    ======================================================================
    FAIL: testAdd1 (bst_tests.NodeTest)
    ----------------------------------------------------------------------
    Traceback (most recent call last):
      File "bond/pybond/tutorials/binary_search_tree/bst_tests.py", line 49, in test_bst
        self.assertEquals(4, tree.left.right.data)
    AssertionError: 4 != 7

Not only does your test fail on the first assertion, but it turns out that you have to chase and fix
several of the assertions because the tree structure has changed. This is a common scenario when
your tests are aggressive about validating the data.

With Bond, there is absolutely no change to the test! Instead, the test fails when it tries to
compare.

.. code-block:: diff

    --- bond/pybond/tutorials/binary_search_tree/test_observations/NodeTest/test_bst.json
    +++ bond/pybond/tutorials/binary_search_tree/test_observations/NodeTest/test_bst_now.json
    @@ -6,8 +6,8 @@
             "left": {
                 "data": 3,
                 "right": {
    -                "data": 4,
    -                "right": {
    +                "data": 7,
    +                "left": {
                         "data": 6
                     }
                 }

    There were differences in observations for NodeTest.test_bst:
    Do you want to accept the changes (NodeTest.test_bst) ? ( [y]es | [k]diff3 | *):

Furthermore, if you click "k" at the above prompt, Bond will invoke a visual merging tool, such as
``kdiff3``, that allows you to navigate all differences, see the context in which they appeared by
inspecting nearby observations, select easily for each different, or for all, whether the
new observed behavior is correct. If so, Bond will save the new observation file as
future reference. Voila! You have just updated the expected values with a click of a button. Bond gives you
deep assertions about your test while keeping the assertion maintenance cost low.

.. image:: _static/kdiff3_bst1.png


Part 2: Mocking with Bond
--------------------------------

Coming soon ...

