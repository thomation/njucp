import static org.bytedeco.llvm.global.LLVM.LLVMGetFirstBasicBlock;
import static org.bytedeco.llvm.global.LLVM.LLVMGetFirstFunction;
import static org.bytedeco.llvm.global.LLVM.LLVMGetFirstGlobal;
import static org.bytedeco.llvm.global.LLVM.LLVMGetFirstInstruction;
import static org.bytedeco.llvm.global.LLVM.LLVMGetInstructionOpcode;
import static org.bytedeco.llvm.global.LLVM.LLVMGetNextBasicBlock;
import static org.bytedeco.llvm.global.LLVM.LLVMGetNextFunction;
import static org.bytedeco.llvm.global.LLVM.LLVMGetNextGlobal;
import static org.bytedeco.llvm.global.LLVM.LLVMGetNextInstruction;
import static org.bytedeco.llvm.global.LLVM.LLVMGetNumOperands;
import static org.bytedeco.llvm.global.LLVM.LLVMGetOperand;
import static org.bytedeco.llvm.global.LLVM.LLVMPrintValueToString;

import java.io.BufferedWriter;
import java.io.FileWriter;

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;

public class AsmBuilder {
    StringBuffer buffer;
    LLVMModuleRef module;
    String outputPath;

    public AsmBuilder(LLVMModuleRef module, String outputPath) {
        this.module = module;
        this.outputPath = outputPath;
        buffer = new StringBuffer();
    }

    public void build() {
        for (LLVMValueRef value = LLVMGetFirstGlobal(module); value != null; value = LLVMGetNextGlobal(value)) {
            String data = LLVMPrintValueToString(value).getString();
            gVar(data);
        }
        for (LLVMValueRef func = LLVMGetFirstFunction(module); func != null; func = LLVMGetNextFunction(func)) {
            int offset = 64;
            gFuncEnter(offset);
            int cur = offset;
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(
                        inst)) {
                    int opcode = LLVMGetInstructionOpcode(inst);
                    String instString = LLVMPrintValueToString(inst).getString();
                    System.out.printf("op:%d, inst:%s\n", opcode, instString);
                    switch (opcode) {
                        case LLVM.LLVMRet:
                            String op1 = LLVMPrintValueToString(LLVMGetOperand(inst, 0)).getString();
                            System.out.println(op1);
                            ret(op1);
                            break;
                        default:
                            break;
                    }
                }
            }
            gFuncExit(offset);

        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write(buffer.toString());
        } catch (Exception e) {
            System.err.println("Error! " + e.getMessage());
        }
    }

    void gFuncEnter(int offset) {
        buffer.append(".text\n");
        buffer.append(".global main\n");
        buffer.append("main:\n");
        buffer.append("addi sp, sp, -" + offset +"\n");
    }
    void gFuncExit(int offset) {
        buffer.append("addi sp, sp, " + offset + "\n");
        buffer.append("li a7, 93\n");
        buffer.append("ecall\n");
    }

    void gVar(String var) {
        buffer.append(".data\n");
        String[] vars = var.split(" ");
        String varName = vars[0].substring(1);
        String index = vars[4];
        buffer.append(varName + "\n");
        buffer.append("word " + index + "\n");
    }

    void ret(String op) {
        String[] ops = op.split(" ");
        buffer.append("li a0 " + ops[1] + "\n");
    }

    public void op2(String op, String dest, String lhs, String rhs) {
        buffer.append(String.format("  %s %s, %s, %s\n", op, dest, lhs, rhs));
    }
}
