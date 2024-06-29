import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class ParserTest {
    SysYParser sysYParser;
    String outputPath;

    public ParserTest(CharStream input, String outputPath) {
        this.outputPath = outputPath;
        SysYLexerLexer sysYLexer = new SysYLexerLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        sysYParser = new SysYParser(tokens);
    }

    public void run() {
        sysYParser.removeErrorListeners();
        ParserErrorListner myErrorListener = new ParserErrorListner();
        sysYParser.addErrorListener(myErrorListener);
        ParseTree tree = sysYParser.program();
        PrettyVisitor v1 = new PrettyVisitor();
        v1.visit(tree);
        SemanticVisitor v2 = new SemanticVisitor();
        v2.visit(tree);
        ParseVisitor v3 = new ParseVisitor();
        v3.visit(tree);
        OutputHelper.getInstance().printResult();
        if (!OutputHelper.getInstance().hasError()) {
            LLVMVisitor v4 = new LLVMVisitor();
            v4.visit(tree);
            if (outputPath != null) {
                AsmBuilder asm = new AsmBuilder(v4.getModule(), outputPath);
                asm.build();
            }
        }
    }
}
