/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package inter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import static org.junit.Assert.assertEquals;

public class SiblingTest {
    @Test
    public void test() {
        assertEquals("1", new ArrayList<>(Arrays.asList("sibling")), new Sibling().toStringsBaseA());
    }
}
