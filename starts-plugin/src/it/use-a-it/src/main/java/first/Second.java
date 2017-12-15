package first;

public class Second {
    public static First c1 = new First();
    
    public Second() { }
    
    public static Short Sum() {
        return (short)(2 + c1.firstInt.intValue());
    }
}