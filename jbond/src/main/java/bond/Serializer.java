package bond;

import com.google.gson.*;

import java.lang.reflect.Type;
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

  // Static methods only
  private Serializer() { }

  private static Gson gson = createGson();

  /**
   * Serialize the given object.
   *
   * @param obj The object to be serialized
   * @return A JSON string representation of {@code obj}
   */
  static String serialize(Object obj) {
    return gson.toJson(obj);
  }

  private static Gson createGson() {
    return new GsonBuilder().setPrettyPrinting().serializeNulls()
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
               })
               .create();
  }

}
