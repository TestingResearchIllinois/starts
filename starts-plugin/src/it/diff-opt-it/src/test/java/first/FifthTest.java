package first;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FifthTest {
    @Test
    public void test() {
        Fourth fifthVar = new Fifth();
        Boolean cond1 = fifthVar.firstInt.toString().equals("1");
        assertTrue(cond1);
    }
}
