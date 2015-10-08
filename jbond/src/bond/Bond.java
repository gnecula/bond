package bond;

import com.google.common.base.Joiner;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class Bond {

    private static Map<String, Object> returnValues = new HashMap<>();

    public static void addObserverReturn(String spyPointName, Object returnValue) {
        returnValues.put(spyPointName, returnValue);
    }

    public static Object observe(String spyPointName, String... observations) {
        System.out.printf("%s: %s\n", spyPointName, Joiner.on(",").join(observations));
        if (returnValues.containsKey(spyPointName)) {
            return returnValues.get(spyPointName);
        } else {
            return null;
        }
    }

    public static void prepForObservation(String packageName) throws Exception {
        Reflections reflects = new Reflections(packageName);
        Set<Class<?>> classes = reflects.getTypesAnnotatedWith(Observable.class);
        for (Class<?> clazz : classes) {
            PowerMockito.mock(clazz);
            PowerMockito.whenNew(clazz).withAnyArguments().thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock constructorIOM) throws Throwable {
//                    System.out.println("NOW MOCKING OUT A CALL TO NEW");
                    Class<?>[] paramTypes = new Class<?>[constructorIOM.getArguments().length];
                    for (int i = 0; i < constructorIOM.getArguments().length; i++) {
                        paramTypes[i] = constructorIOM.getArguments()[i].getClass();
                    }
                    Object realObject = clazz.getConstructor(paramTypes).newInstance(constructorIOM.getArguments());
                    Object spyObject = Mockito.spy(realObject);
                    for (Method method : clazz.getMethods()) {
                        if (method.isAnnotationPresent(Observe.class)) {
                            Observe obs = method.getAnnotation(Observe.class);
                            Object[] argTypeCatchers = new Object[method.getParameterCount()];
                            for (int i = 0; i < method.getParameterCount(); i++) {
                                argTypeCatchers[i] = getMockitoCatcherFromClass(method.getParameterTypes()[i]);
                            }
                            Parameter[] params = method.getParameters();

                            Mockito.when(method.invoke(spyObject, argTypeCatchers)).thenAnswer(new Answer<Object>() {
                                @Override
                                public Object answer(InvocationOnMock methodIOM) throws Throwable {
                                    String[] obsStrings = new String[params.length];
                                    for (int i = 0; i < params.length; i++) {
                                        obsStrings[i] = params[i].getName() + "=" + methodIOM.getArguments()[i];
                                    }
                                    Object response = Bond.observe(
                                            obs.spyPointName().equals(Observe.DEFAULT_POINT_NAME)
                                                    ? methodIOM.getMethod().getName() : obs.spyPointName(),
                                            obsStrings);
                                    if (obs.requireMock()) {
                                        assert response != null;
                                    }
                                    if (response != null) {
                                        return response;
                                    }
                                    return methodIOM.callRealMethod();
                                }
                            });
                        }
                    }
                    return spyObject;
                }
            });
        }
    }

    private static Object getMockitoCatcherFromClass(Class<?> clazz) {
        if (clazz == boolean.class) {
            return Mockito.anyBoolean();
        } else if (clazz == char.class) {
            return Mockito.anyChar();
        } else if (clazz == byte.class) {
            return Mockito.anyByte();
        } else if (clazz == short.class) {
            return Mockito.anyShort();
        } else if (clazz == int.class) {
            return Mockito.anyInt();
        } else if (clazz == long.class) {
            return Mockito.anyLong();
        } else if (clazz == float.class) {
            return Mockito.anyFloat();
        } else if (clazz == double.class) {
            return Mockito.anyDouble();
        } else {
            return Mockito.anyObject();
        }
    }
}
