package io.github.necula01.bond;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * <p>Class used to serialize objects into a JSON representation.</p>
 *
 * <p>Internally, this uses {@link Gson} to serialize objects. This means, by default,
 * all fields of an object are serialized. The exact settings that are used are as follows:</p>
 *
 * <pre><code>
 *   GsonBuilder().setPrettyPrinting().serializeNulls()
 *     .serializeSpecialFloatingPointValues().disableHtmlEscaping().create()
 * </code></pre>
 *
 * <p>In addition to the settings above, this class includes custom logic to serialize {@link Map Maps}
 * and {@link Set Sets} in a way that their values are sorted by taking the {@code toString} of values
 * (for sets) or keys (for maps) to ensure that they will be serialized deterministically. Maps which
 * inherit from {@link SortedMap} and Sets which inherit from {@link SortedSet} are serialized in their
 * normal sorted order.</p>
 *
 * <p>In future releases there will be the option to specify custom serializers for specific types.</p>
 */
public class Serializer {

  private final List<Object> typeAdapters = new ArrayList<>();
  private final List<Type> typeAdapterTypes = new ArrayList<>();
  private final List<Object> typeHierarchyAdapters = new ArrayList<>();
  private final List<Class<?>> typeHierarchyAdapterTypes = new ArrayList<>();

  private Gson gson = createGson();

  /**
   * Serialize the given object.
   *
   * @param obj The object to be serialized
   * @return A JSON string representation of {@code obj}
   */
  String serialize(Object obj) {
    return gson.toJson(obj);
  }

  /**
   * Specify that for objects of type clazz, simply call toString() on the object
   * to serialize it instead of doing a full JSON serialization.
   *
   * @param clazz Type for this to apply to
   * @param <T> Same as clazz
   * @return This object to facilitate a builder-style pattern
   */
  public <T> Serializer withToStringSerialization(Class<T> clazz) {
    return withTypeAdapter(clazz, new JsonSerializer<T>() {
      @Override
      public JsonElement serialize(T obj, Type type, JsonSerializationContext jsc) {
        return new JsonPrimitive(obj.toString());
      }
    });
  }

  /**
   * Set the amount of precision to be used when serializing doubles.
   *
   * @param places Number of places after the decimal to keep in serialized form
   * @return This object to facilitate a builder-style pattern
   */
  public Serializer withDoublePrecision(final int places) {
    return withTypeAdapter(Double.class, new JsonSerializer<Double>() {
      @Override
      public JsonElement serialize(Double dbl, Type type, JsonSerializationContext jsc) {
        return new JsonPrimitive(new BigDecimal(dbl).setScale(places, RoundingMode.HALF_UP).doubleValue());
      }
    });
  }

  /**
   * Set the amount of precision to be used when serializing floats.
   *
   * @param places Number of places after the decimal to keep in serialized form
   * @return This object to facilitate a builder-style pattern
   */
  public Serializer withFloatPrecision(final int places) {
    return withTypeAdapter(Float.class, new JsonSerializer<Float>() {
      @Override
      public JsonElement serialize(Float flt, Type type, JsonSerializationContext jsc) {
        return new JsonPrimitive(new BigDecimal(flt).setScale(places, RoundingMode.HALF_UP).doubleValue());
      }
    });
  }

  /**
   * Set a custom serialization method for type.
   * See {@link com.google.gson.GsonBuilder#registerTypeAdapter(Type, Object)}.
   *
   * @param type The type for this adapter to apply to
   * @param typeAdapter The adapter with custom serialization logic
   * @return This object to facilitate a builder-style pattern
   */
  public Serializer withTypeAdapter(Type type, Object typeAdapter) {
    typeAdapterTypes.add(type);
    typeAdapters.add(typeAdapter);
    gson = createGson();
    return this;
  }

  /**
   * Set a custom serialization method for baseType and its subtypes.
   * See {@link com.google.gson.GsonBuilder#registerTypeHierarchyAdapter(Class, Object)}.
   *
   * @param baseType The baseType for this adapter to apply to
   * @param typeAdapter The adapter with custom serialization logic
   * @return This object to facilitate a builder-style pattern
   */
  public Serializer withTypeHierarchyAdapter(Class<?> baseType, Object typeAdapter) {
    typeHierarchyAdapterTypes.add(baseType);
    typeHierarchyAdapters.add(typeAdapter);
    gson = createGson();
    return this;
  }

  private Gson createGson() {
    GsonBuilder builder = new GsonBuilder().setPrettyPrinting().serializeNulls()
               .serializeSpecialFloatingPointValues().disableHtmlEscaping()
               .registerTypeHierarchyAdapter(Map.class, new JsonSerializer<Map>() {
                 @Override
                 public JsonElement serialize(Map map, Type type, JsonSerializationContext jsc) {
                   if (map instanceof SortedMap) {
                     JsonObject jsonObj = new JsonObject();
                     Set<Map.Entry<?, ?>> entrySet = map.entrySet();
                     for (Map.Entry<?, ?> entry : entrySet) {
                       jsonObj.add(entry.getKey().toString(), jsc.serialize(entry.getValue()));
                     }
                     return jsonObj;
                   } else {
                     TreeMap tm = new TreeMap(new Comparator<Object>() {
                       @Override
                       public int compare(Object o1, Object o2) {
                         return o1.toString().compareTo(o2.toString());
                       }
                     });
                     tm.putAll(map);
                     return jsc.serialize(tm);
                   }
                 }
               })
               .registerTypeHierarchyAdapter(Set.class, new JsonSerializer<Set>() {
                 @Override
                 public JsonElement serialize(Set set, Type type, JsonSerializationContext jsc) {
                   if (set instanceof SortedSet) {
                     JsonArray jsonArray = new JsonArray();
                     for (Object entry : set) {
                       jsonArray.add(jsc.serialize(entry));
                     }
                     return jsonArray;
                   } else {
                     TreeSet ts = new TreeSet(new Comparator<Object>() {
                       @Override
                       public int compare(Object o1, Object o2) {
                         return o1.toString().compareTo(o2.toString());
                       }
                     });
                     ts.addAll(set);
                     return jsc.serialize(ts);
                   }
                 }
               });

    for (int i = 0; i < typeAdapters.size(); i++) {
      builder = builder.registerTypeAdapter(typeAdapterTypes.get(i), typeAdapters.get(i));
    }
    for (int i = 0; i < typeHierarchyAdapters.size(); i++) {
      builder = builder.registerTypeHierarchyAdapter(typeHierarchyAdapterTypes.get(i), typeHierarchyAdapters.get(i));
    }

    return builder.create();
  }

}
