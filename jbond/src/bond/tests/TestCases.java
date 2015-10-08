package bond.tests;

import bond.Bond;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareEverythingForTest;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
//@PrepareEverythingForTest
@PrepareForTest({TestClass.class})
public class TestCases {

    @Before
    public void setUp() throws Exception {
        Bond.prepForObservation("bond");
    }

    @Test
    public void myTestCase() {
        TestClass myClass = new TestClass();

        Bond.addObserverReturn("funcSpyPoint", 2);
        System.out.println(myClass.myFunction(5));
    }

}
