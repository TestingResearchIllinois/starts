package first;

public class First {
    public First() { }

    static class Nested {
        static Short number = new Short((short)0);

        Nested() { }

        public static String getNestedNumberAsString() {
            return number.toString();
        }
    }
}
