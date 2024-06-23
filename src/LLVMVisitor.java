import org.bytedeco.llvm.LLVM.*;

import static org.bytedeco.llvm.global.LLVM.*;

import org.antlr.v4.runtime.Token;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;

public class LLVMVisitor extends SysYParserBaseVisitor<Symbol> {
    Scope curScope;
    GlobalScope globalScope;
    LLVMModuleRef module;
    LLVMBuilderRef builder;
    LLVMTypeRef i32Type;

    public LLVMVisitor() {
        super();
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();

    }

    @Override
    public Symbol visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        curScope = globalScope;
        module = LLVMModuleCreateWithName("module");
        builder = LLVMCreateBuilder();
        i32Type = LLVMInt32Type();

        Symbol ret = visitChildren(ctx);
        LLVMDumpModule(module);
        return ret;
    }

    @Override
    public Symbol visitVarDef(SysYParser.VarDefContext ctx) {
        Symbol defSymbol = null;
        String varName = ctx.IDENT().getText();
        defSymbol = new BasicSymbol(varName, new BasicTypeSymbol("int"));
        curScope.put(defSymbol);
        Symbol varValue = visitChildren(ctx);
        // System.out.printf("visitVarDef ctx:%s varValue:%s\n", ctx.getText(),
        // varValue);
        if (curScope == globalScope) {
            LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, varName);
            LLVMSetInitializer(globalVar, varValue.getValue());
            defSymbol.setValue(globalVar);
        } else {
            LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /* pointerName:String */varName);
            LLVMBuildStore(builder, varValue.getValue(), pointer);
            defSymbol.setValue(pointer);
        }
        return defSymbol;
    }

    @Override
    public Symbol visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        String typeString = ctx.funcType().getText();
        Symbol retType = curScope.find(typeString);
        FunctionSymbol funcType = new FunctionSymbol(funcName, (Type) retType, curScope);
        curScope.put(funcType);
        int paramCount = ctx.funcFParams() != null ? ctx.funcFParams().funcFParam().size() : 0;
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(paramCount);
        if (ctx.funcFParams() != null) {
            for (int i = 0; i < ctx.funcFParams().funcFParam().size(); i++) {
                String id = ctx.funcFParams().funcFParam(i).IDENT().getText();
                Symbol paramType = visit(ctx.funcFParams().funcFParam(i).btype());
                // System.out.printf("paramType:%s\n", paramType);
                Symbol param = new BasicSymbol(id, (Type) paramType);
                funcType.addParamSymbol(param);
                argumentTypes.put(i, i32Type);
            }
        }
        LLVMTypeRef returnType = i32Type;
        LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ paramCount,
                /* isVariadic */ 0);
        LLVMValueRef function = LLVMAddFunction(module, funcName, ft);
        // Visit block items to forbid create new scope in block.
        curScope = funcType;
        funcType.setValue(function);
        funcType.setLLVMType(ft);
        if (ctx.block() != null) {
            LLVMBasicBlockRef block1 = LLVMAppendBasicBlock(function, funcName + "Entry");
            LLVMPositionBuilderAtEnd(builder, block1);
            if (ctx.funcFParams() != null) {
                for (int i = 0; i < ctx.funcFParams().funcFParam().size(); i++) {
                    String id = ctx.funcFParams().funcFParam(i).IDENT().getText();
                    Symbol paramSymbol = curScope.find(id);
                    // System.out.printf("param:%s\n", paramSymbol);
                    LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /* pointerName:String */id);
                    // System.out.printf("point:%s\n", pointer);
                    LLVMValueRef paramValue = LLVMGetParam(function, /* parameterIndex */i);
                    // System.out.printf("param %d value:%s\n", i, paramValue);
                    LLVMBuildStore(builder, paramValue, pointer);
                    paramSymbol.setValue(pointer);
                }
            }
            for (int i = 0; i < ctx.block().blockItem().size(); i++) {
                visit(ctx.block().blockItem(i));
            }
        }
        return funcType;
    }

    @Override
    public Symbol visitStmt(SysYParser.StmtContext ctx) {
        System.out.printf("visitStmt:%s\n", ctx.getText());
        if (ctx.RETURN() != null) {
            if (ctx.exp() != null) {
                Symbol result = visit(ctx.exp());
                LLVMBuildRet(builder, /* result:LLVMValueRef */result.getValue());
                return result;
            }
            visit(ctx.SEMICOLON());
            LLVMBuildRet(builder, null);
            return null;
        }
        if (ctx.ASSIGN() != null) {
            Symbol symbol = curScope.find(ctx.lVal().IDENT().getText());
            LLVMValueRef value = LLVMBuildLoad(builder, symbol.getValue(), ctx.lVal().IDENT().getText());
            Symbol r = visit(ctx.exp());
            LLVMBuildStore(builder, r.getValue(), value);
            return null;
        }
        if (ctx.IF() != null) {
            Symbol condSymbol = visit(ctx.cond());
            FunctionSymbol functionSymbol = getEnclosedFunction();
            // System.out.println("get function:" + functionSymbol.getValue());
            LLVMBasicBlockRef exit = LLVMAppendBasicBlock(functionSymbol.getValue(), "true");
            LLVMBasicBlockRef ifFalse = LLVMAppendBasicBlock(functionSymbol.getValue(), "false");
            LLVMBasicBlockRef entry = LLVMAppendBasicBlock(functionSymbol.getValue(), "entry");
            LLVMBuildCondBr(builder, condSymbol.getValue(), exit, ifFalse);
            // if block
            LLVMPositionBuilderAtEnd(builder, exit);
            buildBlock(ctx.stmt(0));
            LLVMBuildBr(builder, entry);
            // else block
            LLVMPositionBuilderAtEnd(builder, ifFalse);
            if (ctx.ELSE() != null) {
                buildBlock(ctx.stmt(1));
            }
            LLVMBuildBr(builder, entry);

            LLVMPositionBuilderAtEnd(builder, entry);
            return null;
        }
        if (ctx.WHILE() != null) {
            FunctionSymbol functionSymbol = getEnclosedFunction();
            LLVMBasicBlockRef conditon = LLVMAppendBasicBlock(functionSymbol.getValue(), "whileCondition");
            LLVMBasicBlockRef body = LLVMAppendBasicBlock(functionSymbol.getValue(), "whileBody");
            LLVMBasicBlockRef entry = LLVMAppendBasicBlock(functionSymbol.getValue(), "entry");
            LLVMPositionBuilderAtEnd(builder, conditon);
            Symbol condSymbol = visit(ctx.cond());
            LLVMBuildCondBr(builder, condSymbol.getValue(), body, entry);
            LLVMPositionBuilderAtEnd(builder, body);
            buildBlock(ctx.stmt(0));
            LLVMBuildBr(builder, conditon);
            LLVMPositionBuilderAtEnd(builder, entry);
            return null;
        }
        return visitChildren(ctx);
    }

    FunctionSymbol getEnclosedFunction() {
        Scope scope = curScope;
        while (!(scope instanceof FunctionSymbol)) {
            scope = curScope.getEncloseingScope();
        }
        assert scope != null && scope instanceof FunctionSymbol : "no func scop";
        return (FunctionSymbol) scope;
    }

    void buildBlock(SysYParser.StmtContext stmt) {
        if (stmt.block() != null) {
            for (int i = 0; i < stmt.block().blockItem().size(); i++)
                visit(stmt.block().blockItem(i));
        }
    }

    @Override
    public Symbol visitExp(SysYParser.ExpContext ctx) {
        // System.out.println("exp:" + ctx.getText());
        if (ctx.IDENT() != null) { // function
            // visitChildren(ctx);
            String fName = ctx.IDENT().getText();
            Symbol type = curScope.find(fName);
            FunctionSymbol functionSymbol = (FunctionSymbol) type;
            int numArgs = ctx.funcRParams() != null ? ctx.funcRParams().param().size() : 0;
            PointerPointer<LLVMValueRef> params = new PointerPointer<LLVMValueRef>(numArgs);
            for (int i = 0; i < ctx.funcRParams().param().size(); i++) {
                Symbol argSymbol = visit(ctx.funcRParams().param(i));
                params.put(i, argSymbol.getValue());
            }
            LLVMValueRef ret = LLVMBuildCall2(builder, functionSymbol.getLLVMType(), functionSymbol.getValue(), params,
                    numArgs, "returnValue");
            Symbol callSymbol = new BasicSymbol(functionSymbol.getName() + "_return", functionSymbol.getType());
            callSymbol.setValue(ret);
            return callSymbol;
        }
        if (ctx.lVal() != null) {
            Symbol symbol = curScope.find(ctx.lVal().IDENT().getText());
            LLVMValueRef value = LLVMBuildLoad(builder, symbol.getValue(), ctx.lVal().IDENT().getText());
            Symbol symbol2 = new BasicSymbol(ctx.lVal().IDENT().getText(), symbol.getType());
            symbol2.setValue(value);
            return symbol2;
        }
        if (ctx.number() != null) {
            String numString = ctx.number().INTEGER_CONST().getText();
            int num = Integer.parseInt(numString);
            LLVMValueRef value = LLVMConstInt(i32Type, num, /* signExtend */ 0);
            Symbol symbol = new BasicSymbol(numString, new BasicTypeSymbol("int"));
            symbol.setValue(value);
            return symbol;
        }
        if (ctx.unaryOp() != null) {
            return HandleUnaryOP(ctx, ctx.unaryOp());
        }
        if (ctx.PLUS() != null) {
            return HandleBinaryOP(ctx, ctx.PLUS().getSymbol());
        }
        if (ctx.MUL() != null) {
            return HandleBinaryOP(ctx, ctx.MUL().getSymbol());
        }
        if (ctx.MINUS() != null) {
            return HandleBinaryOP(ctx, ctx.MINUS().getSymbol());
        }
        Symbol ret = visitChildren(ctx);
        assert ret != null : "exp error:" + ctx.getText();
        return ret;
    }

    Symbol HandleUnaryOP(SysYParser.ExpContext ctx, SysYParser.UnaryOpContext unary) {
        // System.out.printf("uop: %s\n", unary.getText());
        Symbol expSymbol = visit(ctx.exp(0));
        if (unary.MINUS() != null) {
            LLVMValueRef valueRef = LLVMBuildNeg(builder, expSymbol.getValue(), "tmp_");
            Symbol result = new BasicSymbol("tmp", new BasicTypeSymbol("int"));
            result.setValue(valueRef);
            return result;
        }
        return null;
    }

    Symbol HandleBinaryOP(SysYParser.ExpContext ctx, Token symbol) {
        // System.out.printf("bop: %s\n", symbol.getText());
        LLVMValueRef lv = visit(ctx.exp(0)).getValue();
        LLVMValueRef rv = visit(ctx.exp(1)).getValue();
        LLVMValueRef valueRef = null;
        switch (symbol.getText()) {
            case "+":
                valueRef = LLVMBuildAdd(builder, lv, rv, "tmp_");
                break;
            case "-":
                valueRef = LLVMBuildSub(builder, lv, rv, "tmp_");
                break;
            case "*":
                valueRef = LLVMBuildMul(builder, lv, rv, "tmp_");
                break;
            case "/":
                valueRef = LLVMBuildUDiv(builder, lv, rv, "tmp_");
                break;
            default:
                break;
        }
        assert valueRef != null : "error! operator is not handled:" + ctx.getText();
        Symbol result = new BasicSymbol(valueRef.toString(), new BasicTypeSymbol("int"));
        result.setValue(valueRef);
        return result;
    }

    @Override
    public Symbol visitCond(SysYParser.CondContext ctx) {
        if (ctx.exp() != null) {
            return visit(ctx.exp());
        }
        Symbol lc = visit(ctx.cond(0));
        Symbol rc = visit(ctx.cond(1));
        Symbol condSymbol = new BasicSymbol("tmp_", null);
        if (ctx.NEQ() != null) {
            condSymbol.setValue(LLVMBuildICmp(builder, LLVMIntNE, lc.getValue(), rc.getValue(), "tmp_"));
        }
        if (ctx.LT() != null) {
            condSymbol.setValue(LLVMBuildICmp(builder, LLVMIntSLT, lc.getValue(), rc.getValue(), "tmp_"));
        }
        if (ctx.LE() != null) {
            condSymbol.setValue(LLVMBuildICmp(builder, LLVMIntSLE, lc.getValue(), rc.getValue(), "tmp_"));
        }
        return condSymbol;
    }
}
