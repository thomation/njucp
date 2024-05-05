public class GlobalScope extends BaseScope{
    public GlobalScope(Scope enclosingScope) {
        super("Global", enclosingScope);
        this.put("int", new IntType());
        this.put("void", new VoidType());
    }
    
}
