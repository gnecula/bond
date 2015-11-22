package bond.spypoint;

import bond.Bond;
import bond.BondTestRule;
import bond.IllegalSpyAgentException;
import bond.SpyAgent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.swing.*")
@MockPolicy(SpyPointTestMockPolicy.class)
public class SpyPointTest {

  static class TestDependency {
    @SpyPoint
    public int annotatedStandardMethod(Object arg1, String arg2) {
      return 42;
    }

    @SpyPoint
    public Object annotatedMethodVarags(String arg1, String... args) {
      return null;
    }

    @SpyPoint
    public static String annotatedStaticMethod(String arg) {
      return arg;
    }

    @SpyPoint(spyPointName = "testName")
    public void annotatedMethodWithName() { }

    @SpyPoint(spyResult = true)
    public int annotatedMethodSpyResult() {
      return 42;
    }

    @SpyPoint(requireMock = true)
    public String annotatedMethodRequireMock() {
      return "test return";
    }

    public String methodWithDependency(String arg) {
      TestSecondaryDependency tsd = new TestSecondaryDependency();
      return tsd.annotatedMethod(arg);
    }

    public String methodCallingPrivate() {
      return dependedOnMethod();
    }

    @SpyPoint
    private String dependedOnMethod() {
      return "foo";
    }
  }

  static class TestSecondaryDependency {

    @SpyPoint
    public String annotatedMethod(String arg1) {
      return "foo";
    }

  }

  @Rule
  public BondTestRule btr = new BondTestRule();

  TestDependency dep;

  @Before
  public void setup() {
    dep = new TestDependency();
  }

  @Test
  public void testBasicSpyPoint() {
    int ret = dep.annotatedStandardMethod(42, "foo");
    Bond.obs("ret", ret).spy();
  }

  @Test
  public void testSpyPointWithReturn() {
    SpyAgent agent = new SpyAgent().withResult(7);
    Bond.deployAgent("TestDependency.annotatedStandardMethod", agent);

    int ret = dep.annotatedStandardMethod(42, "foo");
    Bond.obs("ret", ret).spy();
  }

  @Test
  public void testSpyPointWithIncorrectReturnType() {
    SpyAgent agent = new SpyAgent().withResult("test");
    Bond.deployAgent("TestDependency.annotatedStandardMethod", agent);

    try {
      dep.annotatedStandardMethod(42, "foo");
      Bond.spy("no catch");
    } catch (IllegalSpyAgentException e) {
      Bond.spy("catch");
    }
  }

  @Test
  public void testSpyPointWithVarargs() {
    dep.annotatedMethodVarags("arg1", "arg2", "arg3");
  }

  @Test
  public void testSpyPointWithStaticMethod() {
    TestDependency.annotatedStaticMethod("foo");
  }

  @Test
  public void testSpyPointWithName() {
    dep.annotatedMethodWithName();
  }

  @Test
  public void testSpyPointWithSpyResult() {
    dep.annotatedMethodSpyResult();
  }

  @Test
  public void testSpyPointWithRequireReturn() {
    try {
      dep.annotatedMethodRequireMock();
      Bond.spy("no catch");
    } catch (IllegalStateException e) {
      Bond.obs("exception", e.toString()).spy("catch");
    }
  }

  @Test
  public void testMethodWithDependencyOnOtherClass() {
    dep.methodWithDependency("foo");
  }

  @Test
  public void testMethodWithPrivateDependency() {
    dep.methodCallingPrivate();
  }

}

class SpyPointTestMockPolicy extends BondMockPolicy {
  @Override
  public String getPackageName() {
    return "bond.spypoint";
  }
}
