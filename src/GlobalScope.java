public class GlobalScope extends BaseScope{
    public GlobalScope(Scope enclosingScope) {
        super(enclosingScope);
        this.put("int", new SysType("int"));
        this.put("void", new SysType("void"));
    }
    
}
