public class BasicSymbol implements Symbol {
    final String name;
    final Type type;
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
    
}
