package io.github.necula01.bond;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

public class BondTest {

  @Rule
  public BondTestRule btr = new BondTestRule();

  @Test
  public void testBasicSpying() {
    Bond.spy();
    Bond.spy("spyPoint");
  }

  @Test
  public void testBasicSpyingWithObservations() {
    Bond.obs("obs1", "someValue")
        .obs("obs2", 50L)
        .obs("obs3", true)
        .spy();

    Bond.obs("obs4", new int[] {50, 20, 30})
        .obs("obs5", Lists.newArrayList("item1", "item2"))
        .spy("pointName");
  }

  @Test
  public void testAgentWithFilters() {
    SpyAgent agent = new SpyAgent()
        .withFilterKeyContains("key", "foo")
        .withResult("result");

    Bond.deployAgent("myPoint", agent);

    Optional<String> notMatching1 = Bond.spy("myPoint", String.class);
    Optional<String> matching = Bond.obs("key", "foobar").spy("myPoint", String.class);
    Optional<String> notMatching2 = Bond.obs("key", "fo").spy("myPoint", String.class);

    Bond.obs("notMatching1", notMatching1)
        .obs("matching", matching)
        .obs("notMatching2", notMatching2)
        .spy("results");
  }

  @Test
  public void testLaterAgentsTakePrecedence() {
    SpyAgent backupAgent = new SpyAgent()
        .withResult("backup agent result");
    SpyAgent primaryAgent = new SpyAgent()
        .withFilterKeyContains("key", "foo")
        .withResult("primary agent result");

    Bond.deployAgent("myPoint", backupAgent);
    Bond.deployAgent("myPoint", primaryAgent);

    Optional<String> primaryAgentResult = Bond.obs("key", "foobar").spy("myPoint", String.class);
    Optional<String> backupAgentResult = Bond.obs("key", "notbar").spy("myPoint", String.class);

    Bond.obs("primaryAgentResult", primaryAgentResult)
        .obs("backupAgentResult", backupAgentResult)
        .spy("results");
  }

  @Test
  public void testAgentWithDoers() {
    SpyAgent agent = new SpyAgent()
        .withDoer(new Doer() {
          @Override
          public void accept(Map<String, Object> map) {
            Bond.spy("doer 1 spy point");
          }
        })
        .withDoer(new Doer() {
          @Override
          public void accept(Map<String, Object> map) {
            Bond.spy("doer 2 spy point");
          }
        })
        .withFilterKeyContains("key", "foo");

    Bond.deployAgent("myPoint", agent);

    Bond.obs("key", "foo").spy("myPoint");
  }

  @Test
  public void testAgentWithResult() {
    SpyAgent agent = new SpyAgent()
        .withResult("agent result");
    Bond.deployAgent("myPoint", agent);

    Bond.obs("agentResult", Bond.spy("myPoint")).spy();
  }

  @Test
  public void testAgentWithResulter() {
    SpyAgent agent = new SpyAgent()
                         .withResult(new Resulter() {
                           @Override
                           public String accept(Map<String, Object> map) {
                             if (map.containsKey("foo")) {
                               return "foo result";
                             } else {
                               return "no foo result";
                             }
                           }
                         });
    Bond.deployAgent("myPoint", agent);

    Bond.obs("agent result with foo", Bond.obs("foo", "bar").spy("myPoint"))
        .obs("agent result without foo", Bond.spy("myPoint"))
        .spy();
  }

  private class SuperClass { }
  private class SubClass extends SuperClass { }

  @Test
  public void testAgentWithIncorrectResultType() {
    SpyAgent stringAgent = new SpyAgent()
        .withResult("string");

    Bond.deployAgent("myPoint", stringAgent);
    
    try {
      Optional<Integer> agentResult = Bond.spy("myPoint", Integer.class);
      Bond.spy("past spy point");
    } catch (IllegalSpyAgentException e) {
      Bond.spy("catch clause");
    }

    SpyAgent subclassAgent = new SpyAgent().withResult(new SubClass());
    SpyAgent superclassAgent = new SpyAgent().withResult(new SuperClass());

    Bond.deployAgent("myPoint", subclassAgent);

    Optional<SuperClass> agentResult = Bond.spy("myPoint", SuperClass.class);

    Bond.deployAgent("myPoint", superclassAgent);

    try {
      Optional<SubClass> agentSubclassResult = Bond.spy("myPoint", SubClass.class);
      Bond.spy("past spy point");
    } catch (IllegalSpyAgentException e) {
      Bond.spy("catch clause");
    }
  }

  @Test
  public void testWithUncheckedException() {
    SpyAgent agent = new SpyAgent()
        .withException(new RuntimeException("SpyAgent Exception"));
    Bond.deployAgent("myPoint", agent);

    try {
      Bond.spy("myPoint");
    } catch (RuntimeException e) {
      Bond.obs("exceptionMessage", e.toString()).spy();
    }
  }

  @Test
  public void testWithUncheckedExcepter() {
    SpyAgent agent = new SpyAgent()
                         .withException(new Excepter() {
                           @Override
                           public RuntimeException accept(Map<String, Object> map) {
                             if (map.containsKey("foo")) {
                               return new RuntimeException("Foo Exception");
                             } else {
                               return new RuntimeException("No Foo Exception");
                             }
                           }
                         });
    Bond.deployAgent("myPoint", agent);

    try {
      Bond.spy("myPoint");
    } catch (RuntimeException e) {
      Bond.obs("exceptionMessage", e.toString()).spy();
    }

    try {
      Bond.obs("foo", "bar").spy("myPoint");
    } catch (RuntimeException e) {
      Bond.obs("exceptionMessage", e.toString()).spy();
    }
  }

  private class BondTestException extends Exception {
    public BondTestException(String message) {
      super(message);
    }
  }

  private class BondOtherTestException extends Exception {
    public BondOtherTestException(String message) {
      super(message);
    }
  }

  @Test
  public void testWithCheckedException() {
    SpyAgent agent = new SpyAgent()
                         .withException(new BondTestException("Test Exception"));
    Bond.deployAgent("myPoint", agent);

    try {
      Bond.obs("key", "val").spyWithException("myPoint", BondTestException.class);
    } catch (BondTestException e) {
      Bond.obs("exceptionMessage", e.toString()).spy();
    }
  }

  @Test
  public void testWithIncorrectCheckedException() {
    SpyAgent agent = new SpyAgent()
                         .withException(new BondOtherTestException("Test Exception"));
    Bond.deployAgent("myPoint", agent);

    try {
      Bond.obs("key", "val").spyWithException("myPoint", BondTestException.class);
    } catch (BondTestException e) {
      Bond.spy("caught BondTestException");
    } catch (IllegalSpyAgentException e) {
      Bond.obs("exceptionMessage", e.toString()).spy();
    }
  }

  @Test
  public void testWithCheckedExcepter() {
    SpyAgent agent = new SpyAgent()
                         .withException(new CheckedExcepter() {
                           @Override
                           public Exception accept(Map<String, Object> map) {
                             if (map.containsKey("foo")) {
                               return new BondTestException("Foo Exception");
                             } else {
                               return new BondTestException("No Foo Exception");
                             }
                           }
                         });
    Bond.deployAgent("myPoint", agent);

    try {
      Bond.obs("key", "val").spyWithException("myPoint", BondTestException.class);
    } catch (BondTestException e) {
      Bond.obs("exceptionMessage", e.toString()).spy();
    }

    try {
      Bond.obs("foo", "bar").spyWithException("myPoint", BondTestException.class);
    } catch (BondTestException e) {
      Bond.obs("exceptionMessage", e.toString()).spy();
    }
  }

  @Test
  public void testWithCustomTestName() {
    Bond.setCurrentTestName("bond.MyCustomTestName");
    Bond.spy("custom test name spy point");
  }

  @Test
  public void testWithCustomTestNameWithSpecialCharacters() {
    Bond.setCurrentTestName("bond.custom test special!");
    Bond.spy("custom test name spy point");
  }
}
