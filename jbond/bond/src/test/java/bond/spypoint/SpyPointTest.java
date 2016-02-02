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
    public int annotatedStandardMethod(Object arg0, String arg1) {
      return 42;
    }

    @SpyPoint
    public Object annotatedMethodVarags(String arg0, String... arg1) {
      return null;
    }

    @SpyPoint
    public static String annotatedStaticMethod(String arg0) {
      return arg0;
    }

    @SpyPoint(spyPointName = "testName")
    public void annotatedMethodWithName() { }

    @SpyPoint(spyResult = true)
    public int annotatedMethodSpyResult() {
      return 42;
    }

    @SpyPoint(requireAgentResult = true)
    public String annotatedMethodRequireMock() {
      return "test return";
    }

    @SpyPoint(mockOnly = true)
    public String annotatedMethodMockOnly() {
      return "test return";
    }

    public String methodWithDependency(String arg0) {
      TestSecondaryDependency tsd = new TestSecondaryDependency();
      return tsd.annotatedMethod(arg0);
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
    public String annotatedMethod(String arg0) {
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
  public void testSpyPointWithMockOnly() {
    Bond.obs("val", dep.annotatedMethodMockOnly()).spy("unmockedReturn");

    Bond.deployAgent("TestDependency.annotatedMethodMockOnly",
        new SpyAgent().withResult("Mocked Value"));
    Bond.obs("val", dep.annotatedMethodMockOnly()).spy("mockedReturn");

    Bond.deployAgent("TestDependency.annotatedMethodMockOnly",
        new SpyAgent().withResult("Mocked Value").withSkipSaveObservation(false));
    Bond.obs("val", dep.annotatedMethodMockOnly()).spy("mockedReturn");
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
  public String[] getPackageNames() {
    return new String[] {"bond.spypoint"};
  }
}
