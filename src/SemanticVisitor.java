import java.util.ArrayList;

public class SemanticVisitor extends SysYParserBaseVisitor<Void> {
    Scope curScope;
    GlobalScope globalScope;
    int depth = 0;

    @Override
    public Void visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        curScope = globalScope;
        OutputHelper.getInstance().addSemantic(depth, "program");
        depth++;
        Void ret = visitChildren(ctx);
        depth--;
        return ret;
    }

    @Override
    public Void visitCompUnit(SysYParser.CompUnitContext ctx) {
        OutputHelper.getInstance().addSemantic(depth, "compUnit");
        depth ++;
        Void ret = visitChildren(ctx);
        depth --;
        return ret;
    }

    @Override
    public Void visitVarDecl(SysYParser.VarDeclContext ctx) {
        var typeName = ctx.btype().getText();
        if (curScope.find(typeName) == null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_TYPE,
                    ctx.btype().INT().getSymbol().getLine(), typeName);
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
        OutputHelper.getInstance().addSemantic(depth ++, "FuncDef");
        String funcName = ctx.IDENT().getText();
        if (curScope.find(funcName) != null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.REDEF_FUNC, ctx.IDENT().getSymbol().getLine(),
                    funcName);
            return null;
        }
        String typeString = ctx.funcType().getText();
        OutputHelper.getInstance().addSemantic(depth ++, "FuncType");
        Type retType = curScope.find(typeString);
        OutputHelper.getInstance().addSemantic(depth ++, typeString + " " + retType);
        depth --;
        depth --;
        OutputHelper.getInstance().addSemantic(depth, ctx.IDENT() + " " + "IDENT");
        ArrayList<Type> paramsType = new ArrayList<Type>();
        if (ctx.funcFParams() != null) {
            for (int i = 0; i < ctx.funcFParams().funcFParam().size(); i++) {
                // TODO: put id int scope of function
                String id = ctx.funcFParams().funcFParam(i).IDENT().getText();
                paramsType.add(new IntType());
            }
        }
        FunctionType funcType = new FunctionType(retType, paramsType);
        curScope.put(funcName, funcType);
        visit(ctx.block());
        depth --;
        return null;
    }
}
