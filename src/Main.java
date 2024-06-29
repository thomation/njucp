import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        String asm = null;
        if(args.length >= 2) {
            asm = args[1];
        }
        try {
            CharStream input = CharStreams.fromFileName(source);
            ParserTest t2 = new ParserTest(input, asm);
            t2.run();
        } catch (IOException e) {
            System.err.println("Error! " + e.getMessage());
        }
    }
}