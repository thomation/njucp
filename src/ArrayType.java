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
}
