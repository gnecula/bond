#
# Tests for BST trees
# See comments in README.rst
#
import os
import unittest
from bst import Node
from bond import bond

class NodeTest(unittest.TestCase):

    def setUp(self):
        # We will run the test with Bond if BOND_RECONCILE is defined
        self.with_bond = os.environ.get('BOND_RECONCILE')
        if self.with_bond:
            bond.start_test(self)


    def create_tree_1(self):
        tree = Node(8)
        tree.insert(12)
        tree.insert(3)
        tree.insert(4)  # Change to 7 to simulate a change in testing fixture
        tree.insert(6)
        return tree

    def testAdd1(self):
        """
        Test adding nodes to the BST. No Bond.
        :return:
        """
        tree = self.create_tree_1()

        # Add self.assertEquals here to verify the position in the tree
        # of all the data points, in the order in which they were inserted
        if not self.with_bond:
            self.assertEquals(8, tree.data)
            self.assertEquals(12, tree.right.data)
            self.assertEquals(None, tree.right.left)
            self.assertEquals(None, tree.right.right)
            self.assertEquals(3, tree.left.data)
            self.assertEquals(4, tree.left.right.data)
            self.assertEquals(6, tree.left.right.right.data)
        else:
            # Add here a call to bond.spy to spy the tree
            bond.spy('testAdd1', tree=NodeTest.dumpTree(tree))



    def testDelete(self):
        tree = self.create_tree_1()

        tree.delete(4)

        # Add self.assertEquals here to verify the position in the tree
        # of all the data points, in the order in which they were inserted
        if not self.with_bond:
            self.assertEquals(8, tree.data)
            self.assertEquals(12, tree.right.data)
            self.assertEquals(3, tree.left.data)
            self.assertEquals(6, tree.left.right.data)
        else:
            # Add here a call to bond.spy to spy the tree
            bond.spy('testDelete1', tree=NodeTest.dumpTree(tree))

        tree.delete(8)

        if not self.with_bond:
            # Add self.assertEquals here to verify the position in the tree
            # of all the data points, in the order in which they were inserted
            self.assertEquals(12, tree.data)
            self.assertEquals(3, tree.left.data)
            self.assertEquals(6, tree.left.right.data)
        else:
            # Add here a call to bond.spy to spy the tree
            bond.spy('testDelete2', tree=NodeTest.dumpTree(tree))


    @staticmethod
    def dumpTree(node):
        # A helper function to convert a tree to a dictionary mirroring the tree contents
        res = { 'data' : node.data }
        if node.left:
            res['left'] = NodeTest.dumpTree(node.left)
        if node.right:
            res['right'] = NodeTest.dumpTree(node.right)
        return res
