package bond;

import java.util.Map;

/**
 * Interface used to specify an object that will have some side effect. Used to
 * allow a {@link SpyAgent} to take actions based on the current observation.
 *
 * @see SpyAgent
 */
public interface Doer {

  /**
   * Method to do something.
   *
   * @param map A map of all of the (key, value) pairs which
   *            comprise the current observation.
   */
  void accept(Map<String, Object> map);

}
