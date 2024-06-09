import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;

import org.antlr.v4.runtime.Token;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;

public class LLVMVisitor extends SysYParserBaseVisitor<LLVMValueRef> {
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
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        curScope = globalScope;
        module = LLVMModuleCreateWithName("module");
        builder = LLVMCreateBuilder();
        i32Type = LLVMInt32Type();

        LLVMValueRef ret = visitChildren(ctx);
        LLVMDumpModule(module);
        return ret;
    }

    @Override
    public LLVMValueRef visitVarDef(SysYParser.VarDefContext ctx) {
        Symbol defSymbol = null;
        String varName = ctx.IDENT().getText();
        defSymbol = new BasicSymbol(varName, new BasicTypeSymbol("int"));
        curScope.put(defSymbol);
        LLVMValueRef varValue = visitChildren(ctx);
        if (curScope == globalScope) {
            LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, varName);
            LLVMSetInitializer(globalVar, varValue);
            defSymbol.setValue(globalVar);
            return globalVar;
        }
        LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/varName);
        LLVMBuildStore(builder, varValue, pointer);
        defSymbol.setValue(pointer);
        return pointer;
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
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
        // Type paramType = visit(ctx.funcFParams().funcFParam(i));
        // funcType.addParamType(id, paramType);
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
        return function;
    }

    @Override
    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.RETURN() != null) {
            if (ctx.exp() != null) {
                LLVMValueRef result = visit(ctx.exp());
                LLVMBuildRet(builder, /* result:LLVMValueRef */result);
                return result;
            }
            visit(ctx.SEMICOLON());
            LLVMBuildRet(builder, null);
            return null;
        }
        return visitChildren(ctx);
    }

    @Override
    public LLVMValueRef visitExp(SysYParser.ExpContext ctx) {
        System.out.println("exp:" + ctx.getText());
        if (ctx.IDENT() != null) { // function
            return null;
        }
        if(ctx.lVal() != null) { 
            Symbol symbol = curScope.find(ctx.lVal().IDENT().getText());
            if(symbol == null) {
                System.err.println("Cannot find symbol:" + ctx.lVal().IDENT().getText());
                return null;
            }
            System.out.println("symbole value:" + symbol.getValue());
            LLVMValueRef value =  LLVMBuildLoad(builder, symbol.getValue(), ctx.lVal().IDENT().getText());
            return value;
        }
        if (ctx.number() != null) {
            String numString = ctx.number().INTEGER_CONST().getText();
            int num = Integer.parseInt(numString);
            return LLVMConstInt(i32Type, num, /* signExtend */ 0);
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
        return visitChildren(ctx);
    }

    LLVMValueRef HandleUnaryOP(SysYParser.ExpContext ctx, SysYParser.UnaryOpContext unary) {
        // System.out.printf("uop: %s\n", unary.getText());
        LLVMValueRef v = visit(ctx.exp(0));
        if (unary.MINUS() != null) {
            LLVMValueRef result = LLVMBuildNeg(builder, v, "tmp");
            return result;
        }
        return null;
    }

    LLVMValueRef HandleBinaryOP(SysYParser.ExpContext ctx, Token symbol) {
        System.out.printf("bop: %s\n", symbol.getText());
        LLVMValueRef lv = visit(ctx.exp(0));
        LLVMValueRef rv = visit(ctx.exp(1));
        LLVMValueRef result = null;
        switch (symbol.getText()) {
            case "+":
                result = LLVMBuildAdd(builder, lv, rv, /* varName:String */"tmp");
                break;
            case "*":
                result = LLVMBuildMul(builder, lv, rv, "tmp");
                break;
            default:
                break;
        }
        return result;
    }

}
