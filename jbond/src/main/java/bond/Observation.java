package bond;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;

public class Observation {

  SortedMap<String, Object> _observationMap = new TreeMap<>();

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
    SpyAgent<T> agent = Bond.getAgent(spyPointName, _observationMap);
    processObservation(spyPointName);
    if (agent != null) {
      return agent.performDoersGetReturn(_observationMap);
    } else {
      return Optional.absent();
    }
  }

  private void processObservation(String spyPointName) {
    if (spyPointName != null) {
      _observationMap.put("__spyPoint__", spyPointName);
    }
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    // TODO sorting? Maybe using toJsonTree somehow?
    Bond.addObservation(gson.toJson(_observationMap));
  }

  class ObservationWithException<E extends Exception> {

    private Observation _parentObservation;

    ObservationWithException(Observation parentObservation) {
      _parentObservation = parentObservation;
    }

    public <T> Optional<T> spy(String spyPointName) throws E {
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

  }

}
