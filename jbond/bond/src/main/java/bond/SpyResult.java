package bond;

/**
 * <p>A class used to hold the return values of spy points. This class is inspired by Google
 * Guava's {@link com.google.common.base.Optional}, but differs significantly in that it
 * can hold {@code null} values.
 *
 * <p>{@code SpyResult}s have essentially two states: {@link #isPresent()} {@code == true}
 * or {@code == false}. If true, this means the spy point returned a value (from a {@link SpyAgent}),
 * even if that value was {@code null}. You can then use {@link #get()} to access this value.
 * If false, no value was returned by the spy point, and calling {@link #get()} will throw an exception.
 *
 * @param <T> The type of value contained within this {@code SpyResult}.
 */
public abstract class SpyResult<T> {

  private SpyResult() {
    // Do nothing - just here to prevent a public constructor
  }

  /**
   * If {@link #isPresent()} is true, returns the value contained within this {@code SpyResult},
   * which may be null. Attempting to use this method otherwise will result in an exception.
   *
   * @return the value contained in this {@code SpyResult}
   * @throws IllegalStateException if {@link #isPresent()} is false
   */
  public abstract T get();

  /**
   * Returns whether or not this {@code SpyResult} contains a value.
   *
   * @return true iff this {@code SpyResult} contains a value.
   */
  public abstract boolean isPresent();

  /**
   * Returns the value contained within this {@code SpyResult} if {@link #isPresent()} is true,
   * else returns {@code alternate}.
   *
   * @param alternate  The value to be returned if no value is contained within this object
   * @return the value contained within if {@link #isPresent()} is true, else {@code alternate}
   */
  public T getOrElse(T alternate) {
    return isPresent() ? get() : alternate;
  }

  private static final SpyResultAbsent absent = new SpyResultAbsent();

  static <T> SpyResultPresent<T> of(T value) {
    return new SpyResultPresent<>(value);
  }

  static <S> SpyResultAbsent<S> absent() {
    return (SpyResultAbsent<S>) absent;
  }

  static class SpyResultPresent<T> extends SpyResult<T> {

    private final T value;

    public SpyResultPresent(T value) {
      this.value = value;
    }

    public boolean isPresent() {
      return true;
    }

    public T get() {
      return value;
    }

    public boolean equals(Object other) {
      if (!(other instanceof SpyResultPresent)) {
        return false;
      } else if (value == null) {
        return ((SpyResultPresent) other).value == null;
      }
      return value.equals(other);
    }

    public String toString() {
      return String.format("SpyResult(%s)", value);
    }

  }

  private static class SpyResultAbsent<S> extends SpyResult<S> {

    @Override
    public boolean isPresent() {
      return false;
    }

    @Override
    public S get() {
      throw new IllegalStateException("Called `get` on an absent SpyResult!");
    }

    public String toString() {
      return "SpyResultAbsent";
    }
  }

}

