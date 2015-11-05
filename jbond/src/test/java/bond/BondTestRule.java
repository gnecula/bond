package bond;

import com.google.common.base.Optional;
import junit.framework.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;

public class BondTestRule implements TestRule {

  private Optional<File> _observationDirectory = Optional.absent();
  private Optional<Reconcile> _reconciliationMethod = Optional.absent();

  public BondTestRule withObservationDirectory(File observationDirectory) {
    _observationDirectory = Optional.fromNullable(observationDirectory);
    return this;
  }

  public BondTestRule withReconciliationMethod(Reconcile reconcile) {
    _reconciliationMethod = Optional.fromNullable(reconcile);
    return this;
  }

  @Override
  public Statement apply(final Statement statement, Description description) {
    String className = description.getTestClass().getCanonicalName();
    final String testName = className + "." + description.getMethodName();
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
