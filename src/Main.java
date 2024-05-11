import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        try {
            CharStream input = CharStreams.fromFileName(source);
            ParserTest t2 = new ParserTest(input);
            t2.run();
        } catch (IOException e) {
            System.err.println("Error! " + e.getMessage());
        }
    }
}