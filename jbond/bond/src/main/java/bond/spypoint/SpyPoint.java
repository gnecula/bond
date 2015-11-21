package bond.spypoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SpyPoint {

    String DEFAULT_POINT_NAME = "BOND_SPY_DEFAULT_POINT_NAME";

    String spyPointName() default DEFAULT_POINT_NAME;
    boolean requireMock() default false;
}
