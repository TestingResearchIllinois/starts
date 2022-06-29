/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package inter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import static org.junit.Assert.assertEquals;

public class GrandChildTest {
    @Test
    public void test() {
        assertEquals("1", new ArrayList<>(Arrays.asList("BaseA", "GrandChild")), new GrandChild().toStringsBaseA());
        assertEquals("2", new ArrayList<>(Arrays.asList("BaseB", "child")), new GrandChild().toStringsBaseB());
    }
}
