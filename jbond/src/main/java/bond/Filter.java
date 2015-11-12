package bond;

import java.util.Map;

/**
 * Interface used to specify a custom filtering function to decide what observations
 * a {@link SpyAgent} is applicable to.
 *
 * @see SpyAgent
 */
public interface Filter {

  /**
   * Method which is called to decide whether or not the current observation
   * satisfies this filter.
   *
   * @param map A map of all of the (key, value) pairs which
   *            comprise the current observation.
   * @return True iff {@code map} satisfies this filter.
   */
  boolean accept(Map<String, Object> map);

}
