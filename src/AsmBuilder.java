import static org.bytedeco.llvm.global.LLVM.LLVMConstIntGetSExtValue;
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
import static org.bytedeco.llvm.global.LLVM.LLVMGetValueName;
import static org.bytedeco.llvm.global.LLVM.LLVMPrintValueToString;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.ByteBuffer;

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
        System.out.printf("build %s\n", module);
        for (LLVMValueRef value = LLVMGetFirstGlobal(module); value != null; value = LLVMGetNextGlobal(value)) {
            System.out.println(value);
        }
        for (LLVMValueRef func = LLVMGetFirstFunction(module); func != null; func = LLVMGetNextFunction(func)) {
            System.out.println(func);
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
                System.out.println(bb);
                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(
                        inst)) {
                    int opcode = LLVMGetInstructionOpcode(inst);
                    int operandNum = LLVMGetNumOperands(inst);
                    String instString = LLVMPrintValueToString(inst).getString();
                    System.out.printf("opcode:%d, operandNum:%d string:%s\n", opcode, operandNum, instString);
                    switch (opcode) {
                        case LLVM.LLVMRet:
                            String op1 = LLVMPrintValueToString(LLVMGetOperand(inst, 0)).getString();
                            System.out.printf("op: %s\n", op1);
                            ret(instString);
                            break;

                        default:
                            break;
                    }
                }
            }

        }
        buffer.append(".text\r\n" + //
                "  .globl main\r\n" + //
                "main:\r\n" + //
                "  addi sp, sp, 0\r\n" + //
                "mainEntry:\r\n" + //
                "  li a0, 0\r\n" + //
                "  addi sp, sp, 0\r\n" + //
                "  li a7, 93\r\n" + //
                "  ecall");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write(buffer.toString());
        } catch (Exception e) {
            System.err.println("Error! " + e.getMessage());
        }
    }

    public void ret(String inst) {

    }

    public void op2(String op, String dest, String lhs, String rhs) {
        buffer.append(String.format("  %s %s, %s, %s\n", op, dest, lhs, rhs));
    }
}
