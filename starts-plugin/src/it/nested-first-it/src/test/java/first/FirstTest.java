package first;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class FirstTest {
    @Test
    public void test() {
        First first = new First();
        first.add(1);
        first.add(2);
        first.add(3);
        Set<Integer> out = first.getSet();
        int result = 0;
        for(Integer i : out) {
            result += i;
        }
        assertEquals("sum", 6, result);
    }

    @Test
    public void test1() {
        First first = new First();
        String derp = first.getNestedNumberAsString();
        assertTrue(derp.equals("0"));
    }
}
