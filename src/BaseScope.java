import java.util.LinkedHashMap;

public class BaseScope implements Scope {
    final Scope enclosingScope;
    LinkedHashMap<String, Type> types = new LinkedHashMap<String, Type>();
    public BaseScope(Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    public void put(String name, Type type) {
        types.put(name, type);
    }

    public Scope getEncloseingScope() {
        return this.enclosingScope;
    }
    public Type find(String name) {
        Type type = this.types.get(name);
        if(type != null) {
            return type;
        }
        if(this.enclosingScope != null)
            return this.enclosingScope.find(name);
        return null;
    }
}
