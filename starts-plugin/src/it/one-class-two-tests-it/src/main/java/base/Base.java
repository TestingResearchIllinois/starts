/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package base;

import java.util.LinkedHashSet;
import java.util.Set;

public class Base {
    private LinkedHashSet output;

    public Base() {
        super();
        output = new LinkedHashSet();
    }

    public void add(int a) {
        output.add(a);
    }

    public Set<Integer> getSet() {
        return output;
    }
}
