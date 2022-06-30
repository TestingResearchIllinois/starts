/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package inter;

import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.assertEquals;

public class GrandChildTest {
    @Test
    public void test() {
        GrandChild grand = new GrandChild();
        grand.add(1);
        grand.add(2);
        grand.add(3);
        Set<Integer> out = grand.getSet();
        int result = 0;
        for (Integer i : out) {
            result += i;
        }
        assertEquals("sum", 6, result);
    }
}
