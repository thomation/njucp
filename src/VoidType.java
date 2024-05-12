public class VoidType implements Type {

    @Override
    public boolean isMatched(Type rType) {
        return this.getClass() == rType.getClass();
    }
    
}
