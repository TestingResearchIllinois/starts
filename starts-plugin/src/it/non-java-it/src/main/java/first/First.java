package first;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Scanner;

public class First {
    public First() { }
    public String readXML() {
        String fullFileName = "";
        StringBuilder result = new StringBuilder();
        try {
            URL xmlResource = this.getClass().getClassLoader().getResource("books.xml");
            fullFileName = Paths.get(xmlResource.toURI()).toRealPath().toString();
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