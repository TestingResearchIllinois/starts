package first;

public class Second {

    public Short secondInt = new Short((short)0);
    public First c1 = new First();
    
    public Second() {
        secondInt = (short)2;
    }
    
    public Short Sum() {
        secondInt = (short)(secondInt.intValue() + c1.firstInt.intValue());
        return secondInt;
    }
}