package bond;

import bond.reconcile.ReconcileType;
import com.google.common.base.Optional;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.lang.reflect.Type;

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
  private Serializer _serializer = new Serializer()
                                       .withFloatPrecision(Bond.DEFAULT_FLOAT_DOUBLE_PRECISION)
                                       .withDoublePrecision(Bond.DEFAULT_FLOAT_DOUBLE_PRECISION);

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

  /**
   * Set a custom serialization method for type.
   * See {@link com.google.gson.GsonBuilder#registerTypeAdapter(Type, Object)}.
   *
   * @param type The type for this adapter to apply to
   * @param typeAdapter The adapter with custom serialization logic
   * @return This object to facilitate a builder-style pattern
   */
  public BondTestRule withTypeAdapter(Type type, Object typeAdapter) {
    _serializer = _serializer.withTypeAdapter(type, typeAdapter);
    return this;
  }

  /**
   * Set a custom serialization method for baseType and its subtypes.
   * See {@link com.google.gson.GsonBuilder#registerTypeHierarchyAdapter(Class, Object)}.
   *
   * @param baseType The baseType for this adapter to apply to
   * @param typeAdapter The adapter with custom serialization logic
   * @return This object to facilitate a builder-style pattern
   */
  public BondTestRule withTypeHierarchyAdapter(Class<?> baseType, Object typeAdapter) {
    _serializer = _serializer.withTypeHierarchyAdapter(baseType, typeAdapter);
    return this;
  }

  /**
   * Specify that for objects of type clazz, simply call toString() on the object
   * to serialize it instead of doing a full JSON serialization.
   *
   * @param clazz Type for this to apply to
   * @param <T> Same as clazz
   * @return This object to facilitate a builder-style pattern
   */
  public <T> BondTestRule withToStringSerialization(Class<T> clazz) {
    _serializer = _serializer.withToStringSerialization(clazz);
    return this;
  }

  /**
   * Set the amount of precision to be used when serializing floats and doubles.
   *
   * @param places Number of places after the decimal to keep in serialized form
   * @return This object to facilitate a builder-style pattern
   */
  public BondTestRule withFloatDoublePrecision(final int places) {
    _serializer = _serializer.withFloatPrecision(places).withDoublePrecision(places);
    return this;
  }

  /**
   * Set the amount of precision to be used when serializing doubles.
   *
   * @param places Number of places after the decimal to keep in serialized form
   * @return This object to facilitate a builder-style pattern
   */
  public BondTestRule withDoublePrecision(final int places) {
    _serializer = _serializer.withDoublePrecision(places);
    return this;
  }

  /**
   * Set the amount of precision to be used when serializing floats.
   *
   * @param places Number of places after the decimal to keep in serialized form
   * @return This object to facilitate a builder-style pattern
   */
  public BondTestRule withFloatPrecision(final int places) {
    _serializer = _serializer.withFloatPrecision(places);
    return this;
  }

  @Override
  public Statement apply(final Statement statement, Description description) {
    final String testName = String.format("%s.%s", description.getTestClass().getCanonicalName(),
        description.getMethodName());
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        Bond.startTest(testName, _serializer);
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
