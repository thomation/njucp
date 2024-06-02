public class GlobalScope extends BaseScope {
    public GlobalScope(Scope enclosingScope) {
        super("Global", enclosingScope);
        this.put(new BasicTypeSymbol("int"));
        this.put(new BasicTypeSymbol("void"));
    }

}
