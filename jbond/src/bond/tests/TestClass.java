package bond.tests;

public class TestClass {

    public static void main(String[] args) {



    }


    public int myFunction(int i) {
//        System.out.println("About to attempt to create a new MyDependency");
        MyDependency dep = new MyDependency();
//        System.out.println("Just created a new MyDependency");
        return dep.func(i * 10);
//        return bond.tests.MyDependency.func(); // i * 10);
    }

}
