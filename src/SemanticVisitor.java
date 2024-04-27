
public class SemanticVisitor extends SysYParserBaseVisitor<Void> {
    Scope curScope;
    GlobalScope globalScope;

    @Override
    public Void visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        curScope = globalScope;
        return visitChildren(ctx);
    }

	@Override public Void visitVarDecl(SysYParser.VarDeclContext ctx) {
        var typeName = ctx.btype().getText();
        if(curScope.resolve(typeName) == null) {
            System.err.println("no type:" + typeName);
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitVarDef(SysYParser.VarDefContext ctx) {
        String varName = ctx.IDENT().getText();
        // TODO: define type
        curScope.define(new BaseSymbol(varName, null));
        return visitChildren(ctx);
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        if (curScope.resolve(funcName) != null) {
            System.err.println("error redefine " + funcName);
        }
        return visitChildren(ctx);
    }
}
