package io.github.necula01.bond;

import com.google.common.base.Optional;

import java.util.*;

/**
 * <p>Class representing an agent which can modify the behavior of spy points.</p>
 *
 * <p>A {@code SpyAgent} should be deployed to a specific spy point using
 * {@link Bond#deployAgent(String, SpyAgent)}. Then, the agent will have the opportunity
 * to examine the observation associated with the spy point, decide if it is applicable
 * or not (based on filters), and if it is, what action to take (doers, results, exceptions).
 * You can specify the behavior of a {@code SpyAgent} using a builder pattern:</p>
 *
 * <pre><code>
 *   SpyAgent agent = new SpyAgent()
 *                      .withFilterKeyContains("key", "substring")
 *                      .withFilterKeyContains("key2", "substring")
 *                      .withResult("agent result");
 *   Bond.deployAgent("mySpyPoint", agent);
 *
 *   // elsewhere ...
 *
 *   Optional&lt;String&gt; str =
 *     Bond.obs("key", "substring").obs("key2", "substring").spy("mySpyPoint", String.class);
 *   // str will contain "agent result"
 *
 *   Optional&lt;String&gt; str2 =
 *     Bond.obs("key", "substring").obs("key2", "foo").spy("mySpyPoint", String.class);
 *   // str will not contain any value
 * </code></pre>
 *
 * <p>The above snippet will create a {@code SpyAgent} which only applies to certain observations.
 * Note that <b>all</b> filter criteria must be met for a {@code SpyAgent} to apply to a given spy
 * point.</p>
 *
 * @see Bond#deployAgent(String, SpyAgent)
 */
public class SpyAgent {

  RuntimeException _exception;
  Excepter _excepter;
  boolean _hasResult = false;
  Object _result;
  Resulter _resulter;
  List<Doer> _doers = new ArrayList<>();
  List<Filter> _filters = new ArrayList<>();

  /**
   * Limit the applicability of this agent to spy points whose observations include a key
   * matching {@code key} whose value, when converted to a String via {@code toString}, equals
   * {@code value}.
   *
   * @param key Key to look for in the observation
   * @param value Value to match against
   * @return This {@code SpyAgent} to facilitate the builder pattern
   */
  public SpyAgent withFilterKeyEq(final String key, final String value) {
    _filters.add(new Filter() {
      @Override
      public boolean accept(Map<String, Object> map) {
        return map.containsKey(key) && map.get(key).toString().equals(value);
      }
    });
    return this;
  }

  /**
   * Limit the applicability of this agent to spy points whose observations include a key
   * matching {@code key} whose value, when converted to a String via JSON serialization,
   * contains {@code substring}. Note that this is the only FilterKey which uses JSON serialization
   * instead of {@code toString}.
   *
   * @param key Key to look for in the observation
   * @param substring Substring to search for
   * @return This {@code SpyAgent} to facilitate the builder pattern
   */
  public SpyAgent withFilterKeyContains(final String key, final String substring) {
    _filters.add(new Filter() {
      @Override
      public boolean accept(Map<String, Object> map) {
        return map.containsKey(key) && Bond.getSerializer().serialize(map.get(key)).contains(substring);
      }
    });
    return this;
  }

  /**
   * Limit the applicability of this agent to spy points whose observations include a key
   * matching {@code key} whose value, when converted to a String via {@code toString},
   * starts with {@code prefix}.
   *
   * @param key Key to look for in the observation
   * @param prefix Prefix to search for
   * @return This {@code SpyAgent} to facilitate the builder pattern
   */
  public SpyAgent withFilterKeyStartsWith(final String key, final String prefix) {
    _filters.add(new Filter() {
      @Override
      public boolean accept(Map<String, Object> map) {
        return map.containsKey(key) && map.get(key).toString().startsWith(prefix);
      }
    });
    return this;
  }

  /**
   * Limit the applicability of this agent to spy points whose observations include a key
   * matching {@code key} whose value, when converted to a String via {@code toString},
   * ends with {@code suffix}.
   *
   * @param key Key to look for in the observation
   * @param suffix Suffix to search for
   * @return This {@code SpyAgent} to facilitate the builder pattern
   */
  public SpyAgent withFilterKeyEndsWith(final String key, final String suffix) {
    _filters.add(new Filter() {
      @Override
      public boolean accept(Map<String, Object> map) {
        return map.containsKey(key) && map.get(key).toString().endsWith(suffix);
      }
    });
    return this;
  }

  /**
   * Limit the applicability of this agent to spy points for which {@code filter}
   * returns true. The filter will receive a map of all (key, value) pairs in the
   * observation as an argument to its {@code accept} method, which it can use to
   * determine applicability.
   *
   * @param filter The filter object to match against.
   * @return This {@code SpyAgent} to facilitate the builder pattern
   */
  public SpyAgent withFilter(Filter filter) {
    _filters.add(filter);
    return this;
  }

  /**
   * Specify multiple {@link Filter}s simultaneously.
   *
   * @see #withFilter(Filter)
   * @param filters Set of filters to apply
   * @return This {@code SpyAgent} to facilitate the builder pattern
   */
  public SpyAgent withFilters(Filter... filters) {
    for (Filter filter : filters) {
      withFilter(filter);
    }
    return this;
  }

  /**
   * Add a {@link Doer} to this agent. For any spy point which this agent is applicable to,
   * the {@link Doer}'s {@code accept} method will be called with a map of all (key, value)
   * pairs in the observation as an argument
   *
   * @param doer Doer to implement custom side effect logic
   * @return This {@code SpyAgent} to facilitate the builder pattern
   */
  public SpyAgent withDoer(Doer doer) {
    _doers.add(doer);
    return this;
  }

  /**
   * Specify multiple {@link Doer}s simultaneously.
   *
   * @see #withDoer(Doer)
   * @param doers Set of doers to apply
   * @return This {@code SpyAgent} to facilitate the builder pattern
   */
  public SpyAgent withDoers(Doer... doers) {
    _doers.addAll(Arrays.asList(doers));
    return this;
  }

  /**
   * Specify that this agent should throw an exception when a point is spied on that this agent
   * is applicable to.
   *
   * @param e The exception to be thrown
   * @return This {@code SpyAgent} to facilitate the builder pattern
   */
  public SpyAgent withException(RuntimeException e) {
    if (e == null) {
      throw new IllegalArgumentException("Cannot pass in null for exception - it can't be thrown!");
    }
    clearResults();
    _exception = e;
    return this;
  }

  /**
   * Specify that this agent should throw an exception when a point is spied on that this agent
   * is applicable to.
   *
   * @param e An {@link Excepter} which should return an exception to throw
   * @return This {@code SpyAgent} to facilitate the builder pattern
   */
  public SpyAgent withException(Excepter e) {
    if (e == null) {
      throw new IllegalArgumentException("Cannot pass in null for excepter - it can't be called!");
    }
    clearResults();
    _excepter = e;
    return this;
  }

  /**
   * Specify that this agent should throw a checked exception when a point is spied on that this
   * agent is applicable to.
   *
   * @param exception The checked exception to be thrown
   * @return A new agent which maintains all of the properties of the original agent but
   *         is marked as containing a checked exception
   */
  public SpyAgentWithCheckedException withException(Exception exception) {
    return new SpyAgentWithCheckedException(this, exception);
  }

  /**
   * Specify that this agent should throw a checked exception when a point is spied on that this
   * agent is applicable to.
   *
   * @param e An {@link CheckedExcepter} which should return a checked exception to throw
   * @return A new agent which maintains all of the properties of the original agent but
   *         is marked as containing a checked exception
   */
  public SpyAgentWithCheckedException withException(CheckedExcepter e) {
    return new SpyAgentWithCheckedException(this, e);
  }

  /**
   * Specify that this agent should return a value when a point is spied on that this
   * agent is applicable to.
   *
   * @param ret The object which should be returned
   * @return This {@code SpyAgent} to facilitate the builder pattern
   */
  public SpyAgent withResult(Object ret) {
    clearResults();
    _hasResult = true;
    _result = ret;
    return this;
  }

  /**
   * Specify that this agent should return a value when a point is spied on that this
   * agent is applicable to.
   *
   * @param ret A {@link Resulter} which should return the object to be returned.
   * @return This {@code SpyAgent} to facilitate the builder pattern
   */
  public SpyAgent withResult(Resulter ret) {
    if (ret == null) {
      throw new IllegalArgumentException("Cannot pass in null for resulter - it can't be called!");
    }
    clearResults();
    _resulter = ret;
    return this;
  }

  /**
   * Check if this agent is applicable to the current observation.
   *
   * @param observation The mapping of (key, value) pairs which comprises the
   *                    current observation.
   * @return True iff this agent is applicable based on all of its filters
   */
  boolean accept(Map<String, Object> observation) {
    for (Filter filter : _filters) {
      if (!filter.accept(observation)) {
        return false;
      }
    }
    return true;
  }

  // TODO right now impossible to differentiate between agent returning null
  // and agent returning nothing at all - both are Option.absent
  /**
   * Activate all doers, and either throw an exception or return a result depending
   * on what was specified when the agent was created.
   *
   * @param observation Set of (key, value) pairs comprising the current observation
   * @return The result that should be returned by the spy point
   */
  Optional<Object> performDoersGetReturn(Map<String, Object> observation) {
    for (Doer doer : _doers) {
      doer.accept(observation);
    }
    if (_exception != null) {
      throw _exception;
    } else if (_excepter != null) {
      throw _excepter.accept(observation);
    } else if (_hasResult) {
      return Optional.fromNullable(_result);
    } else if (_resulter != null) {
      return Optional.fromNullable(_resulter.accept(observation));
    }
    return Optional.absent();
  }

  /**
   * Clear the current set of results specified in this agent (results and exceptions)
   */
  private void clearResults() {
    _result = null;
    _resulter = null;
    _hasResult = false;
    _excepter = null;
    _exception = null;
  }

  /**
   * A {@link SpyAgent} which can also contain a checked exception. Created as necessary
   * by the {@code SpyAgent} builder pattern.
   */
  static class SpyAgentWithCheckedException extends SpyAgent {

    private CheckedExcepter _excepter;
    private Exception _exception;

    public SpyAgentWithCheckedException(SpyAgent parentAgent, Exception exception) {
      super();
      populateFieldsFromParent(parentAgent);
      _exception = exception;
    }

    public SpyAgentWithCheckedException(SpyAgent parentAgent, CheckedExcepter e) {
      super();
      populateFieldsFromParent(parentAgent);
      _excepter = e;
    }

    Exception getCheckedException(Map<String, Object> observationMap) {
      if (_excepter != null) {
        return _excepter.accept(observationMap);
      }
      return _exception;
    }

    private void populateFieldsFromParent(SpyAgent parentAgent) {
      _result = parentAgent._result;
      _resulter = parentAgent._resulter;
      _hasResult = parentAgent._hasResult;
      _doers = parentAgent._doers;
      _filters = parentAgent._filters;
    }

  }

}
