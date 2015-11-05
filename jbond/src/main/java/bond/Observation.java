package bond;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;

public class Observation {

  SortedMap<String, Object> observationMap = new TreeMap<>();

  public Observation() {
    // TODO anything to be done here?
  }

  public Observation obs(String name, Object value) {
    observationMap.put(name, value);
    return this;
  }

  // TODO what does this return, exactly?
  public Object spy() {
    return null; // spy(null);
  }

  public void spy(String spyPointName) {
    SpyAgent<?> agent = null;
    if (spyPointName != null) {
      observationMap.put("__spyPoint__", spyPointName);
      agent = getGenericSpyAgent(spyPointName);
    }
    processObservation();
    if (agent != null) {
      agent.performDoersGetReturn(observationMap);
    }
  }

  public Optional<String> spyWithString() {
    return spyWithString(null);
  }

  public Optional<String> spyWithString(String spyPointName) {
    return spyWith(spyPointName, Bond.getStringSpyAgents(spyPointName));
  }

  public Optional<Integer> spyWithInteger() {
    return spyWithInteger(null);
  }

  public Optional<Integer> spyWithInteger(String spyPointName) {
    return spyWith(spyPointName, Bond.getIntegerSpyAgents(spyPointName));
  }

  public Optional<Object> spyWithObject() {
    return spyWithObject(null);
  }

  public Optional<Object> spyWithObject(String spyPointName) {
    return spyWith(spyPointName, Bond.getObjectSpyAgents(spyPointName));
  }

  private <T> Optional<T> spyWith(String spyPointName, List<SpyAgent<T>> agents) {
    SpyAgent<T> agent = null;
    if (spyPointName != null) {
      observationMap.put("__spyPoint__", spyPointName);
      agent = getSpyAgent(agents);
    }
    processObservation();
    if (agent == null) {
      return Optional.absent();
    } else {
      return agent.performDoersGetReturn(observationMap);
    }
  }

  private SpyAgent<?> getGenericSpyAgent(String spyPointName) {
    List<SpyAgent<?>> agents = Bond.getGenericSpyAgents(spyPointName);
    for (SpyAgent<?> agent : agents) {
      if (agent.accept(observationMap)) {
        return agent;
      }
    }
    return null;
  }

  private <T> SpyAgent<T> getSpyAgent(List<SpyAgent<T>> agents) {
    for (SpyAgent<T> agent : agents) {
      if (agent.accept(observationMap)) {
        return agent;
      }
    }
    return null;
  }

  private void processObservation() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    // TODO sorting? Maybe using toJsonTree somehow?
    Bond.addObservation(gson.toJson(observationMap));
  }

}
