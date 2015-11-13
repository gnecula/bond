package tutorial.binarysearchtree;

import bond.Bond;
import bond.BondTestRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BinarySearchTreeTest {

  // If BOND_RECONCILE is set, we use Bond, else JUnit
  @Rule
  public BondTestRule btr = System.getenv("BOND_RECONCILE") == null ? null : new BondTestRule();

  public Node<Integer> createTree() {
    Node<Integer> tree = new Node<>(8);
    tree.insert(12);
    tree.insert(3);
    tree.insert(4); // Change to 7 to simulate a change in testing fixture
    tree.insert(6);
    return tree;
  }

  @Test
  public void testAdd() {
    // Test adding nodes to the BST
    Node<Integer> tree = createTree();

    if (!Bond.isActive()) {
      assertEquals(8, tree.data.intValue());
      assertEquals(12, tree.right.data.intValue());
      assertNull(tree.right.left);
      assertNull(tree.right.right);
      assertEquals(3, tree.left.data.intValue());
      assertEquals(4, tree.left.right.data.intValue());
      assertEquals(6, tree.left.right.right.data.intValue());
    } else {
      Bond.obs("tree", tree).spy("testAdd");
    }
  }

  @Test
  public void testDelete() {
    Node<Integer> tree = createTree();

    tree.delete(4);

    if (!Bond.isActive()) {
      assertEquals(8, tree.data.intValue());
      assertEquals(12, tree.right.data.intValue());
      assertEquals(3, tree.left.data.intValue());
      assertEquals(6, tree.left.right.data.intValue());
    } else {
      Bond.obs("tree", tree).spy("testDelete");
    }
  }

}
