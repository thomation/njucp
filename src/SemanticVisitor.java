import org.antlr.v4.runtime.Token;

public class SemanticVisitor extends SysYParserBaseVisitor<Type> {
    Scope curScope;
    GlobalScope globalScope;

    @Override
    public Type visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        curScope = globalScope;
        return visitChildren(ctx);
    }

    @Override
    public Type visitBtype(SysYParser.BtypeContext ctx) {
        var typeName = ctx.getText();
        Type type = curScope.find(typeName);
        if (type == null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_TYPE,
                    ctx.INT().getSymbol().getLine(), typeName);
            return null;
        }
        return type;
    }

    @Override
    public Type visitConstDef(SysYParser.ConstDefContext ctx) {
        String varName = ctx.IDENT().getText();
        if (checkVarRedefine(varName, ctx.IDENT().getSymbol().getLine()))
            return null;
        if (ctx.L_BRACKT() == null || ctx.L_BRACKT().size() == 0) {
            IntType defType = new IntType();
            curScope.put(varName, defType);
            return defType;
        } else {
            // TODO: handle array
        }
        
        return visitChildren(ctx);
    }

    boolean checkVarRedefine(String varName, int line) {
        Type defType = curScope.get(varName);
        if (defType != null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.REDEF_VAR, line, varName);
            return true;
        }
        return false;
    }

    @Override
    public Type visitVarDef(SysYParser.VarDefContext ctx) {
        Type defType = null;
        String varName = ctx.IDENT().getText();
        if (checkVarRedefine(varName, ctx.IDENT().getSymbol().getLine()))
            return null;

        if (ctx.L_BRACKT() == null || ctx.L_BRACKT().size() == 0) {
            defType = new IntType();
            curScope.put(varName, defType);
        } else { // array
            for (int i = ctx.L_BRACKT().size() - 1; i >= 0; i--) {
                visit(ctx.L_BRACKT(i));
                if (ctx.constExp(i).exp().number() == null) {
                    OutputHelper.getInstance().addSemanticError(SemanticErrorType.ARRAY_SIZE_CONST,
                            ctx.L_BRACKT(i).getSymbol().getLine(), ctx.constExp(i).exp().getText());
                    return null;
                }
                int d = Integer.parseInt(ctx.constExp(i).exp().number().getText());
                visit(ctx.constExp(i));
                if (defType == null)
                    defType = new ArrayType(new IntType(), d);
                else
                    defType = new ArrayType(defType, d);
                visit(ctx.R_BRACKT(i));
            }
            curScope.put(varName, defType);
            // System.out.printf("def array:%s, %s\n", varName, defType);
        }

        visitChildren(ctx);
        return defType;
    }

    @Override
    public Type visitExp(SysYParser.ExpContext ctx) {
        if (ctx.IDENT() != null) { // function
            String fName = ctx.IDENT().getText();
            Type type = curScope.find(fName);
            if (type == null) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_FUNC,
                        ctx.IDENT().getSymbol().getLine(), fName);
            } else if (!(type instanceof FunctionType)) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.NOT_FUNC,
                        ctx.IDENT().getSymbol().getLine(), fName);
            } else {
                FunctionType functionType = (FunctionType) type;
                boolean match = true;
                if (ctx.funcRParams() == null && functionType.getParamsType() == null) {
                    match = true;
                } else if (ctx.funcRParams() != null && functionType.getParamsType() == null ||
                        ctx.funcRParams() == null && functionType.getParamsType() != null) {
                    match = false;
                } else if (ctx.funcRParams().param().size() != functionType.getParamsType().size()) {
                    match = false;
                } else {
                    for (int i = 0; i < ctx.funcRParams().param().size(); i++) {
                        Type argType = visit(ctx.funcRParams().param(i));
                        Type paramType = functionType.getParamsType().get(i);
                        System.out.printf("exp:%s, arguments %s, parameters %s\n", ctx.getText(), argType, paramType);
                        if (argType.getClass() != paramType.getClass()) {
                            match = false;
                        }
                    }
                }
                if (!match) {
                    OutputHelper.getInstance().addSemanticError(SemanticErrorType.FUNC_PARAM,
                            ctx.IDENT().getSymbol().getLine(), fName);
                }
                visitChildren(ctx);
                return functionType.getRetType();
            }

        }
        if (ctx.PLUS() != null) {
            HandleBinaryOP(ctx, ctx.PLUS().getSymbol());
        }
        return visitChildren(ctx);
    }

    Type HandleBinaryOP(SysYParser.ExpContext ctx, Token symbol) {
        Type lType = visit(ctx.exp(0));
        Type rType = visit(ctx.exp(1));
        if (lType.getClass() != rType.getClass()) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.MISMATCH_OPERANDS, symbol.getLine(),
                    ctx.exp(0).getText() + " = " + ctx.exp(1).getText());
            return null;
        }
        switch (symbol.getText()) {
            // TODO: compute value
            // case "+":
            // return lType + rType;
            default:
                break;
        }
        return lType;
    }

    @Override
    public Type visitNumber(SysYParser.NumberContext ctx) {
        return new IntType();
    }

    private FunctionType findEncloseFuncType() {
        Scope temp = curScope;
        while (temp != globalScope) {
            if (temp instanceof FunctionType) {
                return (FunctionType) temp;
            }
            temp = temp.getEncloseingScope();
        }
        return null;
    }

    @Override
    public Type visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.RETURN() != null) {
            if (ctx.exp() != null) {
                Type retType = visit(ctx.exp());
                FunctionType funcType = findEncloseFuncType();
                if (funcType == null || retType == null || retType.getClass() != funcType.getRetType().getClass()) {
                    OutputHelper.getInstance().addSemanticError(SemanticErrorType.MISMATCH_RETURN,
                            ctx.RETURN().getSymbol().getLine(),
                            funcType.getRetType() + " != " + retType);
                }
                return retType;
            }
            visit(ctx.SEMICOLON());
            return null;
        }
        if (ctx.ASSIGN() != null) {
            Type lType = visit(ctx.lVal());
            if (lType instanceof FunctionType) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.LEFT_VAR,
                        ctx.ASSIGN().getSymbol().getLine(),
                        String.format("%s", lType.getClass()));

            }
            visit(ctx.ASSIGN());
            Type rType = visit(ctx.exp());
            if (lType != null && rType != null) {
                if (lType.getClass() != rType.getClass()) {
                    OutputHelper.getInstance().addSemanticError(SemanticErrorType.MISMATCH_ASSIGN,
                            ctx.ASSIGN().getSymbol().getLine(),
                            String.format("%s != %s", lType.getClass(), rType.getClass()));
                }
            }
            visit(ctx.SEMICOLON());
            return null;
        }
        Type ret = visitChildren(ctx);
        return ret;
    }

    @Override
    public Type visitLVal(SysYParser.LValContext ctx) {
        String lName = ctx.IDENT().getText();
        Type valType = curScope.find(lName);
        if (valType == null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_VAR,
                    ctx.IDENT().getSymbol().getLine(), lName);
        }
        if (ctx.L_BRACKT() != null && ctx.L_BRACKT().size() > 0) {
            if (!(valType instanceof ArrayType)) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.NOT_ARRAY,
                        ctx.IDENT().getSymbol().getLine(), lName);
                return null;
            }
            Type curType = valType;
            for (int i = 0; i < ctx.L_BRACKT().size(); i++) {
                ArrayType arrayType = (ArrayType) curType;
                curType = arrayType.getContainedType();
                visit(ctx.L_BRACKT(i));
                visit(ctx.exp(i));
                visit(ctx.R_BRACKT(i));
            }
            return curType;
        }
        visitChildren(ctx);
        return valType;
    }

    @Override
    public Type visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        if (curScope.find(funcName) != null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.REDEF_FUNC, ctx.IDENT().getSymbol().getLine(),
                    funcName);
            return null;
        }
        String typeString = ctx.funcType().getText();
        FunctionType funcType = new FunctionType(funcName, curScope);
        curScope.put(funcName, funcType);
        Type retType = curScope.find(typeString);
        funcType.setRetType(retType);
        if (ctx.funcFParams() != null) {
            for (int i = 0; i < ctx.funcFParams().funcFParam().size(); i++) {
                String id = ctx.funcFParams().funcFParam(i).IDENT().getText();
                Type paramType = visit(ctx.funcFParams().funcFParam(i));
                funcType.addParamType(id, paramType);
            }
        }
        // Visit block items to forbid create new scope in block.
        curScope = funcType;
        for (int i = 0; i < ctx.block().blockItem().size(); i++)
            visit(ctx.block().blockItem(i));
        return funcType;
    }

    @Override
    public Type visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        if (ctx.L_BRACKT() == null || ctx.L_BRACKT().size() == 0) {
            return visit(ctx.btype());
        } else {
            // TODO: handle array param
        }
        return visitChildren(ctx);
    }

    @Override
    public Type visitBlock(SysYParser.BlockContext ctx) {
        Scope localScope = new LocalScope(curScope);
        curScope = localScope;
        ctx.blockItem().forEach(this::visit);
        curScope = curScope.getEncloseingScope();
        return null;
    }
}
