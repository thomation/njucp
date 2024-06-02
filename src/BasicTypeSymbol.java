public class BasicTypeSymbol extends BasicSymbol implements Type {

    public BasicTypeSymbol(String name) {
        super(name, null);
    }

    @Override
    public boolean isMatched(Type rType) {
        if(rType instanceof BasicTypeSymbol) {
            BasicTypeSymbol rSymbol = (BasicTypeSymbol)rType;
            return this.name == rSymbol.name;
        }
        return false;
    }
    
}
