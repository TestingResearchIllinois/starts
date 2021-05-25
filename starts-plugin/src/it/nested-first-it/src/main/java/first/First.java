package first;

public class First {
    private Nested nestedSum;

    public First() { }

    public String getNestedNumberAsString() {
         nestedSum = new Nested();
         return nestedSum.number.toString();
    }

    class Nested {
         Integer sum = 0;
         Short number = new Short((short)0);

         Nested() { }
    }
}
