package bond;

import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;

import java.util.*;


public class SerializerTest {

  @Rule
  public BondTestRule btr = new BondTestRule();

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
    Bond.obs("stringOnlyHashMap", Serializer.serialize(stringOnlyHashMap))
        .obs("stringObjectHashMap", Serializer.serialize(stringObjectHashMap))
        .spy("hash maps");
    
    Map<String, String> stringOnlyTreeMap = new TreeMap<>(REVERSE_SORT_COMPARATOR);
    stringOnlyTreeMap.put("b", "bar");
    stringOnlyTreeMap.put("a", "foo");
    stringOnlyTreeMap.put("z", "baz");
    stringOnlyTreeMap.put("g", "test");
    Bond.obs("stringOnlyTreeMap", Serializer.serialize(stringOnlyTreeMap)).spy("tree maps");
  }
  
  @Test
  public void testSetSorting() {
    Set<String> stringHashSet = Sets.newHashSet("b", "a", "z", "g"); 

    Set<StringObject> stringObjectHashSet = Sets.newHashSet(new StringObject("b"), 
        new StringObject("a"), new StringObject("z"), new StringObject("g"));
    Bond.obs("stringHashSet", Serializer.serialize(stringHashSet))
        .obs("stringObjectHashSet", Serializer.serialize(stringObjectHashSet))
        .spy("hash sets");

    Set<String> stringOnlyTreeSet = new TreeSet<>(REVERSE_SORT_COMPARATOR);
    stringOnlyTreeSet.add("b");
    stringOnlyTreeSet.add("a");
    stringOnlyTreeSet.add("z");
    stringOnlyTreeSet.add("g");
    Bond.obs("stringOnlyTreeSet", Serializer.serialize(stringOnlyTreeSet)).spy("tree sets");
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

    Bond.obs("objectMap", Serializer.serialize(objectMap))
        .obs("objectSet", Serializer.serialize(objectSet)).spy();
  }

}
