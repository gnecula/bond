package io.github.necula01.bond;

import io.github.necula01.bond.reconcile.ReconcileType;
import com.google.common.base.Optional;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;

/**
 * <p>A JUnit rule used to take care of setup and cleanup when using Bond with JUnit.
 * Instantiating a {@code BondTestRule} as a field and marking it with
 * {@link org.junit.Rule @Rule} will automatically handle all test setup/cleanup;
 * all that needs to be done besides that is to make observations using {@link Bond#spy()}.</p>
 *
 * <p>For example:</p>
 * <pre><code>
 *   class MyTestClass {
 *    {@literal @Rule}
 *     BondTestRule btr = new BondTestRule()
 *
 *    {@literal @Test}
 *     public void testMyMethod() {
 *       Bond.obs(...).spy();
 *     }
 *   }
 * </code></pre>
 */
public class BondTestRule implements TestRule {

  private Optional<File> _observationDirectory = Optional.absent();
  private Optional<ReconcileType> _reconciliationMethod = Optional.absent();

  /**
   * Set the observation directory to be used for this test. If not set, the environment
   * variable {@code BOND_OBSERVATION_DIR} will be used instead. If that is also not set, an
   * Exception will be thrown.
   *
   * @param observationDirectory Directory to store test observations in
   * @return This object to facilitate a builder-style pattern
   */
  public BondTestRule withObservationDirectory(File observationDirectory) {
    _observationDirectory = Optional.fromNullable(observationDirectory);
    return this;
  }

  /**
   * Set the reconciliation method to be used for this test. If not set, the environment
   * variable {@code BOND_RECONCILE} will be used instead. If that is also not set, a
   * default of {@code ABORT} will be used.
   *
   * @param reconcileType Reconciliation method to use
   * @return This object to facilitate a builder-style pattern
   */
  public BondTestRule withReconciliationMethod(ReconcileType reconcileType) {
    _reconciliationMethod = Optional.fromNullable(reconcileType);
    return this;
  }

  @Override
  public Statement apply(final Statement statement, Description description) {
    final String testName = String.format("%s.%s", description.getTestClass().getCanonicalName(),
        description.getMethodName());
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
