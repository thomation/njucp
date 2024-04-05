import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class ParserTest {
    SysYParser sysYParser;
    public ParserTest(CharStream input) {
        SysYLexerLexer sysYLexer = new SysYLexerLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        sysYParser = new SysYParser(tokens);
    }
    public void run() {
        sysYParser.program();
    }
}
