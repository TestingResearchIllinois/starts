/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package transitive;

import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.assertEquals;

public class BaseTest {
    @Test
    public void test() {
        Base base = new Base();
        base.add(1);
        base.add(2);
        base.add(3);
        Set<Integer> out = base.getSet();
        int result = 0;
        for(Integer i : out) {
            result += i;
        }
        assertEquals("sum", 6, result);
    }
}
