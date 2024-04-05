import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        try {
            CharStream input = CharStreams.fromFileName(source);
            SysYLexerLexer sysYLexer = new SysYLexerLexer(input);
            LexerTest t = new LexerTest(sysYLexer);
            t.run();
            // CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
            // SysYParser sysYParser = new SysYParser(tokens);
            // sysYParser.program();
        } catch (Exception e) {
            System.err.println("Error! " + e.getMessage());
        }
    }
}