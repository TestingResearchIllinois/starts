package first;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

public class First {
    private LinkedHashSet output;

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
	
	public String readXML() {
    	String fullFileName = "";
		StringBuilder result = new StringBuilder();
		try {
			URL resource = this.getClass().getClassLoader().getResource("books.xml");
			fullFileName = Paths.get(resource.toURI()).toRealPath().toString();
			File file = new File(fullFileName);
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				result.append(line).append("\n");
			}
		
			scanner.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result.toString();
	}
}
