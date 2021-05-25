package first;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SecondTest {
    @Test
    public void test() {
        Second secondVar = new Second();
        Boolean cond1 = secondVar.firstInt.toString().equals("1");
        assertTrue(cond1);
    }
}
