import java.util.ArrayList;

public class FunctionType extends BaseScope implements Type {
    Type retType;
    ArrayList<Type> paramsType;

    public FunctionType(String funcName, Scope enclosingScope) {
        super(funcName, enclosingScope);
    }

    public void setRetType(Type retType) {
        this.retType = retType;
    }

    public Type getRetType() {
        return retType;
    }

    public void addParamType(String typeName, Type paramType) {
        if (paramsType == null) {
            paramsType = new ArrayList<Type>();
        }
        paramsType.add(paramType);
        put(typeName, paramType);
    }

    ArrayList<Type> getParamsType() {
        return paramsType;
    }

    @Override
    public boolean isMatched(Type rType) {
        return this.getClass() == rType.getClass();
    }

}
