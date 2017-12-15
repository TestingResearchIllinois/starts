package first;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FirstTest {
    @Test
    public void test() {
        First first = new First();
        Boolean cond = first.firstInt.toString().equals("1");
        assertTrue(cond);
    }
}
