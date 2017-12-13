package first;

import java.util.LinkedHashSet;
import java.util.Set;

public class First {
    private LinkedHashSet output;
    private Nested nestedSum;

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

    public String getNestedNumberAsString() {
        nestedSum = new Nested();
        return nestedSum.number.toString();
    }

   class Nested {
       Integer sum = 0;
       Short number = new Short((short)0);
        Nested() {
        }
    }

}
