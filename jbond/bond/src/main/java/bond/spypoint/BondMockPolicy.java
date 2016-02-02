package bond.spypoint;

import bond.Bond;
import bond.Observation;
import bond.SpyResult;
import org.powermock.core.spi.PowerMockPolicy;
import org.powermock.mockpolicies.MockPolicyClassLoadingSettings;
import org.powermock.mockpolicies.MockPolicyInterceptionSettings;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

/**
 * <p>A {@link PowerMockPolicy} which is used to place mock methods on classes
 * which have {@code @}{@link SpyPoint} annotations. It will only search for classes
 * inside of the package names returned by {@link #getPackageNames()}, which
 * must be overriden. You can create one subclass per test if you like, though it is
 * generally recommended that you create one subclass for the entire project which
 * simply returns the base package of your project. This will help to ensure that
 * any method throughout your code base which has an {@code @SpyPoint} annotation will
 * successfully be spied / mocked. You must specify this mocking policy as an annotation
 * on each one of your test classes (or specify it on some base class of all of your test
 * classes). For example:</p>
 *
 * <pre><code>
 *   package org.myproject;
 *
 *   // ... imports ...
 *
 *   &#64;RunWith(PowerMockRunner.class) // Necessary to enable PowerMock, which Bond uses for mocking
 *   &#64;PowerMockIgnore("javax.swing.*") // Necessary to stop PowerMock from messing with javax.swing,
 *                                     // which Bond depends on. If you have any other dependencies which
 *                                     // are incompatible with PowerMock, add them here as well.
 *   &#64;MockPolicy(MyProjectMockPolicy.class)
 *   public class MyProjectTest {
 *
 *     &#64;Rule
 *     public BondTestRule btr = new BondTestRule();
 *
 *     &#64;Test
 *     public void myTest() {
 *       // ...
 *     }
 *
 *   }
 * </code></pre>
 *
 * <p>And, in the same or another file:</p>
 *
 * <pre><code>
 *   package org.myproject;
 *
 *   &#64;MockPolicy(MyProjectMockPolicy.class)
 *   public class MyProjectMockPolicy extends BondMockPolicy {
 *
 *     public String[] getPackageNames() {
 *       return "org.myproject";
 *     }
 *
 *   }
 * </code></pre>
 *
 * <p>Unfortunately this is pretty cumbersome, due to the way PowerMock works. If you're
 * interested, read on for an explanation of all of these things and why we need them.
 * If you have any ideas on how we can simplify this, please let us know!</p>
 *
 * <ul>
 *   <li>{@code @}{@link org.junit.runner.RunWith}({@link org.powermock.modules.junit4.PowerMockRunner}{@code .class})
 *       - We need to use PowerMockRunner for PowerMock to be able to mock out arbitrary classes.</li>
 *   <li>{@code @}{@link org.powermock.core.classloader.annotations.PowerMockIgnore}{@code ("javax.swing.*")}
 *       - The way PowerMock hooks into the ClassLoader system to be able to do mocking creates some
 *       inconsistencies that {@code javax.swing} is unhappy about; we need to explicitly tell PowerMock
 *       to skip doing anything to it.</li>
 *   <li>{@code @}{@link org.powermock.core.classloader.annotations.MockPolicy}{@code (MyMockPolicy.class)}
 *       - This is the most cumbersome - you need to create a new subclass just to specify a package name.
 *       We need the MockPolicy to be able to let PowerMock know which classes it should prepare for mocking;
 *       this process doesn't play nicely with many things, so we want to restrict it to your project code
 *       only. Since you must specify just a class, not an instance, the class itself has to have the package
 *       information. There's no way to somehow infer your root package (as far as we know); we have considered
 *       attempting to use the package containing the currently running test as a default, but unfortunately this
 *       is not accessible within a MockPolicy. </li>
 * </ul>
 *
 * <p>We considered making a {@code BondTest} base test class which would have these annotations supplied for you.
 * However, this only does away with the {@code @RunWith} and {@code @}{@link org.junit.Rule} /
 * {@link bond.BondTestRule} pieces - the {@code @PowerMockIgnore} option does not seem to propagate to subclasses
 * correctly, and we cannot supply a default package name so you still need to create a new {@code BondMockPolicy}
 * and specify it using {@code @MockPolicy}.</p>
 *
 * <p>Again, this is all pretty nonideal; please let us know if you have suggestions!</p>
 *
 * @see SpyPoint
 * @see org.junit.runner.RunWith
 * @see org.powermock.modules.junit4.PowerMockRunner
 * @see org.powermock.core.classloader.annotations.PowerMockIgnore
 * @see org.powermock.core.classloader.annotations.MockPolicy
 * @see org.powermock.core.spi.PowerMockPolicy
 */
public abstract class BondMockPolicy implements PowerMockPolicy {

  /**
   * Specify which packages should be searched for mocked methods.
   *
   * @return the names of packages to search for mocked methods
   */
  public abstract String[] getPackageNames();

  public void applyClassLoadingPolicy(MockPolicyClassLoadingSettings settings) {
    Set<Class<?>> mockClasses = new HashSet<>();
    for (String packageName : getPackageNames()) {
      mockClasses.addAll(getClassesFromPackage(packageName));
    }
    String[] classNames = new String[mockClasses.size()];
    int idx = 0;
    for (Class<?> clazz : mockClasses) {
      classNames[idx++] = clazz.getName();
    }
    settings.addFullyQualifiedNamesOfClassesToLoadByMockClassloader(classNames);
  }

  public void applyInterceptionPolicy(MockPolicyInterceptionSettings settings) {
    for (String packageName : getPackageNames()) {
      for (Class<?> clazz : getClassesFromPackage(packageName)) {
        for (Method m : getSpiedMethodsForClass(clazz)) {
          settings.proxyMethod(m, getInvocationHandler(m));
        }
      }
    }
  }

  private InvocationHandler getInvocationHandler(Method m) {
    final SpyPoint spyPoint = m.getAnnotation(SpyPoint.class);
    final Type[] parameterTypes = m.getParameterTypes();
    final String[] parameterNames = getParameterNames(m);
    return new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String spyPointName = spyPoint.spyPointName().equals(SpyPoint.DEFAULT_POINT_NAME)
                                  ? method.getDeclaringClass().getSimpleName() + "." + method.getName()
                                  : spyPoint.spyPointName();
        SpyResult<?> response;
        if (parameterTypes.length == 0) {
          response = Bond.spy(spyPointName, method.getReturnType(), spyPoint.mockOnly());
        } else {
          Observation observation = Bond.obs(getObservationKey(parameterNames[0], parameterTypes[0]), args[0]);
          for (int i = 1; i < parameterTypes.length; i++) {
            observation = observation.obs(getObservationKey(parameterNames[i], parameterTypes[i]), args[i]);
          }
          response = observation.spy(spyPointName, method.getReturnType(), spyPoint.mockOnly());
        }
        if (spyPoint.requireAgentResult() && !response.isPresent()) {
          throw new IllegalStateException("You *must* mock out spy point: " + spyPointName);
        }
        Object retValue;
        if (response.isPresent()) {
          retValue = response.get();
        } else {
          retValue = method.invoke(proxy, args);
        }
        if (spyPoint.spyResult()) {
          Bond.obs("result", retValue).spy(spyPointName + ".result");
        }
        return retValue;
      }
    };
  }

  private String[] getParameterNames(Method method) {
    String[] parameterNames = new String[method.getParameterTypes().length];
    try {
      // If we're in Java 8 and compilation occurred with the -parameters flag, we
      // can use reflection to get the names of the parameters.
      Method getParameterMethod = method.getClass().getMethod("getParameters");
      Object[] parameters = (Object[]) getParameterMethod.invoke(method);
      if (parameters.length != parameterNames.length) {
        throw new RuntimeException("Parameter name array and type array lengths must match!");
      }
      for (int i = 0; i < parameters.length; i++) {
        Object param = parameters[i];
        Method getNameMethod = param.getClass().getMethod("getName");
        Object paramName = getNameMethod.invoke(param);
        parameterNames[i] = paramName.toString();
      }
    } catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException|ClassCastException e) {
      for (int i = 0; i < parameterNames.length; i++) {
        parameterNames[i] = "arg" + i;
      }
    }
    return parameterNames;
  }

  private static String getObservationKey(String parameterName, Type parameterType) {
    if (parameterType instanceof Class) {
      return String.format("%s[%s]", parameterName, ((Class) parameterType).getSimpleName());
    } else {
      // This case should never happen, but just in case...
      return parameterName;
    }
  }

  private static Method[] getSpiedMethodsForClass(Class<?> clazz) {
    Set<Method> annotatedMethods = new HashSet<>();
    for (Method method : clazz.getMethods()) {
      if (method.isAnnotationPresent(SpyPoint.class)) {
        annotatedMethods.add(method);
      }
    }
    for (Method method : clazz.getDeclaredMethods()) {
      if (method.isAnnotationPresent(SpyPoint.class)) {
        annotatedMethods.add(method);
      }
    }
    return annotatedMethods.toArray(new Method[annotatedMethods.size()]);
  }

  private static Set<Class<?>> getClassesFromPackage(String packageName) {
    Reflections reflections = new Reflections(packageName, new SubTypesScanner(false), new ResourcesScanner());
    return reflections.getSubTypesOf(Object.class);
  }

}
