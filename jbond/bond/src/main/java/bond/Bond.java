package bond;

import bond.reconcile.ReconcileType;
import bond.reconcile.Reconciler;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>The main entrypoint to Bond, providing static methods to be used for
 * observing the behavior of your program.</p>
 *
 * <p>This class uses a builder-style pattern to build up sets of observations
 * which will then be stored. Calling {@link #obs(String, Object) Bond.obs()}
 * returns an {@link Observation} object which you can then add more observations
 * to. To store all of the observations, call {@link Observation#spy()}. Note
 * that you can also call {@link #spy()} directly on Bond; this will record that
 * that you have spied with no observations. Different variations of {@code spy}
 * are available for different expectations about what the spy point may return or
 * throw: {@link #spy(String)} for a generic Object return type,
 * {@link #spy(String, Class)} for a type-safe specified return type,
 * {@link #spyWithException(String)} for throwing Exceptions, and
 * {@link #spyWithException(String, Class)} for throwing a specific type of checked
 * Exception. All of these methods have counterparts on the {@link Observation}
 * object as well. For example:</p>
 *
 * <pre><code>
 * Bond.obs("key", "value").obs("key2", "value2").spy();
 * SpyResult&lt;Object&gt; ret = Bond.obs("key", "value").spy("pointName");
 * SpyResult&lt;Integer&gt; ret = Bond.obs("key", "value").spy("pointName", Integer.class);
 *
 * try {
 *   Bond.obs("key", "value").spyWithException("pointName", Exception.class);
 * } catch (Exception e) {
 *   ...
 * }
 *
 * try {
 *   Bond.obs("key", "value").spyWithException("pointName", MyCheckedException.class);
 * } catch (MyCheckedException e) {
 *   ...
 * }
 * </code></pre>
 *
 * <p>The value of an observation can be any object, and will be serialized to JSON using
 * {@link Serializer}.</p>
 *
 * <p>Upon finishing a test, the current set of observations is compared against a reference set
 * (or, against an emptry string, if no reference set of observations exists). If the current
 * set does not match the reference set, the differences will be reconciled according to the
 * current reconciliation method (set via {@link #setReconciliationMethod(ReconcileType)},
 * {@link BondTestRule#withReconciliationMethod(ReconcileType)}, or the {@code BOND_RECONCILE}
 * environment variable - see {@link ReconcileType} for available types). The accepted set of
 * observations will be saved to a file located in the base observation directory (set via
 * {@link #setObservationDirectory(File)}, {@link BondTestRule#withObservationDirectory(File)},
 * or the {@code BOND_OBSERVATION_DIR} environment variable), with exact location determined by the name
 * of the test. Any periods in the test name will be translated into a directory hierarchy, and
 * any special (non-alphanumeric) characters will be replaced by underscores. For example, with
 * a test name of "bond.package.MyTest!", the observations will be saved at
 * {@code {observationDirectory}bond/package/MyTest_.json}.</p>
 */
public class Bond {

  static final int DEFAULT_FLOAT_DOUBLE_PRECISION = 6;

  private static final Splitter CLASS_SPLITTER = Splitter.on(".").trimResults();
  private static final Splitter LINE_SPLITTER = Splitter.on("\n");

  private static Optional<String> _currentTest = Optional.absent();
  private static Map<String, List<SpyAgent>> _agentMap = new HashMap<>();
  private static List<String> _observationJsons = new ArrayList<>();
  private static Serializer _serializer = new Serializer();
  private static Optional<File> _observationDirectory = Optional.absent();
  private static Optional<ReconcileType> _reconciliationMethod = Optional.absent();

  /**
   * Private constructor to enforce singleton-style pattern
   */
  private Bond() {}

  /**
   * Spies this point with no observations.
   *
   * @param spyPointName Name of the point being spied on.
   * @return The result of the agent deployed for this point, if any,
   *         else an absent {@link SpyResult}.
   */
  public static SpyResult<Object> spy(String spyPointName) {
    return new Observation().spy(spyPointName);
  }

  /**
   * Spies this point with no observations.
   *
   * @param spyPointName Name of the point being spied on.
   * @param skipSaveObservation If true, don't actually save the observation;
   *                            all other relevant {@link SpyAgent} actions are still
   *                            performed. This will be overridden by the
   *                            {@code skipSaveObservation} field of a SpyAgent.
   * @return The result of the agent deployed for this point, if any,
   *         else an absent {@link SpyResult}.
   */
  public static SpyResult<Object> spy(String spyPointName, boolean skipSaveObservation) {
    return new Observation().spy(spyPointName, skipSaveObservation);
  }

  /**
   * Spies this point with no observation and no name.
   */
  public static void spy() {
    new Observation().spy();
  }

  /**
   * Spies this point with no observation, expecting a return type of
   * {@code expectedType}. Has no effect and returns an absent {@link SpyResult}
   * if {@link #isActive()} is false.
   *
   * @param spyPointName Name of the point being spied on.
   * @param expectedType Type of value expected to be returned from
   *                     any active {@link SpyAgent}.
   * @param <T> Same as expectedType.
   * @throws IllegalSpyAgentException If the result from the agent matching
   *         this spy point does not match expectedType.
   * @return The result of the agent deployed for this point, if any,
   *         else an absent {@link SpyResult}.
   */
  public static <T> SpyResult<T> spy(String spyPointName, Class<T> expectedType) {
    return new Observation().spy(spyPointName, expectedType);
  }

  /**
   * Spies this point with no observation, expecting a return type of
   * {@code expectedType}. Has no effect and returns an absent {@link SpyResult}
   * if {@link #isActive()} is false.
   *
   * @param spyPointName Name of the point being spied on.
   * @param expectedType Type of value expected to be returned from
   *                     any active {@link SpyAgent}.
   * @param skipSaveObservation If true, don't actually save the observation;
   *                            all other relevant {@link SpyAgent} actions are still
   *                            performed. This will be overridden by the
   *                            {@code skipSaveObservation} field of a SpyAgent.
   * @param <T> Same as expectedType.
   * @throws IllegalSpyAgentException If the result from the agent matching
   *         this spy point does not match expectedType.
   * @return The result of the agent deployed for this point, if any,
   *         else an absent {@link SpyResult}.
   */
  public static <T> SpyResult<T> spy(String spyPointName, Class<T> expectedType, boolean skipSaveObservation) {
    return new Observation().spy(spyPointName, expectedType, skipSaveObservation);
  }



  /**
   * Spies this point with no observation, expecting an Exception to be thrown.
   * Has no effect if {@link #isActive()} is false.
   *
   * @param spyPointName Name of the point being spied on.
   * @throws Exception If an Exception is available on the {@link SpyAgent}
   *         deployed for this point (if any).
   * @throws IllegalSpyAgentException If the SpyAgent available for this
   *         point is not a {@link bond.SpyAgent.SpyAgentWithCheckedException}.
   */
  public static void spyWithException(String spyPointName) throws Exception {
    spyWithException(spyPointName, Exception.class);
  }

  /**
   * Spies this point with no observations, expecting a checked Exception of
   * type {@code expectedException} to be thrown. Has no effect if {@link #isActive()}
   * is false.
   *
   * @param spyPointName Name of the point being spied on.
   * @param expectedException Type of Exception expected to be available on
   *                          any {@link SpyAgent} deployed for this point (if any).
   * @param <E> Same as expectedException.
   * @throws E If an Exception is available on the {@link SpyAgent} deployed for
   *         this point (if any).
   * @throws IllegalSpyAgentException If the type of Exception available on the
   *         {@link SpyAgent} for this point is not compatible with E, or if
   *         the agent is not a {@link bond.SpyAgent.SpyAgentWithCheckedException}.
   */
  public static <E extends Exception> void spyWithException(String spyPointName,
                                                            Class<E> expectedException) throws E {
    new Observation().spyWithException(spyPointName, expectedException);
  }

  /**
   * Begin a new spy with the given (key, value) pair as an observation.
   *
   * @param key Name for this value.
   * @param value Value to be observed.
   * @return An {@link Observation} object which can be used in a builder-style
   *         pattern to construct spy points with more observations.
   */
  public static Observation obs(String key, Object value) {
    return new Observation().obs(key, value);
  }

  /**
   * Used to check if Bond is currently active, which is iff you are currently
   * testing. This can be useful to make some control flow decisions
   *
   * @return True iff Bond is currently active
   */
  public static boolean isActive() {
    return _currentTest.isPresent();
  }

  /**
   * Set the name of the current test. You must already be inside of a test
   * to call this method.
   *
   * The default, if this is not called, is the fully qualified name of the
   * method that is running the current test, e.g. {@code bond.BondTest.testBehavior}.
   *
   * @param name New name to use for the current test.
   */
  public static void setCurrentTestName(String name) {
    if (!isActive()) {
      throw new IllegalStateException("Bond must be active to change the name of the current test!");
    }
    if (name == null) {
      throw new IllegalArgumentException("New test name cannot be null!");
    }
    _currentTest = Optional.of(name);
  }

  /**
   * Used to start Bond into testing mode. This is called internally
   * by {@link BondTestRule}. Should be accompanied by a corresponding
   * {@link #finishTest()}.
   *
   * @param testName Name of the current test.
   */
  static void startTest(String testName) {
    startTest(testName, null);
    _serializer = new Serializer()
                      .withFloatPrecision(DEFAULT_FLOAT_DOUBLE_PRECISION)
                      .withDoublePrecision(DEFAULT_FLOAT_DOUBLE_PRECISION);
  }

  /**
   * Used to start Bond into testing mode. This is called internally
   * by {@link BondTestRule}. Should be accompanied by a corresponding
   * {@link #finishTest()}.
   *
   * @param serializer Custom Serializer to use during this test
   * @param testName Name of the current test.
   */
  static void startTest(String testName, Serializer serializer) {
    clearSettings();
    _observationJsons = new ArrayList<>();
    _agentMap = new HashMap<>();
    _currentTest = Optional.of(testName);
    _serializer = serializer;
  }

  /**
   * Set the directory which will be used to store test observations.
   * This should generally be within the {@code resources} folder of your
   * test folder.
   *
   * @param observationDirectory Test observation storage root directory
   */
  public static void setObservationDirectory(File observationDirectory) {
    _observationDirectory = Optional.fromNullable(observationDirectory);
  }

  /**
   * Set the way in which Bond should handle reconciliation of tests which
   * do not match their reference observations.
   *
   * @param reconcileType Reconciliation method
   */
  public static void setReconciliationMethod(ReconcileType reconcileType) {
    _reconciliationMethod = Optional.fromNullable(reconcileType);
  }

  /**
   * Set a custom serialization method for type.
   * See {@link com.google.gson.GsonBuilder#registerTypeAdapter(Type, Object)}.
   *
   * @param type The type for this adapter to apply to
   * @param typeAdapter The adapter with custom serialization logic
   */
  public static void registerTypeAdapter(Type type, Object typeAdapter) {
    _serializer = _serializer.withTypeAdapter(type, typeAdapter);
  }

  /**
   * Set a custom serialization method for baseType and its subtypes.
   * See {@link com.google.gson.GsonBuilder#registerTypeHierarchyAdapter(Class, Object)}.
   *
   * @param baseType The baseType for this adapter to apply to
   * @param typeAdapter The adapter with custom serialization logic
   */
  public static void registerTypeHierarchyAdapter(Class<?> baseType, Object typeAdapter) {
    _serializer = _serializer.withTypeHierarchyAdapter(baseType, typeAdapter);
  }

  /**
   * Specify that for objects of type clazz, simply call toString() on the object
   * to serialize it instead of doing a full JSON serialization.
   *
   * @param clazz Type for this to apply to
   * @param <T> Same as clazz
   */
  public <T> void setToStringSerialization(Class<T> clazz) {
    _serializer = _serializer.withToStringSerialization(clazz);
  }

  /**
   * Set the amount of precision to be used when serializing floats and doubles.
   *
   * @param places Number of places after the decimal to keep in serialized form
   */
  public static void setFloatDoublePrecision(final int places) {
    _serializer = _serializer.withFloatPrecision(places).withDoublePrecision(places);
  }

  /**
   * Set the amount of precision to be used when serializing doubles.
   *
   * @param places Number of places after the decimal to keep in serialized form
   */
  public static void setDoublePrecision(final int places) {
    _serializer = _serializer.withDoublePrecision(places);
  }

  /**
   * Set the amount of precision to be used when serializing floats.
   *
   * @param places Number of places after the decimal to keep in serialized form
   */
  public static void setFloatPrecision(final int places) {
    _serializer = _serializer.withFloatPrecision(places);
  }

  /**
   * Deploy a {@link SpyAgent} to the given spy point. Whenever {@link #spy} is
   * called with the same spy point name, this agent will be checked to determine
   * if it is applicable (based off of its filters), and if so, the agent's
   * {@link Doer doers}, results, and exceptions will be used. Deploying
   * a new agents to a spy point will have the effect of overriding any agents
   * which have been previously deployed to that spy point. If the most recent
   * agent is not applicable to the spy point (due to filters), the next most recent
   * agent is checked for applicability, and so on until an applicable agent is found
   * (or none is found).
   *
   * @see SpyAgent
   * @param spyPointName Name of the spy point to deploy the agent to
   * @param agent Agent which should be deployed
   */
  public static void deployAgent(String spyPointName, SpyAgent agent) {
    if (!isActive()) {
      throw new IllegalStateException("Cannot deploy an agent when not in a test!");
    }
    if (!_agentMap.containsKey(spyPointName)) {
      _agentMap.put(spyPointName, new ArrayList<SpyAgent>());
    }
    _agentMap.get(spyPointName).add(0, agent);
  }

  /**
   * Finish the current test, noting that the test failed because of e.
   *
   * @param e The issue which caused the test to fail.
   * @throws IOException If writing out observations fails.
   */
  static void finishTest(Throwable e) throws IOException {
    Writer stackTraceWriter = new StringWriter();
    PrintWriter stackTracePrintWriter = new PrintWriter(stackTraceWriter);
    e.printStackTrace(stackTracePrintWriter);
    finishTest(String.format("Test had failure(s): %s\n%s", e, stackTraceWriter.toString()));
  }

  /**
   * Finish the current test, noting that no errors occurred during the test's execution.
   *
   * @return True iff the reconciliation process was successful
   * @throws IOException If writing out test observations fails.
   */
  static boolean finishTest() throws IOException {
    return finishTest((String) null);
  }

  /**
   * Finish the current test.
   *
   * @param testFailureMessage Should be null if the test completed successfully.
   *                           Otherwise, should be a string describing what caused the
   *                           test to fail.
   * @return True iff the reconciliation process was successful
   * @throws IOException If writing out test observations fails.
   */
  private static boolean finishTest(String testFailureMessage) throws IOException {
    if (!isActive()) {
      throw new IllegalStateException("Cannot call finishTest when not in a test!");
    }

    File outFileBase = getObservationDirectory();
    for (String pathComponent : CLASS_SPLITTER.split(getFileSafeTestName())) {
      outFileBase = new File(outFileBase, pathComponent);
    }
    Files.createParentDirs(outFileBase);
    File referenceFile = new File(outFileBase.getCanonicalPath() + ".json");

    boolean reconcileResult = reconcileObservations(referenceFile, testFailureMessage);

    _currentTest = Optional.absent();
    return reconcileResult;
  }

  /**
   * Get the currently applicable serializer.
   *
   * @return The Serializer which should be used currently.
   */
  static Serializer getSerializer() {
    return _serializer;
  }

  /**
   * Get the current root directory for test observations.
   *
   * @return Test observation root directory
   */
  private static File getObservationDirectory() {
    if (_observationDirectory.isPresent()) {
      return _observationDirectory.get();
    } else {
      String bondObsDirString = System.getenv("BOND_OBSERVATION_DIR");
      if (bondObsDirString == null) {
        throw new IllegalStateException("Must specify a directory to store Bond's observations! This can be " +
                                            "done using BondTestRule.withObservationDirectory() from your test, " +
                                            "or by setting the BOND_OBSERVATION_DIR environment variable.");
      }
      return new File(bondObsDirString);
    }
  }

  /**
   * Get the current reconciliation method.
   *
   * @return Current reconciliation method
   */
  private static ReconcileType getReconciliationMethod() {
    if (_reconciliationMethod.isPresent()) {
      return _reconciliationMethod.get();
    } else {
      String reconciler = System.getenv("BOND_RECONCILE");
      if (reconciler == null || reconciler.equals("")) {
        return ReconcileType.CONSOLE;
      }
      return ReconcileType.getFromName(reconciler);
    }
  }

  /**
   * Clear the settings which have currently been applied to Bond.
   */
  private static void clearSettings() {
    _observationDirectory = Optional.absent();
    _reconciliationMethod = Optional.absent();
  }


  /**
   * Reconcile the current set of observations against the reference file for this test.
   *
   * @param referenceFile File storing the reference observations to compare against
   * @param testFailureMessage Should be null if the test completed successfully, else
   *                           a message describing what caused it to fail.
   * @return True iff the reconciliation process was successful
   * @throws IOException If a failure occurs while writing observations to a file
   */
  private static boolean reconcileObservations(File referenceFile, String testFailureMessage)
      throws IOException {

    Reconciler reconciler = Reconciler.getReconciler(getReconciliationMethod());
    return reconciler.reconcile(_currentTest.get(), referenceFile, getObservationsAsLines(),
        testFailureMessage);
  }

  /**
   * Returns the filename-safe version of the name of the current test, replacing
   * all non-alphanumeric / period characters with underscores.
   *
   * @return Filename-safe version of the name of the current test
   */
  static String getFileSafeTestName() {
    return _currentTest.get().replaceAll("[^A-z0-9.]", "_");
  }

  /**
   * Get all of the observations currently stored as a List of individual lines.
   *
   * @return List of the current observations broken into individual lines
   */
  private static List<String> getObservationsAsLines() {
    List<String> lines = new ArrayList<>();
    lines.add("[");
    lines.addAll(LINE_SPLITTER.splitToList(Joiner.on(",\n").join(_observationJsons)));
    lines.add("]");
    return lines;
  }

  /**
   * Add an observation to the current set of observations.
   *
   * @param observationJson A JSON string representing the observation.
   */
  static void addObservation(String observationJson) {
    _observationJsons.add(observationJson);
  }

  /**
   * Return the agent which is currently applicable to the given spy point with the given
   * observations, else null.
   *
   * @param spyPointName Spy point name for which to search for agents
   * @param observationMap Observation supplied to the current spy point
   * @return The applicable agent, else null
   */
  static SpyAgent getAgent(String spyPointName, Map<String, Object> observationMap) {
    if (spyPointName == null) {
      return null;
    }
    List<SpyAgent> agents = _agentMap.get(spyPointName);
    if (agents == null) {
      return null;
    }
    for (SpyAgent agent : agents) {
      if (agent.accept(observationMap)) {
        return agent;
      }
    }
    return null;
  }
}
