package base;

import org.junit.Test;

public class SimpleTest {
    @Test
    public void test() throws ClassNotFoundException {
        Class.forName("base.Simple");
    }
}
