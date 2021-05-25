package first;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FirstTest {
    @Test
    public void test() {
        First first = new First();
        assertTrue(first.firstInt.intValue() ==  0);
    }
}