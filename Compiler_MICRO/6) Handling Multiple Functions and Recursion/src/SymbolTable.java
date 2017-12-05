import java.io.*;
import java.util.*;

public class SymbolTable {

   
   private static Stack<SymbolMap> stack = new Stack<SymbolMap>();
   // this static variable will be incremented on each new creation of scope.
   private static int scope_block_counter= 1;  

/* Scope Creation Functions. Have kept them separate for GLOBAL, BLOCK and FUNCTION Scopes.
   TODO! Can Merge them in one.     
*/   
   // This should be the first function call in the grammar where program begins   
   public static void createGlobalScope() {
      SymbolMap global = new SymbolMap();
      global.setScope("GLOBAL");
      stack.push(global);

   }

  
   // Popping Symbol Table to see whats inside :-). To be used by Semantic Routines
   public static SymbolMap popScope(){
      SymbolMap temp = new SymbolMap();
      //stack.pop();
      temp = stack.pop();
      return temp;

   }

   // Popping Symbol Table to see whats inside :-). To be used by Semantic Routines
   public static SymbolMap peekScope(){
      SymbolMap temp = new SymbolMap();
      //stack.pop();
      temp = stack.peek();
      return temp;

   }


  
   // Function call when IF, ELSEIF or FOR blocks start. 
   public static void createBlockScope() {
      SymbolMap current = new SymbolMap();
      current.setScope("BLOCK "+ Integer.toString(scope_block_counter));
      stack.push(current);
      //Increment for next calling of the function.
      scope_block_counter++;

   }

   //Scope Creation for function calls. Here function_name will be $id.text from Micro grammar. 
   public static void createFunctionScope(String function_name){
      SymbolMap current = new SymbolMap();
      current.setScope(function_name);
      stack.push(current);
   }

/* Insert functions for String and INT or FLOAT. Kept them separate because I am using split(',') to
   handle list declaration of ints or floats as examined in Micro testcase files. If a string has ","
   in it then that will enter wrong entry in the Symbol Table.
*/
  
   // @param name: id_list from Micro.g4 . @param type: Will either be 'INT' or 'FLOAT'. TODO! Handle Void
   public static void insertVariableType(String id_list, String type){

      //Stack will already have SymbolTable defined for the current scope.
      SymbolMap currentMap = stack.pop();
      String[] variable_list = id_list.split(",");
      // Need a new class here. Stuck if Only used One class for putting objects into SymbolMap.
      for(int i=0; i<variable_list.length; i++){
         
         currentMap.insertVariable(variable_list[i], type);
      }
      // Stack contains now a HashMap with entries and current scope.
      stack.push(currentMap);
   }


   // @param name: $id.text from Micro.g4 . @param type: "STRING". @param value: value of string
   public static void insertString(String id, String type, String value){
      
      //Stack will already have SymbolTable defined for the current scope.
      SymbolMap currentMap = stack.pop();
      currentMap.insertString(id, type, value);
      stack.push(currentMap);
   }


  

/**   My current design choice is to keep the full stack and do not pop off as we
   go out of current scope. It will add some redundancy of inverting the stack 
   and printing whole stack at once by the end of parsing. Refering to Terrence book,
   we need a tree structure to keep track of stacks at each scope level. We might need 
   that in future. So will define pop() later. And then we can
   get rid of this print function alltogethor.
*/   
  
   public static void printTable() {
   
      Stack<SymbolMap> print_stack = new Stack<SymbolMap>();
      
      //Inverting the stack for printing as warranted by step3.
      while(!stack.isEmpty()) {
         print_stack.push(stack.pop());
      }

      while(!print_stack.isEmpty()) {
          
         SymbolMap temp = new SymbolMap();
         temp = print_stack.pop();
         
         // temp is SymbolMap object. temp.map gives access to LinkedHashMap object.
         Iterator<String> it = temp.map.keySet().iterator();
         
         System.out.println("Symbol table " + temp.getScope());
         
         //TODO! Design Choice: Can get rid of nested classes to define better readable methods 
         while(it.hasNext()){
            String var_type = it.next();
            if(temp.map.get(var_type).getType() == "STRING") {
               System.out.println("name "+ temp.map.get(var_type).getName() + " type STRING" + " value " + temp.map.get(var_type).getValue());
            }
            else {
               System.out.println("name "+ temp.map.get(var_type).getName() + " type "+ temp.map.get(var_type).getType());
 
            }            
         }
         // Printing an extra line to match step3 requirements.
         if(!print_stack.isEmpty()) System.out.println();  
      }    
   }

    public static void printGlobalTiny() {
   
      Stack<SymbolMap> print_stack = new Stack<SymbolMap>();
      
      //Inverting the stack for printing as warranted by step3.
      while(!stack.isEmpty()) {
         print_stack.push(stack.pop());
      }

      while(!print_stack.isEmpty()) {
          
         SymbolMap temp = new SymbolMap();
         temp = print_stack.pop();
         
         // temp is SymbolMap object. temp.map gives access to LinkedHashMap object.
         Iterator<String> it = temp.map.keySet().iterator();
         
         if (temp.getScope().equals("GLOBAL"))
         {
         
           //TODO! Design Choice: Can get rid of nested classes to define better readable methods 
           while(it.hasNext()){
              String var_type = it.next();
              if(temp.map.get(var_type).getType() == "STRING") {
                 System.out.println("str "+ temp.map.get(var_type).getName() + " " + temp.map.get(var_type).getValue());
              }
              else {
                 //System.out.println("name "+ temp.map.get(var_type).getName() + " type "+ temp.map.get(var_type).getType());
 
              }            
            }
         // Printing an extra line to match step3 requirements.
         //if(!print_stack.isEmpty()) System.out.println();  
         }    
       }
   
     }

}



