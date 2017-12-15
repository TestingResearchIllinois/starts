package first;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FourthTest {
    @Test
    public void test() {
        Fourth fourthVar = new Fourth();
        Boolean cond1 = fourthVar.firstInt.toString().equals("1");
        assertTrue(cond1);
    }
}
