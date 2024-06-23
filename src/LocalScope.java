import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;

public class LocalScope extends BaseScope{
    LLVMBasicBlockRef exitBlock;
    public LocalScope(Scope enclosingScope) {
        super("Local", enclosingScope);
    }
    public void setLLVMExitBlock(LLVMBasicBlockRef block) {
        exitBlock = block;
    }
    public LLVMBasicBlockRef getLLVMExitBlock() {
        return exitBlock;
    }
}
