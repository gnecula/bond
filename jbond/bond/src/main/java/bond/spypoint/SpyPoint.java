package bond.spypoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>An annotation used to specify that a method should be spied on by Bond. Placing this
 * annotation will result in Bond placing a call to {@link bond.Bond#spy} whenever the
 * method is called; for example:</p>
 *
 * <pre><code>
 *   public class MyClass {
 *     &#64;SpyPoint
 *     public int myMethod(int myArg, String mySecondArg) {
 *       // your code here
 *     }
 *   }
 * </code></pre>
 *
 * <p>The above code will cause every call to {@code myMethod()} to result in a call to:</p>
 *
 * {@code Bond.obs("arg0[int]", myArg, "arg1[String]", mySecondArg).spy("MyClass.myMethod")}
 *
 * <p>Unfortunately, in Java 7, parameter names are not available at runtime, so arg0, arg1, ... are used
 * instead. In Java 8, for code compiled with the {@code -parameters} flag, the actual names will be used
 * instead.</p>
 *
 * <p>In addition to this, if the call to spy returns a value (through the use of {@link bond.SpyAgent#withResult}),
 * that value will be returned instead of calling the actual method. This can be useful to mock out certain methods
 * during testing. If the method you want to mock normally returns {@code void}, just have the SpyAgent return
 * any value, and this will mark the method as mocked. </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SpyPoint {

  String DEFAULT_POINT_NAME = "BOND_SPY_DEFAULT_POINT_NAME";

  /**
   * The name to use for this spy point. Defaults to {@code ClassName.methodName}.
   *
   * @return the name of this spy point
   */
  String spyPointName() default DEFAULT_POINT_NAME;

  /**
   * If true: in addition to spying the arguments to the method being spied on,
   * spy the return value. This still applies even if the return value is mocked.
   * This option is ignored if the return type of the spied method is {@code void}.
   *
   * @return whether or not to spy the result of the spied method
   */
  boolean spyResult() default false;

  /**
   * If true: require that an agent with a return value is used to mock out this
   * method during testing. This is useful if you have some dangerous method that
   * should never be called during testing.
   *
   * @return whether or not mocking is required for this spy point
   */
  boolean requireAgentResult() default false;
}
