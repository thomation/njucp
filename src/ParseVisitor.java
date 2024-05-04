public class ParseVisitor extends SysYParserBaseVisitor<Void> {
    int depth = 0;

    @Override
    public Void visitProgram(SysYParser.ProgramContext ctx) {
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
        OutputHelper.getInstance().addSemantic(depth++, varName + " IDENT");
        depth--;
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
            if (ctx.exp() != null)
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
        Void ret = visitChildren(ctx);
        depth--;
        return ret;
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "FuncDef");
        String typeString = ctx.funcType().getText();
        OutputHelper.getInstance().addSemantic(depth++, "FuncType");
        OutputHelper.getInstance().addSemantic(depth++, typeString);
        depth--;
        depth--;
        OutputHelper.getInstance().addSemantic(depth, ctx.IDENT() + " " + "IDENT");
        visit(ctx.block());
        depth--;
        return null;
    }

    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        OutputHelper.getInstance().addSemantic(depth++, "Block");
        ctx.blockItem().forEach(this::visit);
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
