import java.util.*;


public class FunctionSymbolTable{   
     static String functionName;  
     static SymbolMap functionTable; 
     ArrayList<IRNode> functionIRList;
     // To hold operators
     static Stack<String> opStack;
     // To Hold List of current functions.
     private static int localVariableCount;
     private static int functionParameterCount;
     // Need to reset the counter.
     static boolean localScope = false;
     // Tracking Number of push statements
     static  int pushCountTrack;
     static  int localParamCount;

     public FunctionSymbolTable(String name){
        this.functionName = name;
        functionTable = new SymbolMap();
        functionIRList= new ArrayList<IRNode>();
        opStack = new Stack<String>();
        localVariableCount = 0;
        functionParameterCount = 0;
   
     }
     
   
 


     // Main function to populate symbol table entries. As hinted in document to use real name as key(Page 3)
     // Function Symbol Table Key: variables real name.
     // Function Symbol Table Value: Object of type syymbol with name as Lx/Px ,type and value.
     // Check insertLocalEntryFunction in SymbolMap.java
     
     public static void insertFunctionTableLocal(String name, String type, String value){
        String[] list = name.split(",");
        String loc = "";
        
        if(functionName.equals("GLOBAL")){
            for(int i=0; i< list.length; i++) {
               functionTable.insertLocalEntry(list[i].trim(),list[i].trim(), type, value);
            }
        }
        else{
           if(getParamCount()!=0) loc = "$P"+Integer.toString(getParamCount());
            for(int i=0; i< list.length; i++) {
               if(localScope){
                  localVariableCount++;
                  loc = "$L"+Integer.toString(getLocalCount());
          
               }
                //System.out.println("Param is "+loc+ "Real name is "+name);
         
               functionTable.insertLocalEntry(list[i],loc, type, value);
           }
    
        }
     }   
     public static void setLocalCount(int count) {
        localVariableCount = count;
     }
     public static void setParamCount(int count) {
        functionParameterCount = count;
     }


     public static void incParamCount() {
        functionParameterCount++;
     }
     

     public static int getParamCount() {
        return functionParameterCount;
     }
      
     public static int getLocalCount() {
        return localVariableCount;
     }
    
   

}
   
