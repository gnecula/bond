package bond.tests;

import bond.Observable;
import bond.Observe;

/**
 * Created by erik on 10/7/15.
 */
@Observable
public class MyDependency {

    @Observe(
            spyPointName = "funcSpyPoint",
            requireMock = false
    )
    public int func(int funcArg) {
//        System.out.println("inside of func");
//        MySecondaryDependency msd = new MySecondaryDependency();
//        System.out.println("inside of func after new creation");
//        return 10 * msd.blah(i);
        return funcArg * 10;
    }

}
