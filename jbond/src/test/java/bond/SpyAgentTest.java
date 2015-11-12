package bond;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpyAgentTest {

  @Rule
  public BondTestRule btr = new BondTestRule();

  @Test
  public void testFilterFunctions() {
    SpyAgent agent = new SpyAgent()
        .withFilter(new Filter() {
          @Override
          public boolean accept(Map<String, Object> map) {
            return map.get("key").equals("foobar");
          }
        });

    Map<String, Object> matchingMap = new HashMap<>();
    matchingMap.put("key", "foobar");

    Map<String, Object> notMatchingMap = new HashMap<>();
    notMatchingMap.put("key", "baz");
    notMatchingMap.put("key2", "foobar");

    Bond.obs("matchingMap", agent.accept(matchingMap))
        .obs("notMatchingMap", agent.accept(notMatchingMap))
        .spy();
  }

  @Test
  public void testFilterContains() {
    SpyAgent agent = new SpyAgent()
        .withFilterKeyContains("key", "foobar");

    List<String> stringList = Lists.newArrayList("foobar", "baz");
    Map<String, Object> matchingMap = new HashMap<>();
    matchingMap.put("key", stringList);
    matchingMap.put("key2", "other");

    Map<String, Object> notMatchingMap = new HashMap<>();
    notMatchingMap.put("key", "baz");
    notMatchingMap.put("key2", "foobar");

    Bond.obs("matchingMap", agent.accept(matchingMap))
        .obs("notMatchingMap", agent.accept(notMatchingMap))
        .spy();
  }

  @Test
  public void testFilterStartsWith() {
    SpyAgent agent = new SpyAgent()
                            .withFilterKeyStartsWith("key", "foo");

    Map<String, Object> matchingMap1 = new HashMap<>();
    matchingMap1.put("key", "foobar");
    matchingMap1.put("key2", "other");

    Map<String, Object> matchingMap2 = new HashMap<>();
    matchingMap2.put("key", "foo");
    matchingMap2.put("key2", "other");

    Map<String, Object> notMatchingMap = new HashMap<>();
    notMatchingMap.put("key", "barfoo");
    notMatchingMap.put("key2", "foobar");

    Bond.obs("matchingMap1", agent.accept(matchingMap1))
        .obs("matchingMap2", agent.accept(matchingMap2))
        .obs("notMatchingMap", agent.accept(notMatchingMap))
        .spy();
  }

  @Test
  public void testFilterEndsWith() {
    SpyAgent agent = new SpyAgent()
                            .withFilterKeyEndsWith("key", "foo");

    Map<String, Object> matchingMap1 = new HashMap<>();
    matchingMap1.put("key", "barfoo");
    matchingMap1.put("key2", "other");

    Map<String, Object> matchingMap2 = new HashMap<>();
    matchingMap2.put("key", "foo");
    matchingMap2.put("key2", "other");

    Map<String, Object> notMatchingMap = new HashMap<>();
    notMatchingMap.put("key", "foobar");
    notMatchingMap.put("key2", "barfoo");

    Bond.obs("matchingMap1", agent.accept(matchingMap1))
        .obs("matchingMap2", agent.accept(matchingMap2))
        .obs("notMatchingMap", agent.accept(notMatchingMap))
        .spy();
  }

  @Test
  public void testFilterEquals() {
    SpyAgent agent = new SpyAgent()
                            .withFilterKeyEq("key", "foo");

    Map<String, Object> matchingMap1 = new HashMap<>();
    matchingMap1.put("key", "foo");
    matchingMap1.put("key2", "other");

    Map<String, Object> notMatchingMap = new HashMap<>();
    notMatchingMap.put("key", "barfoo");
    notMatchingMap.put("key2", "foo");

    Bond.obs("matchingMap1", agent.accept(matchingMap1))
        .obs("notMatchingMap", agent.accept(notMatchingMap))
        .spy();
  }

  @Test
  public void testFilterComposition() {
    SpyAgent agent = new SpyAgent()
        .withFilterKeyEq("key", "value")
        .withFilterKeyContains("key2", "foo");

    Map<String, Object> matchingMap = new HashMap<>();
    matchingMap.put("key", "value");
    matchingMap.put("key2", "foobar");
    matchingMap.put("key3", "random");

    Map<String, Object> halfMatchingMap1 = new HashMap<>();
    halfMatchingMap1.put("key", "value");
    halfMatchingMap1.put("key3", "foobar");

    Map<String, Object> halfMatchingMap2 = new HashMap<>();
    halfMatchingMap2.put("key2", "foobar");
    halfMatchingMap2.put("key", "other");

    Bond.obs("matchingMap", agent.accept(matchingMap))
        .obs("halfMatchingMap1", agent.accept(halfMatchingMap1))
        .obs("halfMatchingMap2", agent.accept(halfMatchingMap2))
        .spy();
  }

}
