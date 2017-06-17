/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package base;

import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.assertEquals;

public class SecondBaseTest {
    @Test
    public void multiTest2() {
        Base base2 = new Base();
        Set<Integer> output = base2.getSet();
        assertEquals(0, output.size());
    }
}
