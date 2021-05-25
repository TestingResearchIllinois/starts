package first;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class FirstTest {
    @Test
    public void test() {
        First first = new First();
        String derp = first.getNestedNumberAsString();
        assertTrue(derp.equals("0"));
    }
}
