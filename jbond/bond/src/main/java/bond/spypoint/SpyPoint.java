package bond.spypoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SpyPoint {

  String DEFAULT_POINT_NAME = "BOND_SPY_DEFAULT_POINT_NAME";

  /**
   * The name to use for this spy point. Defaults to {@code ClassName.methodName}.
   */
  String spyPointName() default DEFAULT_POINT_NAME;

  /**
   * If true: in addition to spying the arguments to the method being spied on,
   * spy the return value. This still applies even if the return value is mocked.
   * This option is ignored if the return type of the spied method is {@code void}.
   */
  boolean spyResult() default false;

  /**
   * If true: require that an agent with a return value is used to mock out this
   * method during testing. This is useful if you have some dangerous method that
   * should never be called during testing.
   */
  boolean requireMock() default false;
}
