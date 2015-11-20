package bond;

import com.google.common.base.Optional;

import java.util.*;

/**
 * Class used to represent an observation. An observation is built up as a collection
 * of (key, value) pairs using a builder-style pattern. For more information, see the
 * class description for {@link Bond}.
 */
public class Observation {

  /**
   * Package-private to make sure this only gets instantiated through
   * {@link Bond#obs(String, Object)}
   */
  Observation() {

  }

  // Sorted map, forcing __spyPoint__ to appear as the first entry (if present)
  private SortedMap<String, Object> _observationMap = new TreeMap<>(new Comparator<String>() {
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

  /**
   * Add a (key, value) pair to the current observation.
   *
   * @see Bond#obs(String, Object)
   * @param key Name of the value being observed
   * @param value Value to be observed
   * @return This object to continue to be used in a builder-style pattern
   */
  public Observation obs(String key, Object value) {
    _observationMap.put(key, value);
    return this;
  }

  /**
   * Spies with the currently built observation and an anonymous spy point.
   *
   * @see Bond#spy()
   */
  public void spy() {
    spy(null);
  }

  /**
   * Spies with the currently built observation.
   *
   * @see Bond#spy(String)
   * @param spyPointName Name of the point being spied on.
   * @return The result of the agent deployed for this point, if any,
   *         else {@link Optional#absent()}. Note that if an agent returns
   *         null, this will return {@link Optional#absent()}.
   */
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

  /**
   * Spies this point with the current observation, expecting a return type of
   * {@code expectedType}. Has no effect and returns {@link Optional#absent()}
   * if {@link Bond#isActive()} is false.
   *
   * @see Bond#spy(String, Class)
   * @param spyPointName Name of the point being spied on
   * @param expectedType Type of value expected to be returned from any active {@link SpyAgent}
   * @param <T> Same as expectedType
   * @throws IllegalSpyAgentException If the result from the agent matching
   *         this spy point does not match expectedType.
   * @return The result of the agent deployed for this point, if any,
   *         else {@link Optional#absent()}. Note that if an agent returns null,
   *         this will return {@link Optional#absent()}.
   */
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

  /**
   * Spies this point with no observation, expecting an Exception to be thrown.
   * Has no effect if {@link Bond#isActive()} is false.
   *
   * @see Bond#spyWithException(String)
   * @param spyPointName Name of the point being spied on.
   * @throws Exception If an Exception is available on the {@link SpyAgent}
   *         deployed for this point (if any).
   * @throws IllegalSpyAgentException If the SpyAgent available for this
   *         point is not a {@link bond.SpyAgent.SpyAgentWithCheckedException}.
   */
  public void spyWithException(String spyPointName) throws Exception {
    spyWithException(spyPointName, Exception.class);
  }

  /**
   * Spies this point with no observations, expecting a checked Exception of
   * type {@code expectedException} to be thrown. Has no effect if {@link Bond#isActive()}
   * is false.
   *
   * @see Bond#spyWithException(String, Class)
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

  /**
   * Process the current observation, JSON-serializing it and adding it
   * to Bond's currently stored observations.
   *
   * @param spyPointName Name of this spy point
   */
  private void processObservation(String spyPointName) {
    if (spyPointName != null) {
      _observationMap.put("__spyPoint__", spyPointName);
    }

    Bond.addObservation(Bond.getSerializer().serialize(_observationMap));
  }

}
