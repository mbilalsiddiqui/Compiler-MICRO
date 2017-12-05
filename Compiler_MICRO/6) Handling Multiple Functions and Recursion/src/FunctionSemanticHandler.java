import java.util.*;

public class FunctionSemanticHandler{
   // Keeping a list of current functions.

    static List<FunctionSymbolTable> currentActiveFunctions = new ArrayList<FunctionSymbolTable>();
    // This holds all the symbol Tables of all the functions.
    static Map<String,SymbolMap> symbolTables = new LinkedHashMap<String,SymbolMap>();
    // Current Function Table. SHould be accessible
     static FunctionSymbolTable currentFuncTable;
   
   // Everything will happen on the last function in this list i.e. function being currently
   // processes as functions might have recursion and nesting.
   public static FunctionSymbolTable checkLastFunction(){
     FunctionSymbolTable temp = currentActiveFunctions.get(currentActiveFunctions.size()-1);
     if(temp==null) return null;
     else
      return temp;
    
   }
   
   public static void setCurrent(FunctionSymbolTable curr){
      currentFuncTable = curr;
   }
      
   
   // Last Function's IRLIst's last IRNode contains RET or Not? if not then add it.
   public static void checkLastStatement(){
      FunctionSymbolTable symbol = checkLastFunction();
      int lastIndex = symbol.functionIRList.size()-1;
      IRNode lastNode = symbol.functionIRList.get(lastIndex);
      if(!lastNode.getOpcode().equals("RET")){
           symbol.functionIRList.add(new IRNode("RET", "", "",""));
      }
   }


   // For each function definition. Add LABEL @functionname and LINK statements
   // For Tiny generation "LINK @size" we can check the local variable and parameter count to
   // allocate space by accessing those functions below.

   public static void declareFunction(){
      FunctionSymbolTable currentEntry = checkLastFunction();
      currentEntry.functionIRList.add(new IRNode("LABEL", currentEntry.functionName, "",""));
      currentEntry.functionIRList.add(new IRNode("LINK", "", "",Integer.toString(FunctionSymbolTable.getLocalCount())));
   
   
   }
   // Printing Function IR lists
   public static void printIRCode(){
      FunctionSymbolTable temp;
     
      boolean globalOnly = true;
      //SymbolMap map = temp.functionTable;
      // YJ- step6
       System.out.println(";IR code");
      int i=0;
       /*for(int j=0; i<FunctionSemanticHandler.symbolTables.size(); j++ ){
           System.out.println("Function name is "+ FunctionSemanticHandler.symbolTables.get(j).functionTable.get(a));
 
       }*/   
    
      for(; i<currentActiveFunctions.size() ; i++){
         temp = currentActiveFunctions.get(i);
         // YJ -step6
            
          SemanticRoutines.tinyInterpreter(temp.functionIRList);
         // SemanticRoutines.printTinyCode();
         
         for(int j=0; j< temp.functionIRList.size();j++){
            IRNode node = temp.functionIRList.get(j);
            node.printIRNode();
           
        }
      }

       //YJ: initialize temp with global function for tiny
       SymbolMap map = new SymbolMap();
       map = symbolTables.get("GLOBAL");
       //System.out.println(map.map.keySet());
       Iterator<String> it = symbolTables.keySet().iterator(); 

      //YJ: now iterate through the symbol table to determine if we have only global scope
      while(it.hasNext()){

        String name = it.next();
        if (!((name.equals("GLOBAL")) || (name.equals("main"))))
        {
          //when you see any other function name
					globalOnly = false; 
        }
        
      }

      if (globalOnly)
      {
        SemanticRoutines.identifyVars(map);
        SemanticRoutines.printTinyCodeGlobal(); 
      }
      else if (!globalOnly)
      {
        //Calling print TinyCode here for step4 output
        SemanticRoutines.printTinyCode();
      }
      
      //printSymbolTable();
         
   }
/*   
   public static void printSymbolTable(){
      
      System.out.println();
             
         // temp is SymbolMap object. temp.map gives access to LinkedHashMap object.
      Iterator<String> it = symbolTables.keySet().iterator();         
         
         while(it.hasNext()){
            String name = it.next();
            System.out.println("Functions Name is "+name+ ": ");
            
            SymbolMap temp = new SymbolMap();
            temp = symbolTables.get(name);
         
            // temp is SymbolMap object. temp.map gives access to LinkedHashMap object.
            Iterator<String> iter = temp.map.keySet().iterator();         
         
            while(iter.hasNext()){
               String var_type = iter.next();
               //System.out.println(var_type);
               System.out.println("name "+ temp.map.get(var_type).getName() + " type "+ temp.map.get(var_type).getType() + " value " + temp.map.get(var_type).getValue());
            
            }   
            System.out.println();
    
         }

     // SymbolTable.printTable();    
   
   }
*/
   

}


