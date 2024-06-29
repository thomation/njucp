import java.io.BufferedWriter;
import java.io.FileWriter;

import org.bytedeco.llvm.LLVM.LLVMModuleRef;

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

    public void op2(String op, String dest, String lhs, String rhs) {
        buffer.append(String.format("  %s %s, %s, %s\n", op, dest, lhs, rhs));
    }
}
