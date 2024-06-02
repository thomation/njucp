public interface Scope {
    void put(Symbol symbol);
    Scope getEncloseingScope();
    Symbol find(String name);
    Symbol get(String name);
}
