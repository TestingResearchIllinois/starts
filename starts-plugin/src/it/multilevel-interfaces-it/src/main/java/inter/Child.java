/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package inter;


import java.util.ArrayList;
import java.util.Arrays;

public class Child implements BaseA, BaseB {
    @Override
    public ArrayList<String> toStringsBaseA() {
        return new ArrayList<>(Arrays.asList("BaseA", "child"));
    }

    @Override
    public ArrayList<String> toStringsBaseB() {
        return new ArrayList<>(Arrays.asList("BaseB", "child"));
    }
}
