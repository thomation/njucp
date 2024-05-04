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
        OutputHelper.getInstance().addSemantic(depth++, "compUnit");
        Void ret = visitChildren(ctx);
        depth--;
        return ret;
    }

    @Override
    public Void visitDecl(SysYParser.DeclContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "Decl");
        Void ret = visitChildren(ctx);
        depth--;
        return ret;
    }

    @Override
    public Void visitVarDecl(SysYParser.VarDeclContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "VarDecl");
        var typeName = ctx.btype().getText();
        OutputHelper.getInstance().addSemantic(depth++, "Btype");
        if (curScope.find(typeName) == null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_TYPE,
                    ctx.btype().INT().getSymbol().getLine(), typeName);
        }
        OutputHelper.getInstance().addSemantic(depth++, typeName + " INT");
        depth--;
        depth--;
        Void ret = visitChildren(ctx);
        depth--;
        return ret;
    }

    @Override
    public Void visitVarDef(SysYParser.VarDefContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "VarDef");
        String varName = ctx.IDENT().getText();
        if (curScope.get(varName) != null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.REDEF_VAR, ctx.IDENT().getSymbol().getLine(),
                    varName);
        }
        OutputHelper.getInstance().addSemantic(depth++, varName + " IDENT");
        depth--;
        curScope.put(varName, new IntType());
        if (ctx.ASSIGN() != null) {
            OutputHelper.getInstance().addSemantic(depth++, ctx.ASSIGN().getText() + " ASSIGN");
            depth--;
        }
        Void ret = visitChildren(ctx);
        depth--;
        return ret;
    }

    @Override
    public Void visitInitVal(SysYParser.InitValContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "InitVal");
        Void ret = visitChildren(ctx);
        depth--;
        return ret;
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "Exp");
        if (ctx.IDENT() != null) {
            String fName = ctx.IDENT().getText();
            if (curScope.find(fName) == null) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_FUNC,
                        ctx.IDENT().getSymbol().getLine(), fName);
            }
        }
        Void ret = visitChildren(ctx);
        depth--;
        return ret;
    }

    @Override
    public Void visitNumber(SysYParser.NumberContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "Number");
        if (ctx.INTEGER_CONST() != null) {
            OutputHelper.getInstance().addSemantic(depth++, ctx.INTEGER_CONST().getText() + " INTEGER_CONST");
            depth--;
        }
        Void ret = visitChildren(ctx);
        depth--;
        return ret;
    }

    @Override
    public Void visitStmt(SysYParser.StmtContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "Stmt");
        if (ctx.RETURN() != null) {
            OutputHelper.getInstance().addSemantic(depth++, ctx.RETURN().getText() + " RETURN");
            depth--;
            if(ctx.exp() != null)
                visit(ctx.exp());
            visit(ctx.SEMICOLON());
            return null;
        }
        if (ctx.ASSIGN() != null) {
            visit(ctx.lVal());
            OutputHelper.getInstance().addSemantic(depth++, ctx.ASSIGN().getText() + " ASSIGN");
            depth--;
            visit(ctx.ASSIGN());
            visit(ctx.exp());
            visit(ctx.SEMICOLON());
            return null;
        }
        Void ret = visitChildren(ctx);
        depth--;
        return ret;
    }

    @Override
    public Void visitLVal(SysYParser.LValContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "LVal");
        String lName = ctx.IDENT().getText();
        OutputHelper.getInstance().addSemantic(depth++, lName + " IDENT");
        depth--;
        if (curScope.find(lName) == null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_VAR,
                    ctx.IDENT().getSymbol().getLine(), lName);
        }
        Void ret = visitChildren(ctx);
        depth--;
        return ret;
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "FuncDef");
        String funcName = ctx.IDENT().getText();
        if (curScope.find(funcName) != null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.REDEF_FUNC, ctx.IDENT().getSymbol().getLine(),
                    funcName);
            return null;
        }
        String typeString = ctx.funcType().getText();
        OutputHelper.getInstance().addSemantic(depth++, "FuncType");
        Type retType = curScope.find(typeString);
        OutputHelper.getInstance().addSemantic(depth++, typeString + " " + retType);
        depth--;
        depth--;
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
        depth--;
        return null;
    }

    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "Block");
        Scope localScope = new LocalScope(curScope);
        curScope = localScope;
        // TODO: add param to local scope
        ctx.blockItem().forEach(this::visit);
        curScope = curScope.getEncloseingScope();
        depth--;
        return null;
    }

    @Override
    public Void visitBlockItem(SysYParser.BlockItemContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "BlockItem");
        Void ret = visitChildren(ctx);
        depth--;
        return ret;
    }
}
