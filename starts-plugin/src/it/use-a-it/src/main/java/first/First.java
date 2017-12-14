package first;

import java.util.LinkedHashSet;
import java.util.Set;

public class First {
    private LinkedHashSet output;
    public Integer firstInt = 0;

    public First() {
        super();
        output = new LinkedHashSet();
        firstInt = 1;
    }

    public void add(int addend) {
        output.add(addend);
    }

    public Set<Integer> getSet() {
        return output;
    }

}
