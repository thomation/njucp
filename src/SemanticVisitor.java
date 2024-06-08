import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class SemanticVisitor extends SysYParserBaseVisitor<Symbol> {
    Scope curScope;
    GlobalScope globalScope;

    @Override
    public Symbol visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        curScope = globalScope;
        return visitChildren(ctx);
    }

    @Override
    public Symbol visitBtype(SysYParser.BtypeContext ctx) {
        var typeName = ctx.getText();
        Symbol type = curScope.find(typeName);
        if (type == null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_TYPE,
                    ctx.INT().getSymbol().getLine(), typeName);
            return null;
        }
        return type;
    }

    @Override
    public Symbol visitConstDef(SysYParser.ConstDefContext ctx) {
        String varName = ctx.IDENT().getText();
        Symbol defSymbol = null;
        if (checkVarRedefine(varName, ctx.IDENT().getSymbol().getLine()))
            return null;
        if (ctx.L_BRACKT() == null || ctx.L_BRACKT().size() == 0) {
            defSymbol = new BasicSymbol(varName, new BasicTypeSymbol("int"));
            curScope.put(defSymbol);
        } else {
            defSymbol = createArray(varName, ctx.L_BRACKT(), ctx.constExp(), ctx.R_BRACKT());
            if (defSymbol == null)
                return null;
            curScope.put(defSymbol);
            // System.out.printf("def const array:%s, %s\n", varName, defType);
        }

        return defSymbol;
    }

    boolean checkVarRedefine(String varName, int line) {
        Symbol defType = curScope.get(varName);
        if (defType != null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.REDEF_VAR, line, varName);
            return true;
        }
        return false;
    }

    ArraySymbol createArray(String arrayName, List<TerminalNode> lb, List<SysYParser.ConstExpContext> constExp,
            List<TerminalNode> rb) {
        List<SysYParser.ExpContext> exp = new ArrayList<SysYParser.ExpContext>();
        for (int i = 0; i < constExp.size(); i++)
            exp.add(constExp.get(i).exp());
        return createArray2(arrayName, lb, exp, rb);
    }

    ArraySymbol createArray2(String arrayName, List<TerminalNode> lb, List<SysYParser.ExpContext> exp,
            List<TerminalNode> rb) {
        ArraySymbol arrayType = null;
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
                arrayType = new ArraySymbol(arrayName, new BasicTypeSymbol("int"), arraySize);
            else
                arrayType = new ArraySymbol(arrayName, arrayType, arraySize);
            visit(rb.get(i));
        }
        return arrayType;
    }

    @Override
    public Symbol visitVarDef(SysYParser.VarDefContext ctx) {
        Symbol defSymbol = null;
        String varName = ctx.IDENT().getText();
        if (checkVarRedefine(varName, ctx.IDENT().getSymbol().getLine()))
            return null;

        if (ctx.L_BRACKT() == null || ctx.L_BRACKT().size() == 0) {
            defSymbol = new BasicSymbol(varName, new BasicTypeSymbol("int"));
            curScope.put(defSymbol);
        } else { // array
            defSymbol = createArray(varName, ctx.L_BRACKT(), ctx.constExp(), ctx.R_BRACKT());
            if (defSymbol == null)
                return null;
            curScope.put(defSymbol);
            // System.out.printf("def array:%s, %s\n", varName, defType);
        }

        visitChildren(ctx);
        return defSymbol;
    }

    @Override
    public Symbol visitExp(SysYParser.ExpContext ctx) {
        if (ctx.IDENT() != null) { // function
            String fName = ctx.IDENT().getText();
            Symbol type = curScope.find(fName);
            if (type == null) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_FUNC,
                        ctx.IDENT().getSymbol().getLine(), fName);
            } else if (!(type instanceof FunctionSymbol)) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.NOT_FUNC,
                        ctx.IDENT().getSymbol().getLine(), fName);
            } else {
                FunctionSymbol functionSymbol = (FunctionSymbol) type;
                boolean match = true;
                if (ctx.funcRParams() == null && functionSymbol.getParamsSymbol() == null) {
                    match = true;
                } else if (ctx.funcRParams() != null && functionSymbol.getParamsSymbol() == null ||
                        ctx.funcRParams() == null && functionSymbol.getParamsSymbol() != null) {
                    match = false;
                } else if (ctx.funcRParams().param().size() != functionSymbol.getParamsSymbol().size()) {
                    match = false;
                } else {
                    for (int i = 0; i < ctx.funcRParams().param().size(); i++) {
                        Type argType = visit(ctx.funcRParams().param(i)).getType();
                        Type paramType = functionSymbol.getParamsSymbol().get(i).getType();
                        // System.out.printf("exp:%s, arguments %s, parameters %s\n", ctx.getText(),
                        // argType, paramType);
                        if (!isTypeMatched(paramType, argType))
                            match = false;
                    }
                }
                if (!match) {
                    OutputHelper.getInstance().addSemanticError(SemanticErrorType.FUNC_PARAM,
                            ctx.IDENT().getSymbol().getLine(), fName);
                }
                visitChildren(ctx);
                return functionSymbol;
            }

        }
        if (ctx.lVal() != null) {

        }
        if (ctx.PLUS() != null) {
            HandleBinaryOP(ctx, ctx.PLUS().getSymbol());
        }
        Symbol ret = visitChildren(ctx);
        // assert ret != null : "No exp matched";
        return ret;
    }

    boolean isTypeMatched(Type lType, Type rType) {
        if (lType == null || rType == null) {
            return false;
        }
        return lType.isMatched(rType);
    }

    Symbol HandleBinaryOP(SysYParser.ExpContext ctx, Token symbol) {
        Symbol lType = visit(ctx.exp(0));
        Symbol rType = visit(ctx.exp(1));
        if (!isTypeMatched(lType.getType(), rType.getType())) {
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
    public Symbol visitNumber(SysYParser.NumberContext ctx) {
        return new BasicTypeSymbol("int");
    }

    private FunctionSymbol findEncloseFuncType() {
        Scope temp = curScope;
        while (temp != globalScope) {
            if (temp instanceof FunctionSymbol) {
                return (FunctionSymbol) temp;
            }
            temp = temp.getEncloseingScope();
        }
        return null;
    }

    @Override
    public Symbol visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.RETURN() != null) {
            if (ctx.exp() != null) {
                Symbol retType = visit(ctx.exp());
                FunctionSymbol funcType = findEncloseFuncType();
                // System.out.printf("func:%s, ret:%s\n", funcType, retType);
                if (funcType == null || retType == null || !isTypeMatched(funcType.getRetType(), retType.getType())) {
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
            Symbol lType = visit(ctx.lVal());
            if (lType instanceof FunctionSymbol) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.LEFT_VAR,
                        ctx.ASSIGN().getSymbol().getLine(),
                        String.format("%s", lType.getClass()));
                return null;
            }
            visit(ctx.ASSIGN());
            Symbol rType = visit(ctx.exp());
            if (!isTypeMatched((Type) lType, (Type) rType)) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.MISMATCH_ASSIGN,
                        ctx.ASSIGN().getSymbol().getLine(),
                        String.format("%s != %s", lType.getClass(), rType.getClass()));
            }

            visit(ctx.SEMICOLON());
            return null;
        }

        Symbol ret = visitChildren(ctx);
        return ret;
    }

    @Override
    public Symbol visitLVal(SysYParser.LValContext ctx) {
        String lName = ctx.IDENT().getText();
        Symbol valSymbol = curScope.find(lName);
        if (valSymbol == null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_VAR,
                    ctx.IDENT().getSymbol().getLine(), lName);
            return null;
        }
        if (ctx.L_BRACKT() != null && ctx.L_BRACKT().size() > 0) {
            if (!(valSymbol instanceof ArraySymbol)) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.NOT_ARRAY,
                        ctx.IDENT().getSymbol().getLine(), lName);
                return null;
            }
            Type curType = (ArraySymbol) valSymbol;
            // System.out.printf("vistval: %s, %s\n", ctx.getText(), valType);
            for (int i = 0; i < ctx.L_BRACKT().size(); i++) {
                if (!(curType instanceof ArraySymbol)) {
                    OutputHelper.getInstance().addSemanticError(SemanticErrorType.ARRAY_DIMENSION,
                            ctx.IDENT().getSymbol().getLine(), lName);
                    return null;
                }
                ArraySymbol arrayType = (ArraySymbol) curType;
                curType = arrayType.getContainedType();
                visit(ctx.L_BRACKT(i));
                visit(ctx.exp(i));
                visit(ctx.R_BRACKT(i));
            }
            return (ArraySymbol) curType;
        }
        visitChildren(ctx);
        assert valSymbol != null : "visitlVal null";
        return valSymbol;
    }

    @Override
    public Symbol visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        if (curScope.find(funcName) != null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.REDEF_FUNC, ctx.IDENT().getSymbol().getLine(),
                    funcName);
            return null;
        }
        String typeString = ctx.funcType().getText();
        Symbol retSymbol = curScope.find(typeString);
        if (!(retSymbol instanceof Type)) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_TYPE, ctx.IDENT().getSymbol().getLine(),
                    funcName);
            return null;
        }
        Type retType = (Type) retSymbol;
        FunctionSymbol funcType = new FunctionSymbol(funcName, retType, curScope);
        curScope.put(funcType);
        if (ctx.funcFParams() != null) {
            for (int i = 0; i < ctx.funcFParams().funcFParam().size(); i++) {
                String id = ctx.funcFParams().funcFParam(i).IDENT().getText();
                Symbol paramType = visit(ctx.funcFParams().funcFParam(i));
                assert paramType instanceof BasicTypeSymbol : "array not support";
                if (paramType instanceof BasicTypeSymbol) {
                    funcType.addParamSymbol(new BasicSymbol(id, paramType.getType()));
                }
            }
        }
        // Visit block items to forbid create new scope in block.
        curScope = funcType;
        for (int i = 0; i < ctx.block().blockItem().size(); i++)
            visit(ctx.block().blockItem(i));
        return funcType;
    }

    @Override
    public Symbol visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        Symbol paramType = null;
        if (ctx.L_BRACKT() == null || ctx.L_BRACKT().size() == 0) {
            paramType = visit(ctx.btype());
        } else {
            paramType = createArray2(ctx.getText(), ctx.L_BRACKT(), ctx.exp(), ctx.R_BRACKT());
            // System.out.printf("def func param array %s is %s\n", ctx.getText(),
            // paramType);
        }
        return paramType;
    }

    @Override
    public Symbol visitBlock(SysYParser.BlockContext ctx) {
        Scope localScope = new LocalScope(curScope);
        curScope = localScope;
        ctx.blockItem().forEach(this::visit);
        curScope = curScope.getEncloseingScope();
        return null;
    }
}
