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
    final Gson plainGson = new GsonBuilder().setPrettyPrinting().create();

    return new GsonBuilder().setPrettyPrinting()
               .registerTypeAdapter(Map.class, new JsonSerializer<Map>() {
                 @Override
                 public JsonElement serialize(Map map, Type type, JsonSerializationContext jsc) {
                   if (map instanceof SortedMap) {
                     return plainGson.toJsonTree(map);
                   } else {
                     TreeMap tm = new TreeMap(new Comparator<Object>() {
                       @Override
                       public int compare(Object o1, Object o2) {
                         return o1.toString().compareTo(o2.toString());
                       }
                     });
                     tm.putAll(map);
                     return plainGson.toJsonTree(tm);
                   }
                 }
               })
               .registerTypeAdapter(Set.class, new JsonSerializer<Set>() {
                 @Override
                 public JsonElement serialize(Set set, Type type, JsonSerializationContext jsc) {
                   if (set instanceof SortedSet) {
                     return plainGson.toJsonTree(set);
                   } else {
                     TreeSet ts = new TreeSet(new Comparator<Object>() {
                       @Override
                       public int compare(Object o1, Object o2) {
                         return o1.toString().compareTo(o2.toString());
                       }
                     });
                     ts.addAll(set);
                     return plainGson.toJsonTree(ts);
                   }
                 }
               })
               .create();
  }

}
