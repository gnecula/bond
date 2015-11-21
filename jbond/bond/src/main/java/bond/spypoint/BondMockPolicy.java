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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
          // TODO what about varags? they just come through as an array I'm assuming?
          Observation observation = Bond.obs(getObservationKey(parameterNames[0], parameterTypes[0]), args[0]);
          for (int i = 1; i < parameterTypes.length; i++) {
            observation = observation.obs(getObservationKey(parameterNames[i], parameterTypes[i]), args[i]);
            // TODO have some sort of *conditional compilation* to get param names in Java 8 but not Java 7:
            // obsStrings[i] = String.format("%s=%s", params[i].getName(), methodIOM.getArguments()[i]);
          }
          response = observation.spy(spyPointName, method.getReturnType());
        }
        if (obs.requireMock()) {
          assert response.isPresent();
        }
        if (response.isPresent()) {
          return response.get();
        }
        return method.invoke(proxy, args);
      }
    };
  }

  private static String getObservationKey(String parameterName, Type parameterType) {
    return String.format("%s[%s]", parameterName, ((Class) parameterType).getSimpleName());
  }

  private static Method[] getSpiedMethodsForClass(Class<?> clazz) {
    List<Method> annotatedMethods = new ArrayList<>();
    for (Method method : clazz.getMethods()) {
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
