package bond;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Bond {

  private static final Splitter CLASS_SPLITTER = Splitter.on(".").trimResults();
  private static final Joiner OBSERVATION_JOINER = Joiner.on("\n");

  private static Optional<String> _currentTest = Optional.absent();

  private static AgentMap<SpyAgent<?>> _allSpyAgents = new AgentMap<>();
  private static AgentMap<SpyAgent<Object>> _objectSpyAgents = new AgentMap<>();
  private static AgentMap<SpyAgent<Integer>> _integerSpyAgents = new AgentMap<>();
  private static AgentMap<SpyAgent<String>> _stringSpyAgents = new AgentMap<>();

  private static List<String> _observationJsons = new ArrayList<>();

  private static Optional<File> _observationDirectory = Optional.absent();
  private static Optional<Reconcile> _reconciliationMethod = Optional.absent();

  /**
   * Private constructor to enforce singleton-style pattern
   */
  private Bond() {}

  public static boolean isActive() {
    return _currentTest.isPresent();
  }

  public static void startTest(String testName) {
    _currentTest = Optional.of(testName);
  }

  public static void setObservationDirectory(File observationDirectory) {
    _observationDirectory = Optional.fromNullable(observationDirectory);
  }

  public static void setReconciliationMethod(Reconcile reconcile) {
    _reconciliationMethod = Optional.fromNullable(reconcile);
  }

  public static void finishTest(Throwable e) throws IOException {
    finishTest("Test had failure(s)!");
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
    // TODO ETK should be configurable in case of not using git
    if (!outFileBase.exists()) {
      File gitignoreFile = new File(outFileBase, ".gitignore");
      Files.createParentDirs(gitignoreFile);
      Files.write("*_now.json\n*.diff\n", gitignoreFile, Charsets.UTF_8);
    }
    for (String pathComponent : CLASS_SPLITTER.split(_currentTest.get())) {
      outFileBase = new File(outFileBase, pathComponent);
    }
    Files.createParentDirs(outFileBase);
    File referenceFile = new File(outFileBase, ".json");
    File currentFile = new File(outFileBase, "_now.json");

    if (currentFile.exists()) {
      currentFile.delete();
    }
    saveObservations(currentFile);

    boolean reconcileResult = reconcileObservations(referenceFile, currentFile, testFailureMessage);

    _currentTest = Optional.absent();
    return reconcileResult;
  }

  private static void saveObservations(File outFile) throws IOException {
    Files.write(OBSERVATION_JOINER.join(_observationJsons), outFile, Charsets.UTF_8);
  }

  // return true if reconcile succeeded
  private static boolean reconcileObservations(File referenceFile, File currentFile,
                                               String testFailureMessage) throws IOException {
    // TODO well this clearly needs work... Same problem as getting the observation directory!
    File reconcileScript = new File("/home/erik/dev/research/bond/bond/pybond/bond/bond_reconcile.py");

    List<String> reconcileCommand = Lists.newArrayList(reconcileScript.getCanonicalPath(),
        "--reference", referenceFile.getCanonicalPath(),
        "--current", currentFile.getCanonicalPath(),
        "--test", _currentTest.get());
    if (_reconciliationMethod.isPresent()) {
      reconcileCommand.add("--reconcile");
      reconcileCommand.add(_reconciliationMethod.get().getName());
    }
    if (testFailureMessage != null) {
      reconcileCommand.add("--no-save");
      reconcileCommand.add(testFailureMessage);
    }
    // TODO I think this doesn't require shell escaping but we should check
    Process reconcileProcess = new ProcessBuilder(reconcileCommand).start();
    return reconcileProcess.exitValue() == 0;
  }

  public static void obs(String key, Object value) {

  }

  public static void spy() {

  }

  static void addObservation(String observationJson) {
    _observationJsons.add(observationJson);
  }

  static void deployAgent(String spyPointName, SpyAgent<?> agent) {
    // TODO check this should theoretically be in all of the methods
    if (!isActive()) {
      throw new IllegalStateException("Must be in a test to call deployAgent!");
    }
    _allSpyAgents.getAgents(spyPointName).add(0, agent);
  }

  // TODO doing something like this could reduce the necessity for agents to have types
  // (at least in the user's eyes) and get rid of the ugly deploy{Object,Integer,...}Agent
  // syntax. But... that would still be necessary when using a Resulter
  static <T> void deployAgentWithResult(String spyPointName, SpyAgent agent, T result) {}
  static <T> void deployAgentWithTResulter(String spyPointName, SpyAgent agent, Resulter<T> result) {}
  //static <T> void deployAgent(String spyPointName, SpyAgentWithResult<T> agent)

  // TODO should these actually add their agent to genericAgents?
  // two ways of thinking about it:
  //   - for spy points that don't need a return, you can just pick any
  //     agent that matches, so add them all
  //   - the category of 'no return' spy points is no different from the
  //     category of any other return type spy point, so it shouldn't grab the others
  static void deployObjectAgent(String spyPointName, SpyAgent<Object> agent) {
    deployAgent(spyPointName, agent);
    _objectSpyAgents.getAgents(spyPointName).add(0, agent);
  }
  
  static void deployIntegerAgent(String spyPointName, SpyAgent<Integer> agent) {
    deployAgent(spyPointName, agent);
    _integerSpyAgents.getAgents(spyPointName).add(0, agent);
  }
  
  static void deployStringAgent(String spyPointName, SpyAgent<String> agent) {
    deployAgent(spyPointName, agent);
    _stringSpyAgents.getAgents(spyPointName).add(0, agent);
  }

  static List<SpyAgent<?>> getGenericSpyAgents(String spyPointName) {
    return _allSpyAgents.getAgents(spyPointName);
  }

  static List<SpyAgent<Object>> getObjectSpyAgents(String spyPointName) {
    return _objectSpyAgents.getAgents(spyPointName);
  }

  static List<SpyAgent<Integer>> getIntegerSpyAgents(String spyPointName) {
    return _integerSpyAgents.getAgents(spyPointName);
  }

  static List<SpyAgent<String>> getStringSpyAgents(String spyPointName) {
    return _stringSpyAgents.getAgents(spyPointName);
  }

  private static class AgentMap<T> extends HashMap<String, List<T>> {
    public List<T> getAgents(String key) {
      if (key == null) {
        return new ArrayList<>();
      } else if (containsKey(key)) {
        return get(key);
      } else {
        ArrayList<T> newList = new ArrayList<>();
        put(key, newList);
        return newList;
      }
    }
  }

}
