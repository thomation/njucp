import org.bytedeco.llvm.LLVM.LLVMValueRef;

public interface Symbol {
    String getName();
    Type getType();
    LLVMValueRef getValue();
    void setValue(LLVMValueRef value);
}
