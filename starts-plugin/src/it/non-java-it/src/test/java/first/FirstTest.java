package first;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FirstTest {
	@Test
	public void test() {
	    First first = new First();
	    String result = first.readXML();
	    assertTrue(result.contains("bk104") || result.contains("boook104"));
	}
}