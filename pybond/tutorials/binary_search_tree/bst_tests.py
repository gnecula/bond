#
# Tests for BST trees
import os
import unittest
from bst import Node
from bond import bond

class NodeTest(unittest.TestCase):

    @staticmethod
    def dumpTree(node):
        # A helper function to convert a tree to a dictionary mirroring the tree contents
        res = { 'data' : node.data }
        if node.left:
            res['left'] = NodeTest.dumpTree(node.left)
        if node.right:
            res['right'] = NodeTest.dumpTree(node.right)
        return res

    def setUp(self):
        self.variant = 1
        bond.settings(observation_directory=os.path.dirname(__file__)+'/test_observations')
        bond.start_test(self)


    def testAdd1(self):
        tree = Node(8)
        tree.insert(12)
        tree.insert(3)
        tree.insert(4)
        tree.insert(6)


        # Add self.assertEquals here to verify the position in the tree
        # of all the data points, in the order in which they were inserted
        self.assertEquals(8, tree.data)
        self.assertEquals(12, tree.right.data)
        self.assertEquals(3, tree.left.data)
        self.assertEquals(4, tree.left.right.data)
        self.assertEquals(6, tree.left.right.right.data)

        # Add here a call to bond.spy to spy the tree
        bond.spy('testAdd1', tree=NodeTest.dumpTree(tree))

    def testDelete(self):
        tree = Node(8)
        tree.insert(12)
        tree.insert(3)
        tree.insert(4)
        tree.insert(6)

        tree.delete(4)

        # Add self.assertEquals here to verify the position in the tree
        # of all the data points, in the order in which they were inserted
        self.assertEquals(8, tree.data)
        self.assertEquals(12, tree.right.data)
        self.assertEquals(3, tree.left.data)
        self.assertEquals(6, tree.left.right.data)

        # Add here a call to bond.spy to spy the tree
        bond.spy('testDelete1', tree=NodeTest.dumpTree(tree))

        tree.delete(8)

        # Add self.assertEquals here to verify the position in the tree
        # of all the data points, in the order in which they were inserted
        self.assertEquals(12, tree.data)
        self.assertEquals(3, tree.left.data)
        self.assertEquals(6, tree.left.right.data)

        # Add here a call to bond.spy to spy the tree
        bond.spy('testDelete2', tree=NodeTest.dumpTree(tree))

