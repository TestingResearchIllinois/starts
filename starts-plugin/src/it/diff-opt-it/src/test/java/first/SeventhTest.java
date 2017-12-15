package first;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SeventhTest {
    @Test
    public void test() {
        Seventh seventhVar = new Seventh();
        Boolean cond1 = seventhVar.firstInt.toString().equals("1");
        assertTrue(cond1);
    }
}
