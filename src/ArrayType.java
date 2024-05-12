import org.antlr.v4.parse.ANTLRParser.throwsSpec_return;

public class ArrayType implements Type{
   Type contained;
   int num_elements; 
   public ArrayType(Type contained, int num_elements) {
      this.contained = contained;
      this.num_elements = num_elements;
   }
   public Type getContainedType() {
      return contained;
   }
   public String toString() {
      return String.format("(%d, %s)", num_elements, contained);
   }
   @Override
   public boolean isMatched(Type rType) {
      if(this.getClass() != rType.getClass())
         return false;
      ArrayType ra = (ArrayType)rType;
      return dimension() == ra.dimension();
   }
   int dimension() {
      int d = 1;
      Type cur = contained;
      while(cur instanceof ArrayType) {
         d ++;
         cur = ((ArrayType)cur).contained;
      }
      return d;
   }
}
