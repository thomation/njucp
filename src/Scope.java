public interface Scope {
    void put(String name, Type type);
    Scope getEncloseingScope();
    Type find(String name);
    Type get(String name);
}
