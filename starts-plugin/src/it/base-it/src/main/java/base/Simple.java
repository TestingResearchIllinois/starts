package base;

import java.util.LinkedHashSet;
import java.util.Set;

public class Simple {
    private LinkedHashSet output;

    public Simple() {
        super();
        output = new LinkedHashSet();
    }

    public void add(int addend) {
        output.add(addend);
    }

    public Set<Integer> getSet() {
        return output;
    }

}
