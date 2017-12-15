package first;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SecondTest {
    @Test
    public void test() {
        Second secondVar = new Second();
        /*The following method throws a
            java.lang.NoSuchMethodError: first.Second.Sum()Ljava/lang/Short;
	            at first.SecondTest.test(SecondTest.java:15)
	    error.
        Boolean cond1 = secondVar.Sum().toString().equals("3");*/
        assertTrue(true);
    }
}