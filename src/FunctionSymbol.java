import java.util.ArrayList;

public class FunctionSymbol extends BaseScope implements Type, Symbol {
    Type retType;
    ArrayList<Symbol> paramsSymbol;

    public FunctionSymbol(String funcName, Type retType, Scope enclosingScope) {
        super(funcName, enclosingScope);
        this.retType = retType;
    }

    public Type getRetType() {
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

}
