package first;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ThirdTest {
    @Test
    public void test() {
        Third thirdVar = new Third();
        Boolean cond1 = thirdVar.firstInt.toString().equals("1");
        assertTrue(cond1);
    }
}
