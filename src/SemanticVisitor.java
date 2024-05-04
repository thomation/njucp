import java.util.ArrayList;
import java.util.function.BiConsumer;

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
    public Type visitVarDecl(SysYParser.VarDeclContext ctx) {
        var typeName = ctx.btype().getText();
        Type declType = curScope.find(typeName);
        if (declType == null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_TYPE,
                    ctx.btype().INT().getSymbol().getLine(), typeName);
        }
        visitChildren(ctx);
        return declType;
    }

    @Override
    public Type visitVarDef(SysYParser.VarDefContext ctx) {
        String varName = ctx.IDENT().getText();
        Type defType = curScope.get(varName);
        if (defType != null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.REDEF_VAR, ctx.IDENT().getSymbol().getLine(),
                    varName);
        }
        if (ctx.constExp() == null) {
            defType = new IntType();
            curScope.put(varName, defType);
        } else { // array
            // TODO: d = const exp
            int d = 1;
            // TODO: support multiple array
            defType = new ArrayType(new IntType(), d);
            curScope.put(varName, defType);
        }
        if (ctx.ASSIGN() != null) {
        }
        visitChildren(ctx);
        return defType;
    }

    @Override
    public Type visitExp(SysYParser.ExpContext ctx) {
        Type expType = null;
        if (ctx.IDENT() != null) {
            String fName = ctx.IDENT().getText();
            expType = curScope.find(fName);
            if (expType == null) {
                OutputHelper.getInstance().addSemanticError(SemanticErrorType.UNDEF_FUNC,
                        ctx.IDENT().getSymbol().getLine(), fName);
            }
        }
        if (ctx.PLUS() != null) {
            HandleBinaryOP(ctx, ctx.PLUS().getSymbol());
        }
        expType = visitChildren(ctx);
        return expType;
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
        // TODO: compute value
        Type ret = visitChildren(ctx);
        return ret;
    }

    @Override
    public Type visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.RETURN() != null) {
            if (ctx.exp() != null)
                visit(ctx.exp());
            visit(ctx.SEMICOLON());
            return null;
        }
        if (ctx.ASSIGN() != null) {
            Type lType = visit(ctx.lVal());
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
        return funcType;
    }

    @Override
    public Type visitBlock(SysYParser.BlockContext ctx) {
        Scope localScope = new LocalScope(curScope);
        curScope = localScope;
        // TODO: add param to local scope
        ctx.blockItem().forEach(this::visit);
        curScope = curScope.getEncloseingScope();
        return null;
    }
}
