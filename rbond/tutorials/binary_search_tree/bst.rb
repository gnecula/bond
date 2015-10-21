# A tutorial on binary-search-trees, by Laurent Luce <http://www.laurentluce.com/>
# Code ported from Python to Ruby; original code from:
# http://www.laurentluce.com/posts/binary-search-tree-library-in-python/
#
# We use this code as the system-under-test


# Tree node: left and right child + data which can be any object
class Node

  attr_accessor :left, :right, :data

  # Node constructor
  # @param data Node data object
  def initialize(data)
    @left = nil
    @right = nil
    @data = data
  end

  # Insert new node with data
  # @param data node data object to insert
  def insert(data)
    if @data.nil?
      @data = data
    else
      if data < @data
        if @left.nil?
          @left = Node.new(data)
        else
          @left.insert(data)
        end
      elsif data > @data
        if @right.nil?
          @right = Node.new(data)
        else
          @right.insert(data)
        end
      end
    end
  end

  # Lookup node containing data
  # @param data node data object to look up
  # @param parent node's parent
  # @return node and node's parent if found or [nil, nil]
  def lookup(data, parent=nil)
    return [self, parent] if data == @data
    if data < @data
      return [nil, nil] if @left.nil?
      @left.lookup(data, self)
    else
      return [nil, nil] if @right.nil?
      @right.lookup(data, self)
    end
  end

  # Delete node containing data
  # @param data node's content to delete
  def delete(data)
    # get node containing data
    node, parent = lookup(data)
    return nil if node.nil?

    children_count = node.children_count

    if children_count == 0
      # if node has no children, just remove it
      if parent.nil?
        @data = nil
      else
        if parent.left == node
          parent.left = nil
        else
          parent.right = nil
        end
      end
    elsif children_count == 1
      # if node has 1 child, replace node with child
      n = node.left.nil? ? node.right : node.left
      if parent.nil?
          @left = n.left
          @right = n.right
          @data = n.data
      else
        if parent.left == node
          parent.left = n
        else
          parent.right = n
        end
      end
    else
      # Node has 2 children; find its successor
      parent = node
      successor = node.right
      until successor.left.nil?
        parent = successor
        successor = successor.left
      end
      # replace node data by its successor data
      node.data = successor.data
      # fix successor's parent's child
      if parent.left == successor
        parent.left = successor.right
      else
        parent.right = successor.right
      end
    end
  end

  # Returns the number of children
  def children_count
    0 if @left.nil? && @right.nil?
    1 if @left.nil? || @right.nil?
    2
  end

end


