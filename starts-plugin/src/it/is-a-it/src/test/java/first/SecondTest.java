package first;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SecondTest {
    @Test
    public void test() {
        Second second = new Second();
        Boolean cond = second.firstInt.toString().equals("1");
        assertTrue(cond);
    }
}
