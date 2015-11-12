package bond;

import java.util.Map;

/**
 * Interface used to specify an object that will return a checked exception.
 * Used to dynamically decide what to throw from a {@link SpyAgent}.
 *
 * @see SpyAgent
 */
public interface CheckedExcepter {

  /**
   * Method which is called to get the exception.
   *
   * @param map A map of all of the (key, value) pairs which
   *            comprise the current observation.
   * @return The checked exception which should be thrown by the {@link SpyAgent}
   */
  Exception accept(Map<String, Object> map);

}
