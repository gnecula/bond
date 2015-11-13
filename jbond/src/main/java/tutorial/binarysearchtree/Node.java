package tutorial.binarysearchtree;

// A tutorial on binary-search-trees, by Laurent Luce <http://www.laurentluce.com/>
// Code originally from: http://www.laurentluce.com/posts/binary-search-tree-library-in-python/
// ported to Java
//
// We use this code as the system-under-test

/**
 * Represents a binary tree node; stores a left and right child, plus some data
 * which is comparable. Duplicates are not stored.
 */
public class Node<T extends Comparable<T>> {

  public T data;
  public Node<T> left = null;
  public  Node<T> right = null;

  /**
   * Initialize a node with no children.
   *
   * @param pData Data object to store inside this node
   */
  public Node(T pData) {
    data = pData;
  }

  /**
   * Insert a new node as a child of this node with data as content.
   *
   * @param pData Content of new node
   */
  public void insert(T pData) {
    if (data == null) {
      data = pData;
    } else {
      if (pData.compareTo(data) < 0) {
        if (left == null) {
          left = new Node<>(pData);
        } else {
          left.insert(pData);
        }
      } else if (pData.compareTo(data) > 0) {
        if (right == null) {
          right = new Node<>(pData);
        } else {
          right.insert(pData);
        }
      }
    }
  }

  /**
   * Lookup the node containing data. Returns null if none found.
   *
   * @param pData Data to search for
   * @return Node containing data
   */
  public Node<T> lookup(T pData) {
    return lookup(pData, null).n1;
  }

  /**
   * Lookup the node containing data with parent node parent.
   *
   * @param pData Data to search for
   * @param parent Current node's parent
   * @return Node containing data and its parent or NodePair containing
   *         both nulls if not found
   */
  private NodePair lookup(T pData, Node<T> parent) {
    if (pData.compareTo(data) < 0) {
      if (left == null) {
        return new NodePair(null, null);
      }
      return left.lookup(pData, this);
    } else if (pData.compareTo(data) > 0) {
      if (right == null) {
        return new NodePair(null, null);
      }
      return right.lookup(pData, this);
    } else {
      return new NodePair(this, parent);
    }
  }

  /**
   * Delete node containing data
   *
   * @param pData Data of node to delete
   */
  public void delete(T pData) {
    NodePair np = lookup(pData, null);
    Node<T> node = np.n1;
    Node<T> parent = np.n2;

    if (node == null) {
      return;
    }

    int childCount = node.childCount();

    if (childCount == 0) {
      // Node has no children, just delete it
      if (parent == null) {
        // Node to be deleted is this
        data = null;
      } else {
        if (parent.left == node) {
          parent.left = null;
        } else {
          parent.right = null;
        }
      }
    } else if (childCount == 1) {
      // Just replace node with its child
      Node<T> n;
      if (node.left == null) {
        n = node.right;
      } else {
        n = node.left;
      }
      if (parent == null) {
        left = n.left;
        right = n.right;
        data = n.data;
      } else {
        if (parent.left == node) {
          parent.left = n;
        } else {
          parent.right = n;
        }
      }
    } else {
      // Node has 2 children, find correct successor
      Node<T> successorParent = node;
      Node<T> successor = node.right;
      while (successor.left != null) {
        successorParent = successor;
        successor = successor.left;
      }
      // Replace node data by successor data
      node.data = successor.data;
      // Fix successor's parent's child
      if (successorParent.left == successor) {
        successorParent.left = successor.right;
      } else {
        successorParent.right = successor.right;
      }
    }

  }

  /**
   * Get number of children of this node
   *
   * @return Number of children this node has
   */
  private int childCount() {
    int cnt = 0;
    if (left != null) {
      cnt++;
    }
    if (right != null) {
      cnt++;
    }
    return cnt;
  }

  private class NodePair {
    Node<T> n1;
    Node<T> n2;

    NodePair(Node<T> n1, Node<T> n2) {
      this.n1 = n1;
      this.n2 = n2;
    }
  }
}
