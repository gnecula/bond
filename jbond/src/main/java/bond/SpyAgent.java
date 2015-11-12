package bond;

import com.google.common.base.Optional;

import java.util.*;

public class SpyAgent {

  protected RuntimeException _exception;
  protected Excepter _excepter;
  protected boolean _hasResult = false;
  protected Object _result;
  protected Resulter _resulter;
  protected List<Doer> _doers = new ArrayList<>();
  protected List<Filter> _filters = new ArrayList<>();

  public SpyAgent withFilterKeyEq(final String key, final String value) {
    _filters.add(new Filter() {
      @Override
      public boolean accept(Map<String, Object> map) {
        return map.containsKey(key) && map.get(key).toString().equals(value);
      }
    });
    return this;
  }

  public SpyAgent withFilterKeyContains(final String key, final String substring) {
    _filters.add(new Filter() {
      @Override
      public boolean accept(Map<String, Object> map) {
        return map.containsKey(key) && map.get(key).toString().contains(substring);
      }
    });
    return this;
  }

  public SpyAgent withFilterKeyStartsWith(final String key, final String prefix) {
    _filters.add(new Filter() {
      @Override
      public boolean accept(Map<String, Object> map) {
        return map.containsKey(key) && map.get(key).toString().startsWith(prefix);
      }
    });
    return this;
  }

  public SpyAgent withFilterKeyEndsWith(final String key, final String suffix) {
    _filters.add(new Filter() {
      @Override
      public boolean accept(Map<String, Object> map) {
        return map.containsKey(key) && map.get(key).toString().endsWith(suffix);
      }
    });
    return this;
  }

  public SpyAgent withFilter(Filter filter) {
    _filters.add(filter);
    return this;
  }

  public SpyAgent withFilters(Filter... filters) {
    for (Filter filter : filters) {
      withFilter(filter);
    }
    return this;
  }

  public SpyAgent withDoer(Doer doer) {
    _doers.add(doer);
    return this;
  }

  public SpyAgent withDoers(Doer... doers) {
    _doers.addAll(Arrays.asList(doers));
    return this;
  }

  public SpyAgent withException(RuntimeException e) {
    if (e == null) {
      throw new IllegalArgumentException("Cannot pass in null for exception - it can't be thrown!");
    }
    clearResults();
    _exception = e;
    return this;
  }

  public SpyAgent withException(Excepter e) {
    if (e == null) {
      throw new IllegalArgumentException("Cannot pass in null for excepter - it can't be called!");
    }
    clearResults();
    _excepter = e;
    return this;
  }

  public <E extends Exception> SpyAgentWithCheckedException withException(Exception exception) {
    return new SpyAgentWithCheckedException(this, exception);
  }

  public <E extends Exception> SpyAgentWithCheckedException withException(CheckedExcepter e) {
    return new SpyAgentWithCheckedException(this, e);
  }

  public SpyAgent withResult(Object ret) {
    clearResults();
    _hasResult = true;
    _result = ret;
    return this;
  }

  public SpyAgent withResult(Resulter ret) {
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
    return null;
  }

  private void clearResults() {
    _result = null;
    _resulter = null;
    _hasResult = false;
    _excepter = null;
    _exception = null;
  }

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
