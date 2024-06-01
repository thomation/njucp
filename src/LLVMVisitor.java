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
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
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
        // System.out.println("exp:" + ctx.getText());
        if (ctx.IDENT() != null) { // function
            // TODO: get llvm function from scope
            return null;
        }
        if (ctx.number() != null) {
            String numString = ctx.number().INTEGER_CONST().getText();
            int num =Integer.parseInt(numString);
            return LLVMConstInt(i32Type, num, /* signExtend */ 0);
        }
        if (ctx.unaryOp() != null) {
            return HandleUnaryOP(ctx, ctx.unaryOp());
        }
        if (ctx.PLUS() != null) {
            return HandleBinaryOP(ctx, ctx.PLUS().getSymbol());
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
            LLVMValueRef result = LLVMBuildNeg(builder, v, "result");
            return result;
        }
        return null;
    }

    LLVMValueRef HandleBinaryOP(SysYParser.ExpContext ctx, Token symbol) {
        System.out.printf("bop: %s\n", symbol.getText());
        LLVMValueRef lv = visit(ctx.exp(0));
        LLVMValueRef rv = visit(ctx.exp(1));
        switch (symbol.getText()) {
            case "+":
                LLVMValueRef result = LLVMBuildAdd(builder, lv, rv, /* varName:String */"result");
                return result;
            default:
                break;
        }
        return null;
    }

}
