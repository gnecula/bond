
package bond.example;

import bond.Bond;
import bond.BondTestRule;
import bond.Resulter;
import bond.SpyAgent;
import bond.spypoint.BondMockPolicy;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

// These three annotations are necessary to enable mocking of methods.
// See http://necula01.github.io/bond/jbond/bond/spypoint/BondMockPolicy.html
// for more detail
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.swing.*")
@MockPolicy(BusesMockPolicy.class)
public class BusesTest {

//  Uncomment below to enable use of Bond when running this test class
//  @Rule
//  public BondTestRule btr = new BondTestRule();
  
  /**
   * A test to verify the distance computation. Verified manually w.r.t.
   * http://andrew.hedges.name/experiments/haversine/
   */
  @Test
  public void testDistanceComputation() {
    double d1 = Buses.computeLatLonDistance(new PointImpl(38.898, -77.037),
        new PointImpl(38.897, -77.043));
    assertEquals(0.330, d1, 0.001); // 0.531 km

    double d2 = Buses.computeLatLonDistance(new PointImpl(38.898, -97.030),
        new PointImpl(38.890, -97.044));
    assertEquals(0.935, d2, 0.001); // 1.504 km

    double d3 = Buses.computeLatLonDistance(new PointImpl(38.958, -97.038),
        new PointImpl(38.890, -97.044));
    assertEquals(4.712, d3, 0.001); // 7.581 km

  }


  class PointImpl implements Buses.Point {
    private double lat;
    private double lon;
    public PointImpl(double lat, double lon) {
      this.lat = lat;
      this.lon = lon;
    }

    public double getLat() {
      return lat;
    }

    public double getLon() {
      return lon;
    }
  }
}

class BusesMockPolicy extends BondMockPolicy {
  @Override
  public String[] getPackageNames() {
    return new String[] {"bond.example"};
  }
}

