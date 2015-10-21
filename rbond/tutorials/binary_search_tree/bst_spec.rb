# Tests for BST trees
require 'rspec'
require 'bond/bond_spec_helper'
require 'bond'
require './bst'

describe Node do
  include_context :bond

  let (:variant) { 1 } # Change this variant to 2 to simulate a change

  it 'should add nodes to the BST correctly, testing without Bond' do
    tree = create_tree_1

    # Add expect().to here to verify the position in the tree
    # of all the data points, in the order in which they were inserted
    expect(tree.data).to eq(8)
    expect(tree.right.data).to eq(12)
    expect(tree.left.data).to eq(3)
    expect(tree.left.right.data).to eq(4)
    expect(tree.left.right.right.data).to eq(6)
  end

  it 'should add nodes to the BST correctly, testing with Bond' do
    tree = create_tree_1

    # Add here a call to bond.spy to spy the tree
    bond.spy('test_add_1', tree: dump_tree(tree))
  end

  it 'should correctly delete nodes from the BST' do
    tree = Node.new(8)
    tree.insert(12)
    tree.insert(3)
    tree.insert(4)
    tree.insert(6)

    tree.delete(4)

    # Add expect().to here to verify the position in the tree
    # of all the data points, in the order in which they were inserted
    expect(tree.data).to eq(8)
    expect(tree.right.data).to eq(12)
    expect(tree.left.data).to eq(3)
    expect(tree.left.right.data).to eq(6)

    # Add here a call to bond.spy to spy the tree
    bond.spy('test_delete_1', tree: dump_tree(tree))

    tree.delete(8)

    # Add expect().to here to verify the position in the tree
    # of all the data points, in the order in which they were inserted
    expect(tree.data).to eq(12)
    expect(tree.left.data).to eq(3)
    expect(tree.left.right.data).to eq(6)

    # Add here a call to bond.spy to spy the tree
    bond.spy('test_delete_2', tree: dump_tree(tree))
  end

  # A helper function to convert a tree to a dictionary mirroring the tree contents
  def dump_tree(node)
    res = { data: node.data }
    res[:left] = dump_tree(node.left) unless node.left.nil?
    res[:right] = dump_tree(node.right) unless node.right.nil?
    res
  end

  def create_tree_1
    tree = Node.new(8)
    tree.insert(12)
    tree.insert(3)
    puts variant
    if variant == 1
      tree.insert(4)
    else
      tree.insert(7)
    end
    tree.insert(6)
    return tree
  end
end
