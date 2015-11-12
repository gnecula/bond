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

  public Optional<Object> spy(String spyPointName) {
    if (!Bond.isActive()) {
      return Optional.absent();
    }
    SpyAgent agent = Bond.getAgent(spyPointName, _observationMap);
    processObservation(spyPointName);
    if (agent != null) {
      return agent.performDoersGetReturn(_observationMap);
    } else {
      return Optional.absent();
    }
  }

  public <T> Optional<T> spy(String spyPointName, Class<T> expectedType) {
    if (!Bond.isActive()) {
      return Optional.absent();
    }
    Optional<T> ret = (Optional<T>) spy(spyPointName);
    if (ret.isPresent()) {
      if (!expectedType.isAssignableFrom(ret.get().getClass())) {
        throw new IllegalSpyAgentException("Requested a return value for " + spyPointName +
            " which is not compatible with the return type of the agent deployed!");
      }
      return ret;
    } else {
      return Optional.absent();
    }
  }

  public void spyWithException(String spyPointName) throws Exception {
    spyWithException(spyPointName, Exception.class);
  }

  public <E extends Exception> void spyWithException(String spyPointName,
                                                     Class<E> expectedException) throws E {
    if (!Bond.isActive()) {
      return;
    }
    try {
      SpyAgent.SpyAgentWithCheckedException agent =
          (SpyAgent.SpyAgentWithCheckedException) Bond.getAgent(spyPointName, _observationMap);
      processObservation(spyPointName);
      if (agent != null) {
        E e = (E) agent.getCheckedException(_observationMap);
        if (e != null && !expectedException.isAssignableFrom(e.getClass())) {
          throw new ClassCastException("jump to catch clause");
        }
        if (e != null) {
          throw e;
        }
      }
    } catch (ClassCastException e) {
      throw new IllegalSpyAgentException("Requested a return value / exception type for " + spyPointName +
                                             " which is not compatible with the agent deployed!");
    }
  }

  private void processObservation(String spyPointName) {
    if (spyPointName != null) {
      _observationMap.put("__spyPoint__", spyPointName);
    }

    Bond.addObservation(Serializer.serialize(_observationMap));
  }

}
