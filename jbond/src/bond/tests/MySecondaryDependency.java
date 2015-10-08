package bond.tests;

import bond.Observable;
import bond.Observe;

/**
 * Created by erik on 10/8/15.
 */
@Observable
public class MySecondaryDependency {

    @Observe
    public int blah(int i) {
        System.out.println("inside of blah");
        return i * 10;
    }

}
