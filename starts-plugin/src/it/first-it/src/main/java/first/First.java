package first;

import java.util.LinkedHashSet;
import java.util.Set;

public class First {
    private LinkedHashSet output;

    public First() {
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
