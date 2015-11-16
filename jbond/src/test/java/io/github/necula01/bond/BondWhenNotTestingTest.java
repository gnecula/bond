package io.github.necula01.bond;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;

public class BondWhenNotTestingTest {

  @Test
  public void testIsActive() {
    assertFalse(Bond.isActive());
  }

  @Test
  public void testSpyDoesNothing() {
    Bond.spy();
    assertFalse(Bond.spy("pointName").isPresent());
    assertFalse(Bond.obs("key", "value").spy("pointName").isPresent());
  }

  @Test
  public void testMethodsThrowExceptions() throws IOException {
    try {
      Bond.deployAgent("pointName", new SpyAgent());
      fail();
    } catch (IllegalStateException e) {
      // Do nothing
    }

    try {
      Bond.finishTest();
      fail();
    } catch (IllegalStateException e) {
      // Do nothing
    }
  }

}
