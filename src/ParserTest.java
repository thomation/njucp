import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class ParserTest {
    SysYParser sysYParser;
    public ParserTest(CharStream input) {
        SysYLexerLexer sysYLexer = new SysYLexerLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        sysYParser = new SysYParser(tokens);
    }
    public void run() {
        sysYParser.removeErrorListeners();
        ParserErrorListner myErrorListener = new ParserErrorListner();
        sysYParser.addErrorListener(myErrorListener);
        ParseTree tree = sysYParser.program().getChild(0);
        Visitor visitor = new Visitor();
        visitor.visit(tree);
    }
}
