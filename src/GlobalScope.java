public class GlobalScope extends BaseScope{
    public GlobalScope(Scope enclosingScope) {
        super("GlobalScope", enclosingScope);
        this.define(new BaseTypeSymbol("int"));
        this.define(new BaseTypeSymbol("void"));
    }
    
}
