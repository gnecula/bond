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
  private static final Splitter LINE_SPLITTER = Splitter.on("\n").trimResults();

  private static Optional<String> _currentTest = Optional.absent();

  private static Map<String, List<SpyAgent<?>>> _agentMap = new HashMap<>();

  private static List<String> _observationJsons = new ArrayList<>();

  private static Optional<File> _observationDirectory = Optional.absent();
  private static Optional<ReconcileType> _reconciliationMethod = Optional.absent();

  /**
   * Private constructor to enforce singleton-style pattern
   */
  private Bond() {}

  public static <T> Optional<T> spy(String spyPointName) {
    return new Observation().spy(spyPointName);
  }

  public static void spy() {
    new Observation().spy();
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

  private static void clearSettings() {
    _observationDirectory = Optional.absent();
    _reconciliationMethod = Optional.absent();
  }

  public static void deployAgent(String spyPointName, SpyAgent<?> agent) {
    if (!_agentMap.containsKey(spyPointName)) {
      _agentMap.put(spyPointName, new ArrayList<SpyAgent<?>>());
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
    // TODO okay for now but if this stays this way (with no good default) then
    // we should make it a required constructor parameter on BondTestRule
    if (!_observationDirectory.isPresent()) {
      throw new IllegalStateException("Bond's test observation directory must be set!");
    }
    if (!isActive()) {
      throw new IllegalStateException("Cannot call finishTest when not in a test!");
    }

    File outFileBase = _observationDirectory.get();
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

    Reconciler reconciler;
    if (_reconciliationMethod.isPresent()) {
      reconciler = Reconciler.getReconciler(_reconciliationMethod.get());
    } else {
      reconciler = Reconciler.getReconciler();
    }
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

  static <T> SpyAgent<T> getAgent(String spyPointName, Map<String, Object> observationMap) {
    if (spyPointName == null) {
      return null;
    }
    List<SpyAgent<?>> agents = _agentMap.get(spyPointName);
    if (agents == null) {
      return null;
    }
    try {
      for (SpyAgent<?> agent : agents) {
        if (agent.accept(observationMap)) {
          return (SpyAgent<T>) agent;
        }
      }
      return null;
    } catch (ClassCastException e) {
      throw new IllegalSpyAgentException("Requested a return value for " + spyPointName +
          " which is not compatible with the return type of the agent deployed!");
    }
  }
}
