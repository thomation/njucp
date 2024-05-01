import java.util.ArrayList;

public class FunctionType implements Type {
    Type retType;
    ArrayList<Type> paramsType;
    public FunctionType(Type retType, ArrayList<Type> paramsType) {
        this.retType = retType;
        this.paramsType = paramsType;
    }

}
