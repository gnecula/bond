package bond;

import com.google.common.base.Optional;

import java.util.*;

public class SpyAgent<T> {

  protected RuntimeException _exception;
  protected Excepter _excepter;
  protected boolean _hasResult = false;
  protected T _result;
  protected Resulter<T> _resulter;
  protected List<Doer> _doers = new ArrayList<>();
  protected List<Filter> _filters = new ArrayList<>();

  // TODO decide if we should call toString on the object before comparing to
  // value, tojson on it, or what?
  public SpyAgent<T> withFilterKeyEq(final String key, final String value) {
    _filters.add(new Filter() {
      @Override
      public boolean accept(Map<String, Object> map) {
        return map.containsKey(key) && map.get(key).toString().equals(value);
      }
    });
    return this;
  }

  public SpyAgent<T> withFilterKeyContains(final String key, final String substring) {
    _filters.add(new Filter() {
      @Override
      public boolean accept(Map<String, Object> map) {
        return map.containsKey(key) && map.get(key).toString().contains(substring);
      }
    });
    return this;
  }

  public SpyAgent<T> withFilterKeyStartsWith(final String key, final String prefix) {
    _filters.add(new Filter() {
      @Override
      public boolean accept(Map<String, Object> map) {
        return map.containsKey(key) && map.get(key).toString().startsWith(prefix);
      }
    });
    return this;
  }

  public SpyAgent<T> withFilterKeyEndsWith(final String key, final String suffix) {
    _filters.add(new Filter() {
      @Override
      public boolean accept(Map<String, Object> map) {
        return map.containsKey(key) && map.get(key).toString().endsWith(suffix);
      }
    });
    return this;
  }

  public SpyAgent<T> withFilter(Filter filter) {
    _filters.add(filter);
    return this;
  }

  public SpyAgent<T> withFilters(Filter... filters) {
    for (Filter filter : filters) {
      withFilter(filter);
    }
    return this;
  }

  public SpyAgent<T> withDoer(Doer doer) {
    _doers.add(doer);
    return this;
  }

  public SpyAgent<T> withDoers(Doer... doers) {
    _doers.addAll(Arrays.asList(doers));
    return this;
  }

  // TODO: Only supports RuntimeExceptions. To get around this we would need to
  // duplicate *all* of the typed spy/return methods with another version that
  // throws checked exceptions - and even then the only info you would get is
  // *throws Exception*, not even a specific type, which is the only situation
  // that I can see being useful...
  public SpyAgent<T> withException(RuntimeException e) {
    if (e == null) {
      throw new IllegalArgumentException("Cannot pass in null for exception - it can't be thrown!");
    }
    clearResults();
    _exception = e;
    return this;
  }

  public SpyAgent<T> withException(Excepter e) {
    if (e == null) {
      throw new IllegalArgumentException("Cannot pass in null for excepter - it can't be called!");
    }
    clearResults();
    _excepter = e;
    return this;
  }

  public <E extends Exception> SpyAgentWithCheckedException<T, E> withCheckedException(E exception) {
    return new SpyAgentWithCheckedException<>(this, exception);
  }

  public <E extends Exception> SpyAgentWithCheckedException<T, E> withCheckedException(CheckedExcepter<E> e) {
    return new SpyAgentWithCheckedException<>(this, e);
  }

  public SpyAgent<T> withResult(T ret) {
    clearResults();
    _hasResult = true;
    _result = ret;
    return this;
  }

  public SpyAgent<T> withResult(Resulter<T> ret) {
    if (ret == null) {
      throw new IllegalArgumentException("Cannot pass in null for returner - it can't be called!");
    }
    clearResults();
    _resulter = ret;
    return this;
  }

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
  Optional<T> performDoersGetReturn(Map<String, Object> observation) {
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
    return null;
  }

  private void clearResults() {
    _result = null;
    _resulter = null;
    _hasResult = false;
    _excepter = null;
    _exception = null;
  }

  static class SpyAgentWithCheckedException<T, E extends Exception> extends SpyAgent<T> {

    private CheckedExcepter<E> _excepter;
    private E _exception;

    public SpyAgentWithCheckedException(SpyAgent<T> parentAgent, E exception) {
      super();
      populateFieldsFromParent(parentAgent);
      _exception = exception;
    }

    public SpyAgentWithCheckedException(SpyAgent<T> parentAgent, CheckedExcepter<E> e) {
      super();
      populateFieldsFromParent(parentAgent);
      _excepter = e;
    }

    void throwCheckedException(Map<String, Object> observationMap) throws E {
      if (_exception != null) {
        throw _exception;
      }
      if (_excepter != null) {
        throw _excepter.accept(observationMap);
      }
    }

    private void populateFieldsFromParent(SpyAgent<T> parentAgent) {
      _result = parentAgent._result;
      _resulter = parentAgent._resulter;
      _hasResult = parentAgent._hasResult;
      _doers = parentAgent._doers;
      _filters = parentAgent._filters;
    }

  }

}
