import java.util.LinkedHashMap;

public class BaseScope implements Scope {
    final String scopeName;
    final Scope enclosingScope;
    LinkedHashMap<String, Symbol> symbols = new LinkedHashMap<String, Symbol>();
    public BaseScope(String scopeName, Scope enclosingScope) {
        this.scopeName = scopeName;
        this.enclosingScope = enclosingScope;
    }

    public void put(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
    }

    public Scope getEncloseingScope() {
        return this.enclosingScope;
    }
    public Symbol find(String name) {
        Symbol symbol = get(name);
        if(symbol != null) {
            return symbol;
        }
        if(this.enclosingScope != null)
            return this.enclosingScope.find(name);
        return null;
    }
    public Symbol get(String name) {
        return this.symbols.get(name);
    }
}
