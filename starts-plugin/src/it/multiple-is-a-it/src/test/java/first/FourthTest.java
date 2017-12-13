package first;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FourthTest {
    @Test
    public void test() {
        //this test works for the first starts run, but since we change Fourth class that extends Third class, Third class will need to be recompiled for the second run of STARTS which currently isn't being done.
        Fourth fourthVar = new Fourth();
        Boolean cond1 = fourthVar.firstInt.toString().equals("1");
        /*Boolean cond2 = secondVar.secondInt.toString().equals("3");
        System.out.println("secondVar.firstInt: " + secondVar.firstInt.toString() + " secondVar.secondInt: " + secondVar.secondInt.toString());
        System.out.println("cond1: " + cond1.toString() + " cond2: " + cond2.toString());
        assertTrue(cond1 && cond2);*/
        assertTrue(cond1);
    }
}
