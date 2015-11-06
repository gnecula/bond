package bond;

import bond.reconcile.ReconcileType;
import com.google.common.base.Optional;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;

public class BondTestRule implements TestRule {

  private Optional<File> _observationDirectory = Optional.absent();
  private Optional<ReconcileType> _reconciliationMethod = Optional.absent();
  private Optional<String> _testName = Optional.absent();

  public BondTestRule withTestName(String name) {
    _testName = Optional.fromNullable(name);
    return this;
  }

  public BondTestRule withObservationDirectory(File observationDirectory) {
    _observationDirectory = Optional.fromNullable(observationDirectory);
    return this;
  }

  public BondTestRule withReconciliationMethod(ReconcileType reconcileType) {
    _reconciliationMethod = Optional.fromNullable(reconcileType);
    return this;
  }

  @Override
  public Statement apply(final Statement statement, Description description) {
    final String testName;
    if (_testName.isPresent()) {
      testName = _testName.get();
    } else {
      testName = String.format("%s.%s", description.getTestClass().getCanonicalName(),
          description.getMethodName());
    }
    //String testFile = description.getTestClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        Bond.startTest(testName);
        if (_observationDirectory.isPresent()) {
          Bond.setObservationDirectory(_observationDirectory.get());
        }
        if (_reconciliationMethod.isPresent()) {
          Bond.setReconciliationMethod(_reconciliationMethod.get());
        }
        try {
          statement.evaluate();
        } catch (Throwable e) {
          Bond.finishTest(e);
          throw e;
        }
        if (!Bond.finishTest()) {
          org.junit.Assert.fail("BOND_FAIL. Pass BOND_RECONCILE=[kdiff3|console|accept] " +
              "environment variable to reconcile the observations.");
        }
      }
    };
  }

}
