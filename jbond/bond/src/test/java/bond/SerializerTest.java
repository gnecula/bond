package bond;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.*;


public class SerializerTest {

  @Rule
  public BondTestRule btr = new BondTestRule();

  private Serializer serializer;

  private class StringObject {
    private String str;
    public StringObject(String str) {
      this.str = str;
    }
    public String toString() {
      return this.str;
    }
  }
  
  private static final Comparator<String> REVERSE_SORT_COMPARATOR = new Comparator<String>() {
    @Override
    public int compare(String o1, String o2) {
      return -1 * o1.compareTo(o2);
    }
  };

  @Before
  public void setup() {
    serializer = new Serializer();
  }

  @Test
  public void testMapSorting() {
    Map<String, String> stringOnlyHashMap = new HashMap<>();
    stringOnlyHashMap.put("b", "bar");
    stringOnlyHashMap.put("a", "foo");
    stringOnlyHashMap.put("z", "baz");
    stringOnlyHashMap.put("g", "test");
    
    Map<StringObject, String> stringObjectHashMap = new HashMap<>();
    stringObjectHashMap.put(new StringObject("b"), "bar");
    stringObjectHashMap.put(new StringObject("a"), "foo");
    stringObjectHashMap.put(new StringObject("z"), "baz");
    stringObjectHashMap.put(new StringObject("g"), "test");
    Bond.obs("stringOnlyHashMap", serializer.serialize(stringOnlyHashMap))
        .obs("stringObjectHashMap", serializer.serialize(stringObjectHashMap))
        .spy("hash maps");
    
    Map<String, String> stringOnlyTreeMap = new TreeMap<>(REVERSE_SORT_COMPARATOR);
    stringOnlyTreeMap.put("b", "bar");
    stringOnlyTreeMap.put("a", "foo");
    stringOnlyTreeMap.put("z", "baz");
    stringOnlyTreeMap.put("g", "test");
    Bond.obs("stringOnlyTreeMap", serializer.serialize(stringOnlyTreeMap)).spy("tree maps");
  }
  
  @Test
  public void testSetSorting() {
    Set<String> stringHashSet = Sets.newHashSet("b", "a", "z", "g"); 

    Set<StringObject> stringObjectHashSet = Sets.newHashSet(new StringObject("b"), 
        new StringObject("a"), new StringObject("z"), new StringObject("g"));
    Bond.obs("stringHashSet", serializer.serialize(stringHashSet))
        .obs("stringObjectHashSet", serializer.serialize(stringObjectHashSet))
        .spy("hash sets");

    Set<String> stringOnlyTreeSet = new TreeSet<>(REVERSE_SORT_COMPARATOR);
    stringOnlyTreeSet.add("b");
    stringOnlyTreeSet.add("a");
    stringOnlyTreeSet.add("z");
    stringOnlyTreeSet.add("g");
    Bond.obs("stringOnlyTreeSet", serializer.serialize(stringOnlyTreeSet)).spy("tree sets");
  }

  private class NestingObject {
    public Set<String> stringSet;
    public Map<String, String> stringMap;
  }

  @Test
  public void testNestedSorting() {
    Set<String> stringHashSet = Sets.newHashSet("b", "a", "z", "g");
    Map<String, String> stringHashMap = new HashMap<>();
    stringHashMap.put("b", "bar");
    stringHashMap.put("a", "foo");
    stringHashMap.put("z", "baz");
    stringHashMap.put("g", "test");

    NestingObject no = new NestingObject();
    no.stringMap = stringHashMap;
    no.stringSet = stringHashSet;

    Map<String, Object> objectMap = new HashMap<>();
    objectMap.put("nestingObject", no);
    objectMap.put("hashSet", stringHashSet);
    objectMap.put("hashMap", stringHashMap);

    Set<Object> objectSet = Sets.newHashSet(stringHashSet, stringHashMap, no);

    Bond.obs("objectMap", serializer.serialize(objectMap))
        .obs("objectSet", serializer.serialize(objectSet)).spy();
  }

  @Test
  public void testRestrictedDoublePrecision() {
    serializer = serializer.withDoublePrecision(3);
    Bond.obs("pi", serializer.serialize(Math.PI)).spy();
  }

  @Test
  public void testRestrictedFloatPrecision() {
    serializer = serializer.withFloatPrecision(5);
    Bond.obs("1.23456789f", serializer.serialize(1.23456789f)).spy();
  }

  @Test
  public void testOverwriteOldDoublePrecision() {
    serializer = serializer.withDoublePrecision(2);
    serializer = serializer.withDoublePrecision(7);
    Bond.obs("pi", serializer.serialize(Math.PI)).spy();
  }

  @Test
  public void testCustomTypeAdapter() {
    serializer = serializer.withTypeAdapter(StringObject.class, new JsonSerializer<StringObject>() {
      @Override
      public JsonElement serialize(StringObject strObj, Type type, JsonSerializationContext jsc) {
        return new JsonPrimitive(strObj.toString() + ", with custom serialization!");
      }
    });

    Bond.obs("StringObject", serializer.serialize(new StringObject("foobar"))).spy();
  }

  @Test
  public void testUseToStringSerialization() {
    serializer = serializer.withToStringSerialization(StringObject.class);

    Bond.obs("StringObject.toString()", serializer.serialize(new StringObject("foobar"))).spy();
  }
}
