public class IntType implements Type{

    @Override
    public boolean isMatched(Type rType) {
        return this.getClass() == rType.getClass();
    }
    
}
