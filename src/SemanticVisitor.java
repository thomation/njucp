import java.util.ArrayList;

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
        if(curScope.find(typeName) == null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_TYPE, ctx.btype().INT().getSymbol().getLine(), typeName);
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitVarDef(SysYParser.VarDefContext ctx) {
        String varName = ctx.IDENT().getText();
        curScope.put(varName, new IntType());
        return visitChildren(ctx);
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        if (curScope.find(funcName) != null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.REDEF_FUNC, ctx.IDENT().getSymbol().getLine(), funcName);
            return null;
        }
        String typeString = ctx.funcType().getText();
        Type retType = curScope.find(typeString);
        ArrayList<Type> paramsType = new ArrayList<Type>();
        FunctionType funcType = new FunctionType(retType, paramsType);
        curScope.put(funcName, funcType);
        visit(ctx.block());
        return null;
    }
}
