/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package base;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class FirstBaseTest {
    @Test
    public void test() {
        Base base1 = new Base();
        base1.add(1);
        base1.add(2);
        base1.add(3);
        Set<Integer> out = base1.getSet();
        int result = 0;
        for (Integer i : out) {
            result += i;
        }
        assertEquals("sum", 6, result);
    }
}
