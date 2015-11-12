package bond;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.*;

public class Serializer {

  private static Gson gson = createGson();

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
