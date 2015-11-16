package io.github.necula01.bond;

import java.util.Map;

/**
 * Interface used to specify an object that will return a result. Used to
 * dynamically decide what to return from a {@link SpyAgent}.
 *
 * @see SpyAgent
 */
public interface Resulter {

  /**
   * Method which is called to get the result.
   *
   * @param map A map of all of the (key, value) pairs which
   *            comprise the current observation.
   * @return The object which should be returned by the {@link SpyAgent}
   */
  Object accept(Map<String, Object> map);

}
