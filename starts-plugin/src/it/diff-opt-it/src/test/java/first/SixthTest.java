package first;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SixthTest {
    @Test
    public void test() {
        Sixth sixthVar = new Sixth();
        Boolean cond1 = sixthVar.firstInt.toString().equals("1");
        assertTrue(cond1);
    }
}
