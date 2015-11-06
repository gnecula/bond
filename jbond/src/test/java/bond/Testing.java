package bond;

import com.google.common.base.Optional;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

public class Testing {

  @Rule
  public BondTestRule btr = new BondTestRule().withObservationDirectory(new File("/tmp/bond"));

  @Test
  public void testDeploy() {

    SpyAgent<Integer> agent = new SpyAgent<Integer>()
        .withFilterKeyContains("key", "substr")
        .withResult(10);
    Bond.deployAgent("myPointName", agent);

    Optional<Integer> myInt = Bond.obs("key", "vaflue").obs("key2", "val2").spy("myPointName");

    //InputStream myin = System.in;
    //System.out.println("he");

  }

}
