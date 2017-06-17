/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package transitive;

import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.assertEquals;

public class SiblingTest {
    @Test
    public void test() {
        Sibling cousin = new Sibling();
        cousin.add(1);
        cousin.add(2);
        cousin.add(3);
        Set<Integer> out = cousin.getSet();
        int result = 0;
        for(Integer i : out) {
            result += i;
        }
        assertEquals("sum", 6, result);
    }
}
