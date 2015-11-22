package bond.spypoint;

import bond.Bond;
import bond.Observation;
import com.google.common.base.Optional;
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

public abstract class BondMockPolicy implements PowerMockPolicy {

  public abstract String getPackageName();

  public void applyClassLoadingPolicy(MockPolicyClassLoadingSettings settings) {
    Set<Class<?>> mockClasses = getClassesFromPackage(getPackageName());
    String[] classNames = new String[mockClasses.size()];
    int idx = 0;
    for (Class<?> clazz : mockClasses) {
      classNames[idx++] = clazz.getName();
    }
    settings.addFullyQualifiedNamesOfClassesToLoadByMockClassloader(classNames);
  }

  public void applyInterceptionPolicy(MockPolicyInterceptionSettings settings) {
    for (Class<?> clazz : getClassesFromPackage(getPackageName())) {
      for (Method m : getSpiedMethodsForClass(clazz)) {
        settings.proxyMethod(m, getInvocationHandler(m));
      }
    }
  }

  private InvocationHandler getInvocationHandler(Method m) {
    final SpyPoint obs = m.getAnnotation(SpyPoint.class);
    final Type[] parameterTypes = m.getParameterTypes();
    final String[] parameterNames = new String[parameterTypes.length];
    try {
      // If we're in Java 8 and compilation occurred with the -parameters flag, we
      // can use reflection to get the names of the parameters.
      Method getParameterMethod = m.getClass().getMethod("getParameters");
      Object[] parameters = (Object[]) getParameterMethod.invoke(m);
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
        parameterNames[i] = "param" + i;
      }
    }
    return new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String spyPointName = obs.spyPointName().equals(SpyPoint.DEFAULT_POINT_NAME)
                                  ? proxy.getClass().getSimpleName() + "." + method.getName()
                                  : obs.spyPointName();
        Optional<?> response;
        if (parameterTypes.length == 0) {
          response = Bond.spy(spyPointName, method.getReturnType());
        } else {
          Observation observation = Bond.obs(getObservationKey(parameterNames[0], parameterTypes[0]), args[0]);
          for (int i = 1; i < parameterTypes.length; i++) {
            observation = observation.obs(getObservationKey(parameterNames[i], parameterTypes[i]), args[i]);
          }
          response = observation.spy(spyPointName, method.getReturnType());
        }
        if (obs.requireMock() && !response.isPresent()) {
          throw new IllegalStateException("You *must* mock out spy point: " + spyPointName);
        }
        Object retValue;
        if (response.isPresent()) {
          retValue = response.get();
        } else {
          retValue = method.invoke(proxy, args);
        }
        if (obs.spyResult()) {
          Bond.obs("result", retValue).spy(spyPointName + ".result");
        }
        return retValue;
      }
    };
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
