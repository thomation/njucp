import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope {
    String name;
    final Map<String, Symbol> symbols = new LinkedHashMap<>();
    final Scope enclosingScope;

    public BaseScope(String name, Scope enclosingScope) {
        this.name = name;
        this.enclosingScope = enclosingScope;
        System.out.println("Define Scope: " + name);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Symbol> getSymbols() {
        return this.symbols;
    }

    public Scope getEncloseingScope() {
        return this.enclosingScope;
    }

    public void define(Symbol symbol) {
        this.symbols.put(symbol.getName(), symbol);
    }

    public Symbol resolve(String name) {
        Symbol symbol = this.symbols.get(name);
        if(symbol != null) {
            return symbol;
        }
        if(this.enclosingScope != null)
            return this.enclosingScope.resolve(name);
        return null;
    }
}
