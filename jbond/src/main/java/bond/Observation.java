package bond;

import com.google.common.base.Optional;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.*;

public class Observation {



  // Sorted map, forcing __spyPoint__ to appear as the first entry (if present)
  SortedMap<String, Object> _observationMap = new TreeMap<>(new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
      if (s1.equals("__spyPoint__")) {
        return -1;
      } else if (s2.equals("__spyPoint__")) {
        return 1;
      } else {
        return s1.compareTo(s2);
      }
    }
  });

  public Observation() {
    // TODO anything to be done here?
  }

  public Observation obs(String key, Object value) {
    _observationMap.put(key, value);
    return this;
  }

  public void spy() {
    spy(null);
  }

  public <T> Optional<T> spy(String spyPointName) {
    //if (!Bond.isActive()) {
    //  return Optional.absent();
    //}
    SpyAgent<T> agent = Bond.getAgent(spyPointName, _observationMap);
    processObservation(spyPointName);
    if (agent != null) {
      return agent.performDoersGetReturn(_observationMap);
    } else {
      return Optional.absent();
    }
  }

  public <T, E extends Exception> Optional<T> spyWithException(String spyPointName) throws E {
    if (!Bond.isActive()) {
      return Optional.absent();
    }
    SpyAgent.SpyAgentWithCheckedException<T, E> agent;
    try {
      agent = (SpyAgent.SpyAgentWithCheckedException<T, E>) Bond.getAgent(spyPointName, _observationMap);
    } catch (ClassCastException e) {
      throw new IllegalSpyAgentException("Requested a return value / exception type for " + spyPointName +
                                             " which is not compatible with the agent deployed!");
    }
    processObservation(spyPointName);
    if (agent != null) {
      agent.throwCheckedException(_observationMap);
      return agent.performDoersGetReturn(_observationMap);
    } else {
      return Optional.absent();
    }
  }

  private void processObservation(String spyPointName) {
    if (spyPointName != null) {
      _observationMap.put("__spyPoint__", spyPointName);
    }

    Bond.addObservation(Serializer.serialize(_observationMap));
  }

}
