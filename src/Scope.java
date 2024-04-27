import java.util.Map;

public interface Scope {
    String getName();
    void setName(String name);
    Map<String, Symbol> getSymbols();
    Scope getEncloseingScope();
    void define(Symbol symbol);
    Symbol resolve(String name);
}
