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

   static class Nested {
       Integer sum = 0;
       static Short number = new Short((short)0);
        Nested() {
        }

       public static String getNestedNumberAsString() {
           return number.toString();
       }
    }

}
