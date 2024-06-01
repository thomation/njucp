import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

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
        Type defType = null;
        if (checkVarRedefine(varName, ctx.IDENT().getSymbol().getLine()))
            return null;
        if (ctx.L_BRACKT() == null || ctx.L_BRACKT().size() == 0) {
            defType = new IntType();
            curScope.put(varName, defType);
        } else {
            defType = createArray(ctx.L_BRACKT(), ctx.constExp(), ctx.R_BRACKT());
            if (defType == null)
                return null;
            curScope.put(varName, defType);
            // System.out.printf("def const array:%s, %s\n", varName, defType);
        }

        return defType;
    }

    boolean checkVarRedefine(String varName, int line) {
        Type defType = curScope.get(varName);
        if (defType != null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.REDEF_VAR, line, varName);
            return true;
        }
        return false;
    }

    ArrayType createArray(List<TerminalNode> lb, List<SysYParser.ConstExpContext> constExp, List<TerminalNode> rb) {
        List<SysYParser.ExpContext> exp = new ArrayList<SysYParser.ExpContext>();
        for (int i = 0; i < constExp.size(); i++)
            exp.add(constExp.get(i).exp());
        return createArray2(lb, exp, rb);
    }

    ArrayType createArray2(List<TerminalNode> lb, List<SysYParser.ExpContext> exp, List<TerminalNode> rb) {
        ArrayType arrayType = null;
        int offset = lb.size() - exp.size();
        for (int i = lb.size() - 1; i >= 0; i--) {
            int expIndex = i - offset;
            int arraySize = 0;
            visit(lb.get(i));
            if (expIndex >= 0) {
                if (exp.get(expIndex).number() == null) {
                    OutputHelper.getInstance().addSemanticError(SemanticErrorType.ARRAY_SIZE_CONST,
                            lb.get(i).getSymbol().getLine(), exp.get(expIndex).getText());
                    return null;
                }
                arraySize = Integer.parseInt(exp.get(expIndex).number().getText());
                visit(exp.get(expIndex));
            }
            if (arrayType == null)
                arrayType = new ArrayType(new IntType(), arraySize);
            else
                arrayType = new ArrayType(arrayType, arraySize);
            visit(rb.get(i));
        }
        return arrayType;
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
            defType = createArray(ctx.L_BRACKT(), ctx.constExp(), ctx.R_BRACKT());
            if (defType == null)
                return null;
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
                        // System.out.printf("exp:%s, arguments %s, parameters %s\n", ctx.getText(), argType, paramType);
                        if (!isTypeMatched(paramType, argType))
                            match = false;
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

    boolean isTypeMatched(Type lType, Type rType) {
        if (lType == null || rType == null) {
            return false;
        }
        return lType.isMatched(rType);
    }

    Type HandleBinaryOP(SysYParser.ExpContext ctx, Token symbol) {
        Type lType = visit(ctx.exp(0));
        Type rType = visit(ctx.exp(1));
        if (!isTypeMatched(lType, rType)) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.MISMATCH_OPERANDS, symbol.getLine(),
                    ctx.exp(0).getText() + " = " + ctx.exp(1).getText());
            return null;
        }
        switch (symbol.getText()) {
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
                if (!isTypeMatched(funcType.getRetType(), retType)) {
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
                return null;
            }
            visit(ctx.ASSIGN());
            Type rType = visit(ctx.exp());
            if (!isTypeMatched(lType, lType)) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.MISMATCH_ASSIGN,
                        ctx.ASSIGN().getSymbol().getLine(),
                        String.format("%s != %s", lType.getClass(), rType.getClass()));
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
            return null;
        }
        if (ctx.L_BRACKT() != null && ctx.L_BRACKT().size() > 0) {
            if (!(valType instanceof ArrayType)) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.NOT_ARRAY,
                        ctx.IDENT().getSymbol().getLine(), lName);
                return null;
            }
            Type curType = valType;
            // System.out.printf("vistval: %s, %s\n", ctx.getText(), valType);
            for (int i = 0; i < ctx.L_BRACKT().size(); i++) {
                if (!(curType instanceof ArrayType)) {
                    OutputHelper.getInstance().addSemanticError(SemanticErrorType.ARRAY_DIMENSION,
                            ctx.IDENT().getSymbol().getLine(), lName);
                    return null;
                }
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
        Type retType = curScope.find(typeString);
        FunctionType funcType = new FunctionType(funcName, retType, curScope);
        curScope.put(funcName, funcType);
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
        Type paramType = null;
        if (ctx.L_BRACKT() == null || ctx.L_BRACKT().size() == 0) {
            paramType = visit(ctx.btype());
        } else {
            paramType = createArray2(ctx.L_BRACKT(), ctx.exp(), ctx.R_BRACKT());
            // System.out.printf("def func param array %s is %s\n", ctx.getText(), paramType);
        }
        return paramType;
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
