package bond;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.*;


public class Testing {

  //@Test
  public void testSerialization() {

    /**
     *
     *
     *
     * SPYING:
     *
     * STYLE 1a:
     * Bond.spy("pointName", new Obs("key", value), new Obs("key", value), ...)
     * Bond.spy(new Obs("key", value), new Obs("key", value), ...)
     * Bond.spy("pointName", new Obs("key", value), new Obs("key", value), ...).andIntegerResult()
     * Bond.spy("pointName", new Obs("key", value), new Obs("key", value), ...).noResult() <- optional
     *
     * 1b:
     * Bond.spy("pointName", new Obs("key", value), new Obs("key", value), ...)
     * Bond.spyWithInteger("pointName", new Obs("key", value), new Obs("key", value), ...)
     *
     * STYLE 2a:
     * Bond.spy("name").obs("key", value).obs("key", value).withoutResult() <- required
     * Bond.spy().obs("key", value).obs("key", value).withoutResult()
     * Bond.spy("name").obs("key", value).obs("key", value).andIntegerResult()
     *
     * 2b: ***
     * Bond.obs("key", value).obs("key", value).spy("name")
     * Bond.obs("key", value).obs("key", value).spy()
     * Bond.obs("key", value).obs("key", value).spyWithInteger("name")
     *
     * STYLE 3:  ** note, completely unchecked, suuuuper bad **
     * Bond.spy("key", value, "key", value, ...)
     * Bond.spyWithName("pointName", "key", value, "key", value, ...)
     * Bond.spy("key", value, "key", value, ...).andIntegerResult()
     * Bond.spy("key", value, "key", value, ...).andNoResult() <- optional
     *
     *
     * DEPLOYING:
     *
     * STYLE 1a:
     * SpyAgent<Integer> agent = SpyAgent.on("pointName").withResult(10);
     * Bond.deployIntegerAgent(agent);
     *
     * SpyAgent<Object> agent = SpyAgent.on("pointName");
     * Bond.deployAgent(agent); // no return value; object above is annoyingly necessary though
     *
     * 1b:
     * IntegerSpyAgent agent = SpyAgent.on("pointName").withResult(10);
     * Bond.deployAgent(agent);
     *
     * IntegerSpyAgent agent = SpyAgent.on("pointName").withIntegerResulter(new Resulter<Integer>() { ... });
     * Bond.deployAgent(agent);
     *
     * ResultlessSpyAgent agent = SpyAgent.on("pointName").withoutResult();
     * Bond.deployAgent(agent);
     *
     * STYLE 2: ***
     * SpyAgent agent = SpyAgent.on("pointName");
     * Bond.deployAgent(agent); // no result value
     *
     * SpyAgent agent = SpyAgent.on("pointName");
     * Bond.deployAgent(agent, 10); // automatically makes it an integer spy point, cool
     *
     * SpyAgent agent = SpyAgent.on("pointName");
     * Bond.deployAgentWithIntegerResulter(agent, new Resulter<Integer>() { public Integer accept... });
     *     AND/OR
     * SpyAgentWithResult<Integer> = SpyAgent.on("pointName").withResulter(new Resulter<Integer>() {...});
     * Bond.deployAgent(agent);
     *
     *
     *
     *
     *
     * General variant: Option with all of them: allow an additional "withException" call to specify
     * that your agent returns some checked exception (but not more specific than Exception)
     */

  }

  @Rule
  public BondTestRule btr = new BondTestRule().withObservationDirectory(new File("/tmp/bond"));

  @Test
  public void testBlank() {
    System.out.println("TEST BLANK");
  }

  @Test
  public void testWithFailure() {
    System.out.println("TESTWITHFAILURE");
    fail("my message");
  }

  @Test
  public void testWithException() {
    throw new RuntimeException("BLAH");
  }

}
