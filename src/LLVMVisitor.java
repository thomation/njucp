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
        // System.out.printf("visitVarDef ctx:%s varValue:%s\n", ctx.getText(), varValue);
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
        if (curScope.find(funcName) != null) {
            OutputHelper.getInstance().addSemanticError(SemanticErrorType.REDEF_FUNC, ctx.IDENT().getSymbol().getLine(),
                    funcName);
            return null;
        }
        String typeString = ctx.funcType().getText();
        Symbol retType = curScope.find(typeString);
        FunctionSymbol funcType = new FunctionSymbol(funcName, (Type) retType, curScope);
        curScope.put(funcType);
        // if (ctx.funcFParams() != null) {
        // for (int i = 0; i < ctx.funcFParams().funcFParam().size(); i++) {
        // String id = ctx.funcFParams().funcFParam(i).IDENT().getText();
        // LLVMValueRef paramType = visit(ctx.funcFParams().funcFParam(i));
        // funcType.addParamSymbol(id, paramType);
        // }
        // }
        LLVMTypeRef returnType = i32Type;
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>();
        LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ 0, /* isVariadic */ 0);
        LLVMValueRef function = LLVMAddFunction(module, funcName, ft);
        // Visit block items to forbid create new scope in block.
        curScope = funcType;
        if (ctx.block() != null) {
            LLVMBasicBlockRef block1 = LLVMAppendBasicBlock(function, funcName + "Entry");
            LLVMPositionBuilderAtEnd(builder, block1);
            for (int i = 0; i < ctx.block().blockItem().size(); i++)
                visit(ctx.block().blockItem(i));
        }
        funcType.setValue(function);
        return funcType;
    }

    @Override
    public Symbol visitStmt(SysYParser.StmtContext ctx) {
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
        return visitChildren(ctx);
    }

    @Override
    public Symbol visitExp(SysYParser.ExpContext ctx) {
        // System.out.println("exp:" + ctx.getText());
        if (ctx.IDENT() != null) { // function
            return null;
        }
        if (ctx.lVal() != null) {
            Symbol symbol = curScope.find(ctx.lVal().IDENT().getText());
            if (symbol == null) {
                System.err.println("Cannot find symbol:" + ctx.lVal().IDENT().getText());
                return null;
            }
            // System.out.println("symbole value:" + symbol.getValue());
            LLVMValueRef value = LLVMBuildLoad(builder, symbol.getValue(), ctx.lVal().IDENT().getText());
            symbol.setValue(value);
            return symbol;
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
            return HandleBinaryOP(ctx, ctx.PLUS().getSymbol());
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
                valueRef = LLVMBuildAdd(builder, lv, rv, /* varName:String */"tmp_");
                break;
            case "*":
                valueRef = LLVMBuildMul(builder, lv, rv, "tmp_");
                break;
            default:
                break;
        }
        Symbol result = new BasicSymbol(valueRef.toString(), new BasicTypeSymbol("int"));
        result.setValue(valueRef);
        return result;
    }

}
