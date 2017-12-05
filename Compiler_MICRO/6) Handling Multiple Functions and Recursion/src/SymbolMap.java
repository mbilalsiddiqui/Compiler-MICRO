import java.util.*;
public class SymbolMap{

   private String scope = null;
   //To keep original insertion order of keys.  
   public LinkedHashMap<String, Symbol> map = new LinkedHashMap<String,Symbol>();
   
   //Inner nested class
   class Symbol{

      private String name;
      private String var_type;
      private String value;
   
      // Defining two default constructors for Symbols of Type INT or FLOAT and for Holding String Records
      private Symbol(String symbol_name, String id_float) {  
   
         this.name = symbol_name;
         this.var_type = id_float;
         //this.value = null;
      }

      private Symbol(String symbol_name, String type, String value) {  

         this.name = symbol_name;
         this.var_type = type;
         this.value = value;
      }
      // Getter Methods
      public String getType() {
         return this.var_type;
      }   

      public String getName() {
         return this.name;
      } 
      
      public String getValue() {
         return this.value;
      }
   }
   
   public void insertVariable(String var_name, String type){
  
      // To access nested class constructor we need to instantiate the object first
      Symbol sym = new Symbol(var_name, type);
      if(!map.containsKey(var_name)) {

         map.put(var_name,sym);

      }
      else {
          System.out.println("DECLARATION ERROR " + var_name);
          //System.out.println("Error in inserting INT or FLOAT ");
          System.exit(-1);
      }


   }

   public void insertString(String name, String type, String value){
      
      // Utilizing Inner class object with Constructor overloading
      Symbol sym = new Symbol(name, type, value);
      if(!map.containsKey(name)) {

         map.put(name,sym);

      }
      else {
          System.out.println("DECLARATION ERROR " + name);
          //System.out.println("Error in inserting STRING LITERAL ");
          System.exit(-1);
      }


   }
   
   
   public void insertFunctionTableEntry(String name, String type, String value){
      
      // Utilizing Inner class object with Constructor overloading
      Symbol sym = new Symbol(name, type, value);
      if(!map.containsKey(name)) {

         map.put(name,sym);

      }
      else {
          System.out.println("DECLARATION ERROR " + name);
          //System.out.println("Error in inserting STRING LITERAL ");
          System.exit(-1);
      }


   }
   
    public void insertLocalEntry(String name, String localName, String type, String value){
      
      // Utilizing Inner class object with Constructor overloading
      Symbol sym = new Symbol(localName, type, value);
      if(!map.containsKey(name)) {

         map.put(name,sym);

      }
      else {
          System.out.println("DECLARATION ERROR " + name);
          //System.out.println("Error in inserting STRING LITERAL ");
          System.exit(-1);
      }


   }
  

   // Setter and getter for private variable scope
   public void setScope(String scope){
      this.scope = scope;
   }


   public String getScope(){
      return this.scope;
   }

}

