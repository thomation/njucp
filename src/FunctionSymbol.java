import java.util.ArrayList;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

public class FunctionSymbol extends BaseScope implements Type, Symbol {
    Symbol retType;
    ArrayList<Symbol> paramsSymbol;
    LLVMValueRef value;
    LLVMTypeRef type;

    public FunctionSymbol(String funcName, Symbol retType, Scope enclosingScope) {
        super(funcName, enclosingScope);
        this.retType = retType;
    }

    public Symbol getRetType() {
        return retType;
    }

    public void addParamSymbol(Symbol symbol) {
        if (paramsSymbol == null) {
            paramsSymbol = new ArrayList<Symbol>();
        }
        paramsSymbol.add(symbol);
        put(symbol);
    }

    ArrayList<Symbol> getParamsSymbol() {
        return paramsSymbol;
    }

    @Override
    public boolean isMatched(Type rType) {
        return this.getClass() == rType.getClass();
    }

    @Override
    public String getName() {
        return this.scopeName;
    }

    @Override
    public Type getType() {
        return this;
    }

    @Override
    public LLVMValueRef getValue() {
        return value;
    }

    @Override
    public void setValue(LLVMValueRef value) {
        this.value = value;
    }
    public void setLLVMType(LLVMTypeRef type) {
        this.type = type;
    }
    public LLVMTypeRef getLLVMType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (getParamsSymbol() != null) {
            for (Symbol s : getParamsSymbol()) {
                sb.append(s.toString());
                sb.append(";");
            }
        }
        return String.format("funcname:%s rettype:%s params:%s;llvmValue:%s\n", scopeName, retType.toString(), sb.toString(), value);
    }
}
