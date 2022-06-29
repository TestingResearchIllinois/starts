/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package inter;

import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.assertEquals;

public class ChildTest {
    @Test
    public void test() {
        Child son = new Child();
        son.add(1);
        son.add(2);
        son.add(3);
        Set<Integer> out = son.getSet();
        int result = 0;
        for (Integer i : out) {
            result += i;
        }
        assertEquals("sum", 6, result);
    }
}
