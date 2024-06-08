import org.bytedeco.llvm.LLVM.LLVMValueRef;

public class BasicSymbol implements Symbol {
    final String name;
    final Type type;
    LLVMValueRef value;

    public BasicSymbol(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    @Override
    public LLVMValueRef getValue() {
        return value;
    }

    @Override
    public void setValue(LLVMValueRef value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("name:%s, type:%s", name, type);
    }

}
