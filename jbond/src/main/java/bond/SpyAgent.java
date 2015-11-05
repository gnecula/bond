package bond;

import com.google.common.base.Optional;

import java.util.*;

public class SpyAgent<T> {

  private final String _spyPointName;

  private RuntimeException _exception;
  private Excepter _excepter;
  private boolean _hasResult = false;
  private T _result;
  private Resulter<T> _resulter;
  private List<Doer> _doers = new ArrayList<>();
  private List<Filter> _filters = new ArrayList<>();

  // TODO anything to do here?
  private SpyAgent(String spyPointName) {
    _spyPointName = spyPointName;
  }

  public static <T> SpyAgent<T> on(String spyPointName) {
    return new SpyAgent<>(spyPointName);
  }

  public String getSpyPointName() {
    return _spyPointName;
  }

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

}
