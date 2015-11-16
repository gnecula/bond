package io.github.necula01.bond;

import java.util.Map;

/**
 * Interface used to specify an object that will return an exception. Used to
 * dynamically decide what to throw from a {@link SpyAgent}.
 *
 * @see SpyAgent
 */
public interface Excepter extends CheckedExcepter {

  /**
   * Method which is called to get the exception.
   *
   * @param map A map of all of the (key, value) pairs which
   *            comprise the current observation.
   * @return The exception which should be thrown by the {@link SpyAgent}
   */
  RuntimeException accept(Map<String, Object> map);

}
