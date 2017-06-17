package base;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

public class SimpleTest {

    @Test
    public void test() {
        Simple simple = new Simple();
        simple.add(1);
        simple.add(2);
        simple.add(3);
        Set<Integer> out = simple.getSet();
        int result = 0;
        for(Integer i : out) {
            result += i;
        }
        assertEquals("sum", 6, result);
    }

}
