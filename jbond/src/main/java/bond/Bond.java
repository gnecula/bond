package bond;

import bond.reconcile.ReconcileType;
import bond.reconcile.Reconciler;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bond {

  private static final Splitter CLASS_SPLITTER = Splitter.on(".").trimResults();
  private static final Splitter LINE_SPLITTER = Splitter.on("\n");

  private static Optional<String> _currentTest = Optional.absent();
  private static Map<String, List<SpyAgent>> _agentMap = new HashMap<>();
  private static List<String> _observationJsons = new ArrayList<>();
  private static Optional<File> _observationDirectory = Optional.absent();
  private static Optional<ReconcileType> _reconciliationMethod = Optional.absent();

  /**
   * Private constructor to enforce singleton-style pattern
   */
  private Bond() {}

  public static Optional<Object> spy(String spyPointName) {
    return new Observation().spy(spyPointName);
  }

  public static void spy() {
    new Observation().spy();
  }

  public static <T> Optional<T> spy(String spyPointName, Class<T> expectedType) {
    return new Observation().spy(spyPointName, expectedType);
  }

  public void spyWithException(String spyPointName) throws Exception {
    spyWithException(spyPointName, Exception.class);
  }

  public <E extends Exception> void spyWithException(String spyPointName,
                                                     Class<E> expectedException) throws E {
    new Observation().spyWithException(spyPointName, expectedException);
  }

  public static Observation obs(String key, Object value) {
    return new Observation().obs(key, value);
  }

  public static boolean isActive() {
    return _currentTest.isPresent();
  }

  public static void startTest(String testName) {
    clearSettings();
    _observationJsons = new ArrayList<>();
    _agentMap = new HashMap<>();
    _currentTest = Optional.of(testName);
  }

  public static void setObservationDirectory(File observationDirectory) {
    _observationDirectory = Optional.fromNullable(observationDirectory);
  }

  public static void setReconciliationMethod(ReconcileType reconcileType) {
    _reconciliationMethod = Optional.fromNullable(reconcileType);
  }

  private static File getObservationDirectory() {
    if (_observationDirectory.isPresent()) {
      return _observationDirectory.get();
    } else {
      String bondObsDirString = System.getenv("BOND_OBS_DIR");
      if (bondObsDirString == null) {
        throw new IllegalStateException("Must specify a directory to store Bond's observations! This can be " +
                                            "done using BondTestRule.withObservationDirectory() from your test, " +
                                            "or by setting the BOND_OBS_DIR environment variable.");
      }
      return new File(bondObsDirString);
    }
  }

  private static ReconcileType getReconciliationMethod() {
    if (_reconciliationMethod.isPresent()) {
      return _reconciliationMethod.get();
    } else {
      String reconciler = System.getenv("BOND_RECONCILE");
      if (reconciler == null || reconciler.equals("")) {
        return ReconcileType.ABORT;
      }
      return ReconcileType.getFromName(reconciler);
    }
  }

  private static void clearSettings() {
    _observationDirectory = Optional.absent();
    _reconciliationMethod = Optional.absent();
  }

  public static void deployAgent(String spyPointName, SpyAgent agent) {
    if (!isActive()) {
      throw new IllegalStateException("Cannot deploy an agent when not in a test!");
    }
    if (!_agentMap.containsKey(spyPointName)) {
      _agentMap.put(spyPointName, new ArrayList<SpyAgent>());
    }
    _agentMap.get(spyPointName).add(0, agent);
  }

  public static void finishTest(Throwable e) throws IOException {
    finishTest("Test had failure(s): " + e);
  }

  public static boolean finishTest() throws IOException {
    return finishTest((String) null);
  }

  // returns true if the test should fail
  private static boolean finishTest(String testFailureMessage) throws IOException {
    if (!isActive()) {
      throw new IllegalStateException("Cannot call finishTest when not in a test!");
    }

    File outFileBase = getObservationDirectory();
    for (String pathComponent : CLASS_SPLITTER.split(_currentTest.get())) {
      outFileBase = new File(outFileBase, pathComponent);
    }
    Files.createParentDirs(outFileBase);
    File referenceFile = new File(outFileBase.getCanonicalPath() + ".json");

    boolean reconcileResult = reconcileObservations(referenceFile, testFailureMessage);

    _currentTest = Optional.absent();
    return reconcileResult;
  }

  // return true if reconcile succeeded
  private static boolean reconcileObservations(File referenceFile, String testFailureMessage)
      throws IOException {

    Reconciler reconciler = Reconciler.getReconciler(getReconciliationMethod());
    return reconciler.reconcile(_currentTest.get(), referenceFile, getObservationsAsLines(),
        testFailureMessage);
  }

  private static List<String> getObservationsAsLines() {
    List<String> lines = new ArrayList<>();
    for (String obs : _observationJsons) {
      lines.addAll(LINE_SPLITTER.splitToList(obs));
    }
    return lines;
  }

  static void addObservation(String observationJson) {
    _observationJsons.add(observationJson);
  }

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
