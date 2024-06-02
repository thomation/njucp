public class ArraySymbol implements Type, Symbol {
   String name;
   Type contained;
   int num_elements;

   public ArraySymbol(String name, Type contained, int num_elements) {
      this.name = name;
      this.contained = contained;
      this.num_elements = num_elements;
   }

   public Type getContainedType() {
      return contained;
   }

   public String toString() {
      if (num_elements == 0) {
         return String.format("(dynamic, %s)", contained);
      }
      return String.format("(%d, %s)", num_elements, contained);
   }

   @Override
   public boolean isMatched(Type rType) {
      if (this.getClass() != rType.getClass())
         return false;
      ArraySymbol ra = (ArraySymbol) rType;
      return dimension() == ra.dimension();
   }

   int dimension() {
      int d = 1;
      Type cur = contained;
      while (cur instanceof ArraySymbol) {
         d++;
         cur = ((ArraySymbol) cur).contained;
      }
      return d;
   }

   @Override
   public String getName() {
      return name;
   }
}
