
public class SemanticVisitor extends SysYParserBaseVisitor<Void> {
    Scope curScope; 
    GlobalScope globalScope;
    @Override
    public Void visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        curScope = globalScope;
        return visitChildren(ctx);
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        return visitChildren(ctx);
    }
}
