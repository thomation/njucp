import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        try {
            // CharStream input = CharStreams.fromFileName(source);
            // LexerTest t = new LexerTest(input);
            // t.run();
            CharStream input2 = CharStreams.fromFileName(source);
            ParserTest t2 = new ParserTest(input2);
            t2.run();
        } catch (Exception e) {
            System.err.println("Error! " + e.getMessage());
        }
    }
}