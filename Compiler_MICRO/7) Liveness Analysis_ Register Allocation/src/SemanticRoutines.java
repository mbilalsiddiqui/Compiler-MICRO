import java.util.*;

public class SemanticRoutines{
   // Each function will have its own list of IR nodes.
   static ArrayList<IRNode> IR_LIST = new ArrayList<IRNode>();
   static FunctionSymbolTable currentFunction;
   static int numberOfPush = 0;
   // Registers Mapping
   static HashMap<String,String> registerMapping = new HashMap<String,String>();
   // Initialize operation mapping
  
 
   static HashMap<String,String> temporaryTypeMap = new HashMap<String,String>();
   static int current_register = 1;
   //YJ: This keeps track of the parameters of the function currently interpreted by TinyInterpreter
   static int currentFuncParams = 0;
   //YJ: Tiny register allocation tracker
   static int tinyRegAllocTracker = 0;
   //YJ: Name of current function being interpreted by TinyInterpreter
   static String currentFuncName = "";
   //YJ: tiny Symbol map
   static SymbolMap tinyCurrentMap;
   //YJ-STEP-7: register allocator hash map
   public static HashMap<String,RegisterAllocator> regAlloc = new LinkedHashMap<String,RegisterAllocator>();    
   //YJ-STEP-7: live set updated with each IRnode that is being processed for Tiny code generation
   public static Set<String> liveSet = new HashSet<String>();
   //YJ-STEP7: Use in Allocate 
    public static String lastFreedReg = "";

   // To store the variables defined in the program in the right order 
   public static ArrayList<String> VAR_LIST = new ArrayList<String>();
   // To store TinyInstructionNodes 
   public static ArrayList<TinyInstructionNode> TINY_LIST = new ArrayList<TinyInstructionNode>();
   /* Building Temporay Registers for use in IR Code Generation. Will increment count on every time*/
   private static int current_temporay_register = 1;
   private static int label_counter = 1;
   private static boolean workaround = false;
   public SymbolMap current_symbol_map = new SymbolMap();
    // Label to IRNode Mapping to be used for CONTROL FLOW GRAPH. Also used it in
   // FunctionSemanticHandler.java to save Function Name Mapping
   public static HashMap<String,IRNode> conditionalUnconditionalJumpTarget = new HashMap<String,IRNode>();

   public static Stack<String> conditional_statement_operator = new Stack<String>();
   public static Stack<String> labels = new Stack<String>();
  
   static IRNode for_init;
   static IRNode for_incr_second; 
   static IRNode for_incr_first;
   //YJ-STEP-7: Initializing the hash map for 4 registers    
   public static void initialize(){
    for(int i=0; i<4; i++) {
      if(!regAlloc.containsKey("r"+String.valueOf(i)))
         regAlloc.put("r"+String.valueOf(i), new RegisterAllocator("empty", false));
    }
   }
   public static void resetTemporaryRegisterCounter(){
      current_register = 1;
   }
   //Map types(Int or Float) of registers to names
   public static String getTempMapping(String type){
      String temp = "$T"+Integer.toString(current_register);
      temporaryTypeMap.put(temp, type);
      current_register++;
      return temp;
   }

   // Returns type of the variable and its name and also checks global scope if not in local scope.
   public static VarInfo returnTypeOfVar(String id, SymbolMap current_map){
       currentFunction = FunctionSemanticHandler.currentFuncTable;
       //System.out.println("Current funcin is "+ currentFunction.functionName);
       String localName = "";
       String type = "";
       VarInfo local = new VarInfo(localName,type); 
        if(!currentFunction.functionTable.map.containsKey(id)){
        
          if(!current_map.map.containsKey(id)){
             System.out.println("Variable "+id+" not present");
             System.exit(0);
          }
          else{
             local.localName = current_map.map.get(id).getName();
             local.type = current_map.map.get(id).getType();
          }
       }
       else{
          
          local.localName = currentFunction.functionTable.map.get(id).getName();
          local.type = currentFunction.functionTable.map.get(id).getType();
      
       }
      return local;

   }
  
   // a := b  ---> a is left value and b is right value
   public static void leftRightValuestoIR(String id, SymbolMap current_map){
       //
       currentFunction = FunctionSemanticHandler.currentFuncTable;
       VarInfo local = returnTypeOfVar(id, current_map);
       //Create Mapping
       registerMapping.put(local.localName, local.type);
       currentFunction.opStack.push(local.localName);     
       //System.out.println("Pushed value is"+ local.localName);
  
 
   }
   // For statments of type b := 40. Separating them into two functions
   public static void constantToIR(String constant){
        // For generating IR for constant stores. STOREI constant $Tx
        currentFunction = FunctionSemanticHandler.currentFuncTable;
        String type =  getTypeOfLRvalue(constant);
     
        // Create New Temporary
        String temporary = getTempMapping(type);
        
        currentFunction.functionIRList.add(new IRNode("STORE"+type.charAt(0), constant, "",temporary));
        currentFunction.opStack.push(temporary);
   }


    public static void simpleAssignmentToIR(SymbolMap current_map){
        // For generating IR for constant stores. STOREI $Tx variable 
        currentFunction = FunctionSemanticHandler.currentFuncTable;
        //System.out.println("Stack size is " + currentFunction.opStack.size() );
        if(currentFunction.opStack.size()<2) return;
        String op1 = currentFunction.opStack.pop();
        String op2 = currentFunction.opStack.pop();
       
        String type = getTypeReg(current_map, op1, op2);
       
        currentFunction.functionIRList.add(new IRNode("STORE"+type.charAt(0), op2, "",op1));
    }





   public static String getTypeOfLRvalue(String constant) {
        String type="";
        if(constant.matches("[0-9]+")){
           type = "INT";
        }
        if(constant.matches("[0-9]*\\.[0-9]+")){

           type = "FLOAT";
        }

        if(type==""){
           type = registerMapping.get(constant);
        }
        return type;
   }
   
   public static String getTypeReg(SymbolMap current_map, String op1, String op2){
        String type = "";
        //FunctionSemanticHandler.printIRCode(); 
     
        if(op1.contains("$T")){
           type = temporaryTypeMap.get(op1);
        }else if (op2.matches("[0-9]+")) {
	   type = "INT";
	}
	else if (op2.matches("[0-9]*\\.[0-9]+")) {
	   type = "FLOAT";
	}
	else { 
           if(current_map.map.containsKey(op1)){
                  type = current_map.map.get(op1).getType();
           }
          
	}
	if(type == "")
	 {
		type = registerMapping.get(op1);
	 }
         return type;
          
   }


   // Handlling mul/div/add/sub
   public static void handleOperator(String operator, SymbolMap current_map){
       // For generating IR for constant stores. STOREI $Tx variable 
        currentFunction = FunctionSemanticHandler.currentFuncTable;
        
        //System.out.println("Handle Operator Stack size is"+currentFunction.opStack.size());
      
        if(currentFunction.opStack.size()<2) return;
        String op2 = currentFunction.opStack.pop();
        String op1 = currentFunction.opStack.pop();
        String type = getTypeReg(current_map, op1, op1);
        // Create New Temporary
        String temporary = getTempMapping(type);
        //System.out.println("getTemp"+temporary);
        currentFunction.opStack.push(temporary);
        String oper = "";
        if(operator.equals("*")) oper = "MULT";
        if(operator.equals("+")) oper = "ADD";
        if(operator.equals("/")) oper = "DIV";
        if(operator.equals("-")) oper = "SUB";
      
        currentFunction.functionIRList.add(new IRNode(oper+type.charAt(0), op1, op2,temporary));

   }
 

   public static void addPushToIR(String expr){
       
       if(currentFunction.pushCountTrack==0){
          currentFunction.functionIRList.add(new IRNode("PUSH", "", "",""));
       }
       currentFunction.pushCountTrack++;
       currentFunction = FunctionSemanticHandler.currentFuncTable;
       
       String localName = ""; 
       if(!currentFunction.functionTable.map.containsKey(expr)){
           // Get the current temp
           localName = "$T"+ Integer.toString(current_register-1);
       }
       else{
           // Get base indexed local name
           localName = currentFunction.functionTable.map.get(expr).getName();
       }
       currentFunction.functionIRList.add(new IRNode("PUSH", localName, "",""));
     
   }
   
   public static void addJsrAndPoptoIR(String function){
       currentFunction = FunctionSemanticHandler.currentFuncTable;
       currentFunction.functionIRList.add(new IRNode("JSR", function, "",""));
       // Popping equivalent number of pushed parameters
       for(int i=0; i< currentFunction.pushCountTrack; i++){
           currentFunction.functionIRList.add(new IRNode("POP", "", "",""));
       }
       // Keep type as int across all. Redundant here
       String temporary = getTempMapping("INT");
       currentFunction.functionIRList.add(new IRNode("POP", temporary, "",""));
       currentFunction.opStack.push(temporary);
   }

   public static void returnToIR(SymbolMap current_map){
        currentFunction = FunctionSemanticHandler.currentFuncTable;
        String op1 = currentFunction.opStack.pop();
        String type = getTypeReg(current_map,op1,op1);
        currentFunction.functionIRList.add(new IRNode("STORE"+type.charAt(0), op1, "","$R"));
        currentFunction.opStack.push(op1);
        currentFunction.functionIRList.add(new IRNode("RET", "", "",""));
         
     
   }




    public static void readIRNodes(String memVariable, SymbolMap current_map) {
            currentFunction = FunctionSemanticHandler.currentFuncTable;
            // B.S: Added functionality to handle multiple read/write parameters. Can be string
            String[] listOfRead = memVariable.split(",");
            IRNode temp;
            for(int i=0; i< listOfRead.length; i++){
               String readType = ""; 
               if(current_map.map.containsKey(listOfRead[i])){
                  readType = current_map.map.get(listOfRead[i]).getType();
                 }
               else{
               
                VarInfo local = returnTypeOfVar(listOfRead[i],current_map);
                 
              
                readType= local.type;
                }
               // If not a local variable then just use its global name.
	       if(currentFunction.functionTable.map.containsKey(listOfRead[i])){
                  String localName = currentFunction.functionTable.map.get(listOfRead[i]).getName();
                  temp = new IRNode("READ"+readType.charAt(0),"","",localName);

               }
               else{
                  temp = new IRNode("READ"+readType.charAt(0),"","",listOfRead[i]);

               }
               currentFunction.functionIRList.add(temp);
           		
	       
   
            }
              

    }
   

   public static void writeIRNodes(String memVariable, SymbolMap current_map) {
           currentFunction = FunctionSemanticHandler.currentFuncTable;
           // B.S: Added functionality to handle multiple read/write parameters
           String[] listOfWrites = memVariable.split(",");
           IRNode temp;
           for(int i=0; i< listOfWrites.length; i++){
               String writeType = ""; 
               if(current_map.map.containsKey(listOfWrites[i])){
                  writeType = current_map.map.get(listOfWrites[i]).getType();
                 }
               else{
               
                VarInfo local = returnTypeOfVar(listOfWrites[i],current_map);
                 
              
                writeType= local.type;
                }
               // If not a local variable then just use its global name.
               if(currentFunction.functionTable.map.containsKey(listOfWrites[i])){
                  String localName = currentFunction.functionTable.map.get(listOfWrites[i]).getName();
                  temp = new IRNode("WRITE"+writeType.charAt(0),"","",localName);

               }
               else{
                  temp = new IRNode("WRITE"+writeType.charAt(0),"","",listOfWrites[i]);

               }
               currentFunction.functionIRList.add(temp);
           		
           
           }
   }

   public static String labelGenerator(){
       
      String temporary = "label" + Integer.toString(label_counter);
      label_counter++;
      return temporary;
   }


   public static String pushOnLabelStack(){
      // We can keep track of depth of nesting using stack of labels.
      // Parser rules will  make sure that correct label was popped or peek at(in case of jump don't want to pop).
      String curr_label = labelGenerator();
      labels.push(curr_label);
      return curr_label;
   
   }
   
   public static String popLabelStack(){
      return labels.pop();  
   }
 
   public static String peekOnLabelStack(){
      if(!labels.isEmpty()) {
         return labels.peek();
      }
      else{
         return " ";
         //System.out.println("IN-VALID MICRO CODE");
         //System.exit(-1);
      }
   
   }
   
   
   public static String peekSecondLabel(){
      String lab1 ="", lab2="";
      if(!labels.isEmpty()) {
          lab1 =  ""+labels.pop();
      }
      if(!labels.isEmpty()) {
          lab2 =  ""+labels.pop();
      }
      
      labels.push(lab2);
      labels.push(lab1);
      return lab2;
   }
  
   public static String peekThirdLabel(){
      String lab1 ="", lab2="", lab3="";
      if(!labels.isEmpty()) {
          lab1 =  ""+labels.pop();
      }
      if(!labels.isEmpty()) {
          lab2 =  ""+labels.pop();
      }
      
      if(!labels.isEmpty()) {
          lab3 =  ""+labels.pop();
      }
      labels.push(lab3);
      labels.push(lab2);
      labels.push(lab1);
      return lab3;
   }
   

   public static void makeJumpIRCode(String label){
       //Current Function Table
       currentFunction = FunctionSemanticHandler.currentFuncTable;
       //Save node for CFG mapping .
       IRNode temp = new IRNode("JUMP", "", "",label);
       //TODO! Chnage here
       //conditionalUnconditionalJumpTarget.put(label,temp);
       
       conditionalUnconditionalJumpTarget.put(label,conditionalUnconditionalJumpTarget.get(label));
       currentFunction.functionIRList.add(temp);
       
	
   }

   public static void makeLabelIRCode(String label){
       currentFunction = FunctionSemanticHandler.currentFuncTable;
         //Save node for CFG mapping .
       IRNode temp = new IRNode("LABEL", "", "",label);
       conditionalUnconditionalJumpTarget.put(label,temp);
       currentFunction.functionIRList.add(temp);	
   }
   
   public static void conditionalOperatorStack(String expr){
      conditional_statement_operator.push(expr.trim());
   }

   public static void addConditionalStatementIRCode(String compop, SymbolMap map){
     
      currentFunction = FunctionSemanticHandler.currentFuncTable;
      if(currentFunction.opStack.size()<2) return;   
      // expr compop expr : op1 is expr on right and op2 is expr on left
      String op1 = currentFunction.opStack.pop();
      String op2 = currentFunction.opStack.pop();
      
      
      String current_label = peekOnLabelStack();     
      
      if (compop.equals("<")) {
	 currentFunction.functionIRList.add(new IRNode("GE", op2 , op1, current_label));
      }
      else if (compop.equals(">")) {
	 currentFunction.functionIRList.add(new IRNode("LE", op2 , op1, current_label));
      }
      else if (compop.equals("=")) {
	 currentFunction.functionIRList.add(new IRNode("NE", op2 , op1, current_label));
      }
      else if (compop.equals("!=")) {
	  currentFunction.functionIRList.add(new IRNode("EQ", op2 , op1, current_label));
      }
      else if (compop.equals("<=")) {
	  currentFunction.functionIRList.add(new IRNode("GT", op2 , op1, current_label));
      }
      else if (compop.equals(">=")) {
	  currentFunction.functionIRList.add(new IRNode("LT", op2 , op1, current_label));
      } 
      
      
   }

  
   public static void initiateFunction(String id){
  
      SymbolTable.createFunctionScope(id);
      //symbolTables.add();
      FunctionSymbolTable curr = new FunctionSymbolTable(id); 
      //System.out.println("Function Name is"+$id.text);
      curr.setParamCount(0); 
      temporaryTypeMap.clear();
      resetTemporaryRegisterCounter();
      FunctionSemanticHandler.currentActiveFunctions.add(curr);
     
   }
   
   public static void setParamCount(){
      currentFunction = FunctionSemanticHandler.currentFuncTable;
     
      currentFunction.localParamCount = FunctionSymbolTable.getParamCount();
   }
   public static void enterForLoop(){
      FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction());
      pushOnLabelStack();
      pushOnLabelStack();
      //pushOnLabelStack();
   }

   public static void forLoopStart() 
   {
      currentFunction = FunctionSemanticHandler.currentFuncTable;
      // e.g. incr: i := i+1 --> first and second refers to 'i' and '1' respectively  
      for_incr_first = currentFunction.functionIRList.remove(currentFunction.functionIRList.size()-1);
      for_incr_second = currentFunction.functionIRList.remove(currentFunction.functionIRList.size()-1);
        
      IRNode temp =  currentFunction.functionIRList.get(currentFunction.functionIRList.size()-1); 
      
      if(temp.getOperand1().matches("[0-9]*\\.[0-9]+") || temp.getOperand1().matches("[0-9]+")){
         for_init =  currentFunction.functionIRList.remove(currentFunction.functionIRList.size()-1);
     
         workaround=true;
      }
      //temp.printIRNode();
     }

   
   public static void endForLoop()
   {
      currentFunction = FunctionSemanticHandler.currentFuncTable;
      //makeLabelIRCode(peekSecondLabel());
    
      // Adding the init and incr statement IR nodes
      if(workaround){
       
         currentFunction.functionIRList.add(for_init);
      }
      /* Handling two generated labels in FOR loop rule start. 
         Here label 1: LOOP start and label2: OUT and currently on top of stack is label2
         No label for INCR statement */
      
      currentFunction.functionIRList.add(for_incr_second);
      currentFunction.functionIRList.add(for_incr_first);
      
      
   
      makeJumpIRCode(peekSecondLabel());
      makeLabelIRCode(popLabelStack());
      // poplabelStack() called two times will empty for loop labels.
      popLabelStack();
      //popLabelStack();
   }
//YJ -- Helper functions for step7

//This function checks if a string completely a number
public static boolean isNumeric(String str)
{
  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
}

//This function checks if a variable is live or not
public static boolean isLive(String var)
{
  if (var.equals("empty"))
   return true;
  else
   return liveSet.contains(var);
}

//This function frees all dead variables
public static void freeDeadVars(String var)
{
  if (!isNumeric(var))
  {
  Set<Map.Entry<String, RegisterAllocator>> set = regAlloc.entrySet();
  //Iterate through each register
  for(Map.Entry<String, RegisterAllocator> me : set)
  {
   if(me.getValue().getRegValue().equals(var))
   {
    if(!isLive(me.getValue().getRegValue()))
    {
      //System.out.println(";Free'd Register: "+me.getKey()+" Having Value: "+ me.getValue().getRegValue());
      free(me.getKey());
    }
   }
  }
  }

}

//YJ--STEP7: ENSURE

    public static String ensure(String opr)
    {
      /* 
      This function ensure that the input operand is in the register, if it is then it returns the register containing that operand else
      it allocates the register for the operand and returns that register. If there is no register free then it generates the spill for 
      register, allocates that register to the operand and returns that register 
      */

      String reg = "";
      boolean dummy = true;

      //if the operand is an int or float return the operand as it is
      if(isNumeric(opr))
        return opr;

      //Now check if the opr is already in a register or not, if yes then ruturn the respective register
      //A set for the regAlloc entries
      Set<Map.Entry<String, RegisterAllocator>> set = regAlloc.entrySet();
      //Iterate through entries
      for(Map.Entry<String, RegisterAllocator> me : set)
      {
        if(me.getValue().getRegValue().equals(opr))
        {
          //System.out.println("THIS IS ENSURE - "+"Variable: "+me.getValue().getRegValue()+" is Stored in REG: "+me.getKey());
          reg = me.getKey();
          dummy = false;
          //System.out.println("THIS IS ENSURE");
          //System.out.println("REGISTER: "+ me.getKey()+" VALUE: "+ me.getValue().getRegValue() + " STATUS: "+me.getValue().getRegStatus());
          break;
        }
      }
    
      if (dummy)
      {
        //If the operand is not in any register then it must be allocated one
        reg = allocate(opr);
        //Generate a move (load) from opr into reg using Tiny Instructions
       // System.out.println("ENSURE: load was generated for: "+ opr + " using: "+ reg);
        TINY_LIST.add(new TinyInstructionNode("move", opr, reg));      
      }

    //System.out.println("REGISTER 0 VALUE: "+ regAlloc.get("r0").getRegValue()+ " REGISTER 0 STATUS: "+ regAlloc.get("r0").getRegStatus());
    //System.out.println("REGISTER 1 VALUE: "+ regAlloc.get("r1").getRegValue()+ " REGISTER 1 STATUS: "+ regAlloc.get("r1").getRegStatus());
    //System.out.println("REGISTER 2 VALUE: "+ regAlloc.get("r2").getRegValue()+ " REGISTER 2 STATUS: "+ regAlloc.get("r2").getRegStatus());
    //System.out.println("REGISTER 3 VALUE: "+ regAlloc.get("r3").getRegValue()+ " REGISTER 3 STATUS: "+ regAlloc.get("r3").getRegStatus());
 

      //return register
      return reg;                  
    }

//YJ--STEP7: FREE
    public static void free(String reg)
    {
    /* 
    Given a register as input it frees the register, if the register is dirty and variable is live then generates a store for that variable
    */
      boolean regStatus;
      String regVar;
         
      //if you find reg as dirty and the variable contained in reg is live then
      //Extract the status and value (variable) strored in the reg
      regStatus = regAlloc.get(reg).getRegStatus();
      regVar = regAlloc.get(reg).getRegValue();

      if ((regStatus) && isLive(regVar))
      {
        // store the variable into memory -- i.e. add tiny instruction for that
        TINY_LIST.add(new TinyInstructionNode("move", reg, regVar));
        System.out.println(";Spill For Reg: "+reg+" Having value: "+regVar);
      }
      
      //free the register
      regAlloc.put(reg, new RegisterAllocator("empty", false));
      //regAlloc.get(reg).setRegValue("empty"); 
      //regAlloc.get(reg).setRegStatus(false);
      
    }

//YJ--STEP7: ALLOCATE
    public static String allocate(String opr)
    {
    /* 
    if there is a free register chose that register for this operand, if not then chose a register to free. Once you get a register for this
    operand mark that register for this operand. Return register after all of this 
    */
     
      String reg = "";
      boolean noFreeRegNoDirty = true;
      boolean noFreeReg = true;     

      //iterate through the regAlloc hashmap entries, if you find a free register allocate it
      Set<Map.Entry<String, RegisterAllocator>> set = regAlloc.entrySet();
      for(Map.Entry<String, RegisterAllocator> me : set)
      {
        //System.out.println("ALLOCATE ITERATION");
        if (me.getValue().getRegValue().equals("empty"))
        {
          //System.out.println("THIS IS ALLOCATE");
          //System.out.println("REGISTER: "+ me.getKey()+" VALUE: "+ me.getValue().getRegValue() + " STATUS: "+me.getValue().getRegStatus());
          //select the register
          reg = me.getKey();
          noFreeReg = false;
          break;
        }    
        
      }

      if (noFreeReg)
      {
        for(Map.Entry<String, RegisterAllocator> me : set)
        {
          //if there is no empty register then choose one to free
          //1. Chose a non-dirty register
          //2. Chose the one which has a variable that is live the farthest
          if (!me.getValue().getRegStatus())
          {
            free(me.getKey());
            reg = me.getKey();
            noFreeRegNoDirty = false;
            break;
          }
        }    
      }

      //if all registers are not empty and have dirty values and are equally farthest used
      //chose smallest number register
      if (noFreeReg && noFreeRegNoDirty)
      {        
        if(lastFreedReg.equals("r0"))
        {
          free("r1");
			    reg = "r1";
          lastFreedReg = "r1";
        }
        else if(lastFreedReg.equals("r1"))
        {
          free("r2");
			    reg = "r2";
          lastFreedReg = "r2";
        }
        else if(lastFreedReg.equals("r2"))
        {
          free("r3");
			    reg = "r3";
          lastFreedReg = "r3";
        }
        else if(lastFreedReg.equals("r3"))
        {
          free("r0");
			    reg = "r0";
          lastFreedReg = "r0";
        }
        //System.out.println("ALLOCATE: Forced removal of reg: "+reg);             
      }

       //set the register allocator with appropriate values
      // if (!regAlloc.containsKey(reg))
      regAlloc.put(reg, new RegisterAllocator(opr, false));
   
       //regAlloc.get(reg).setRegValue(opr);
       //regAlloc.get(reg).setRegStatus(false);

    //System.out.println("REGISTER 0 VALUE: "+ regAlloc.get("r0").getRegValue()+ " REGISTER 0 STATUS: "+ regAlloc.get("r0").getRegStatus());
    //System.out.println("REGISTER 1 VALUE: "+ regAlloc.get("r1").getRegValue()+ " REGISTER 1 STATUS: "+ regAlloc.get("r1").getRegStatus());
    //System.out.println("REGISTER 2 VALUE: "+ regAlloc.get("r2").getRegValue()+ " REGISTER 2 STATUS: "+ regAlloc.get("r2").getRegStatus());
    //System.out.println("REGISTER 3 VALUE: "+ regAlloc.get("r3").getRegValue()+ " REGISTER 3 STATUS: "+ regAlloc.get("r3").getRegStatus());

      return reg;
      
    }

//YJ--STEP7: Bottom-up register allocation algorithm
   public static void botUpRegAllocation(String operator, String operand1, String operand2, String result)
   {
     String Rx = ensure(operand1);
     String Ry = ensure(operand2);

     if (!liveSet.contains(operand1))
       free (Rx);
     
     if (!liveSet.contains(operand2))
       free (Ry);

     String Rz = allocate(result);

     //Generate Tiny Code

     //mark Rz dirty
     regAlloc.get(Rz).setRegStatus(true);

/*
     //at end of BB, for each dirty register generate code to store regiter into appropriate variable
     Set<Map.Entry<String, RegisterAllocator>> set = regAlloc.entrySet();
     for(Map.Entry<String, RegisterAllocator> me : set)
     {
       if (me.getValue().getRegStatus())
       {
          TINY_LIST.add(new TinyInstructionNode("move", me.getKey(), me.getValue().getRegValue())); 
       }
     }       
*/
   }


	
//YJ -- Changed printsymbol map to this to identify initial global variable decls
    public static void identifyVars(SymbolMap map){ 

         SymbolMap temp = new SymbolMap();
         temp = map;
         
         // temp is SymbolMap object. temp.map gives access to LinkedHashMap object.
         Iterator<String> it = temp.map.keySet().iterator();         
         
         while(it.hasNext()){
            String var_type = it.next();
            if(temp.map.get(var_type).getType() == "STRING") {
               //System.out.println("name "+ temp.map.get(var_type).getName() + " type STRING" + " value " + temp.map.get(var_type).getValue());
            }
            else {
               VAR_LIST.add(temp.map.get(var_type).getName());
 
            }            
         }
         
   }

   // For use in IR Code generation
   public static String makeTemporary(){
       
      String temporary = "$T" + Integer.toString(current_temporay_register);
      current_temporay_register++;
      return temporary;
   }

//YJ: IR to tiny operands conversion

public static String irToTinyOpCoverter(String operand)
{
	String convertedOperand = "";
	//do nothing if var
	if (operand.indexOf("$") == -1)
		convertedOperand = operand;

	//temp to Reg
    else if (operand.indexOf("$T") != -1)
    	convertedOperand = tempToRegister(operand); 
	
	//local to mem offset
	else if (operand.indexOf("$L") != -1)
		convertedOperand = localToMem(operand);
	
	//param to mem offset
	else if (operand.indexOf("$P") != -1)
        convertedOperand = paramToMem(operand);

    return convertedOperand;
}  
 
//YJ: find type of a variable
public static String varToType(String varName)
{
	  SymbolMap temp;
    temp = FunctionSemanticHandler.symbolTables.get(currentFuncName);
    if(temp.map.keySet().size()==0){
       temp = FunctionSemanticHandler.symbolTables.get("GLOBAL");
    }
    String varType = "";
    SymbolMap my = SymbolTable.peekScope();

    // temp is SymbolMap object. temp.map gives access to LinkedHashMap object.
    Iterator<String> iter = temp.map.keySet().iterator();         
         
    while(iter.hasNext())
    {
      String var_type = iter.next();
      if (temp.map.get(var_type).getName().equals(varName))
      {
        varType = temp.map.get(var_type).getType();
      }	
    }

    return varType;
}

 //YJ -- Get function parameter count

    public static int funcParamCount(String funcName)
    {
      SymbolMap temp;
      temp = FunctionSemanticHandler.symbolTables.get(funcName);
      tinyCurrentMap = temp;
      int counter = 0;   
      // temp is SymbolMap object. temp.map gives access to LinkedHashMap object.
      Iterator<String> iter = temp.map.keySet().iterator();         
         
      while(iter.hasNext())
      {
        String var_type = iter.next();
        if (temp.map.get(var_type).getName().indexOf("$P") != -1)
        {
          counter = counter +1;
        }	
       }   
       
       return counter;
    }

  
//YJ -- will convert temps to regs but will do nothing to vars
	public static String tempToRegister(String temp)
	{
    String register;
    register = ensure(temp);

    /* This is for old cases
		if (temp.indexOf("$") == -1)
			return temp;			

		String tempNumber = temp.substring(temp.indexOf("T")+1);
		int regNumber = Integer.parseInt(tempNumber) - 1;
		String register = "r" + Integer.toString(regNumber); 
		//Register Tracker
		tinyRegAllocTracker = regNumber;
    */
		return register;
	}
//YJ-- will convert function params to stack offsets (memory locations)
	public static String paramToMem(String temp)
	{
		if (temp.indexOf("$") == -1)
			return temp;			

		String tempNumber = temp.substring(temp.indexOf("P")+1);
		int memOffSet =  6 + currentFuncParams - Integer.parseInt(tempNumber);
		String memLoc = "$" + Integer.toString(memOffSet);
		return memLoc; 
	}

//YJ-- will convert function local variables to stack offsets (memory locations)
	public static String localToMem(String temp)
	{
		//if (temp.indexOf("$") == -1)
			//return temp;			

		String tempNumber = temp.substring(temp.indexOf("L")+1);
		int memOffSet = Integer.parseInt(tempNumber);
		String memLoc = "$-"+Integer.toString(memOffSet);
		return memLoc; 
	}

	//YJ -- Convert the 3AC to Tiny Code and store in a separate data structure
  //YJ - step6
  public static void tinyInterpreter(ArrayList<IRNode> irList)
  {
    //YJ--step7: Bottom up vars
    String Rx = "";
    String Ry = "";
    String Rz = "";

    //System.out.println("REGISTER 0 VALUE: "+ regAlloc.get("r0").getRegValue()+ " REGISTER 0 STATUS: "+ regAlloc.get("r0").getRegStatus());
    //System.out.println("REGISTER 1 VALUE: "+ regAlloc.get("r1").getRegValue()+ " REGISTER 1 STATUS: "+ regAlloc.get("r1").getRegStatus());
    //System.out.println("REGISTER 2 VALUE: "+ regAlloc.get("r2").getRegValue()+ " REGISTER 2 STATUS: "+ regAlloc.get("r2").getRegStatus());
    //System.out.println("REGISTER 3 VALUE: "+ regAlloc.get("r3").getRegValue()+ " REGISTER 3 STATUS: "+ regAlloc.get("r3").getRegStatus());

		// YJ-step6
    int size = irList.size();
    int i=0; 
    String tempReg="";

    SymbolMap current_map;

    for(; i<size ; i++){
    // YJ-step6    
    IRNode node = irList.get(i);

    //YJ-step7: print the outset of IRnode
    
    //System.out.println("INSTRUCTION (IR node): ");
    //node.printIRNode();

    //YJ-step7: current node is out set is current live set

    liveSet = node.OUT; 
 
    //System.out.println(" OUT Set:= "+ liveSet);

    //node.printIRNode();
    char holding_float_or_int = 'i';
    //System.out.println("I am here");

    if ( (node.getOpcode().equals("STOREI")) || (node.getOpcode().equals("STOREF")) )
	  {
  
	  if ( (node.getResult().indexOf("$T")) != -1 )
	  {
      //allocate register for result
      Rz = 	allocate(node.getResult());
	    TINY_LIST.add(new TinyInstructionNode("move", node.getOperand1(), Rz));
	  }
	  else if ( (node.getOperand1().indexOf("$T")) != -1 && node.getResult().indexOf("$L") == -1 )
	  {	
      //ensure if operand 1 is in register
      Rx = ensure(node.getOperand1());

	  	if (node.getResult().equals("$R"))
        {
          TINY_LIST.add(new TinyInstructionNode("move", Rx, "$"+Integer.toString(6+currentFuncParams)));
        }
	  	// the result should not be equalto local variables they should be handled in the next coming blocks
	  	else
          TINY_LIST.add(new TinyInstructionNode("move", Rx, node.getResult()));
        
      }    
    //NUM->APPROX ISSUE step5 test_for
      else if (node.getResult().indexOf("$") == -1 && node.getOperand1().indexOf("$") == -1)
      {
        //ensure if operand 1 is in register
        Rx = ensure(node.getOperand1());
        //allocate register for result
       // Rz = 	allocate(node.getResult());
  
      	//all of the fields of IR nodes have variables

        //tempReg = tempToRegister(makeTemporary());
	      //TINY_LIST.add(new TinyInstructionNode("move", node.getOperand1(), tempReg));
        TINY_LIST.add(new TinyInstructionNode("move", Rx, node.getResult()));
      }
      else if (node.getOperand1().indexOf("$L") != -1)
      {
        //ensure if operand 1 is in register
        Rx = ensure(node.getOperand1());
      	//tempReg = Integer.toString(tinyRegAllocTracker+1);
      	// we have local variable in operand 1 of the IR node and second operand has $R for sure
      	//TINY_LIST.add(new TinyInstructionNode("move", localToMem(node.getOperand1()), "r"+tempReg));
        TINY_LIST.add(new TinyInstructionNode("move", Rx, "$"+Integer.toString(6+currentFuncParams)));

      }
      else if (node.getResult().indexOf("$L") != -1)
      {
        //ensure if operand 1 is in register
        Rx = ensure(node.getOperand1()); 
      	// we have a local variable in operand 2 of the IR node and operand1 has a temporary
        TINY_LIST.add(new TinyInstructionNode("move", Rx, localToMem(node.getResult())));

      }
     
      //Free all dead variables
      freeDeadVars(node.getOperand1());

      //System.out.println("REGISTER 0 VALUE: "+ regAlloc.get("r0").getRegValue()+ " REGISTER 0 STATUS: "+ regAlloc.get("r0").getRegStatus());
      //System.out.println("REGISTER 1 VALUE: "+ regAlloc.get("r1").getRegValue()+ " REGISTER 1 STATUS: "+ regAlloc.get("r1").getRegStatus());
      //System.out.println("REGISTER 2 VALUE: "+ regAlloc.get("r2").getRegValue()+ " REGISTER 2 STATUS: "+ regAlloc.get("r2").getRegStatus());
      //System.out.println("REGISTER 3 VALUE: "+ regAlloc.get("r3").getRegValue()+ " REGISTER 3 STATUS: "+ regAlloc.get("r3").getRegStatus());
 

	}
		
    else if ( (node.getOpcode().equals("MULTI")) || (node.getOpcode().equals("MULTF")) )
    {

      //ensure operands
      //need only one reg, benefit provided by tiny Simulator
      Rx = ensure(node.getOperand1());
      Ry = ensure(node.getOperand2());

      //if operand1 and operand2 are dead, free the registers corresponding to Rx and Ry in the regAlloc
      if (!isLive(node.getOperand1()))
        free(Rx);
      if(!isLive(node.getOperand2()))
        free(Ry);

      //allocate register for result
      //Rz = allocate(node.getResult());
			
      //generate code for operator MUL				
      if((node.getOpcode().equals("MULTF")))
        holding_float_or_int = 'r';      
      else
        holding_float_or_int = 'i';
      
       //old statements kept for reference                                                
      //TINY_LIST.add(new TinyInstructionNode("move", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getResult())));
	    //TINY_LIST.add(new TinyInstructionNode("mul"+String.valueOf(holding_float_or_int), irToTinyOpCoverter(node.getOperand2()), irToTinyOpCoverter(node.getResult())));

      //TINY_LIST.add(new TinyInstructionNode("move", Rx, Rz));
	    TINY_LIST.add(new TinyInstructionNode("mul"+String.valueOf(holding_float_or_int), Ry, Rx));

      //mark Register corresponding to Result dirty and change owner of Rx from operand to result
      regAlloc.get(Rx).setRegStatus(true);
      regAlloc.get(Rx).setRegValue(node.getResult());
      
    }		

    else if ( (node.getOpcode().equals("ADDI")) || (node.getOpcode().equals("ADDF")) )
    {
      //ensure operands
      //need only one reg, benefit provided by tiny Simulator
      Rx = ensure(node.getOperand1());
      Ry = ensure(node.getOperand2());

      //if operand1 and operand2 are dead, free the registers corresponding to Rx and Ry in the regAlloc
      if (!isLive(node.getOperand1()))
        free(Rx);
      if(!isLive(node.getOperand2()))
        free(Ry);

      //allocate register for result
      //Rz = allocate(node.getResult());
			
      //generate code for operator MUL				
      if((node.getOpcode().equals("ADDF")))
        holding_float_or_int = 'r';      
      else
        holding_float_or_int = 'i';
      
       //old statements kept for reference                                                
      //TINY_LIST.add(new TinyInstructionNode("move", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getResult())));
	    //TINY_LIST.add(new TinyInstructionNode("mul"+String.valueOf(holding_float_or_int), irToTinyOpCoverter(node.getOperand2()), irToTinyOpCoverter(node.getResult())));

      //TINY_LIST.add(new TinyInstructionNode("move", Rx, Rz));
	    TINY_LIST.add(new TinyInstructionNode("add"+String.valueOf(holding_float_or_int), Ry, Rx));

      //mark Register corresponding to Result dirty and change owner of Rx from operand to result
      regAlloc.get(Rx).setRegStatus(true);
      regAlloc.get(Rx).setRegValue(node.getResult());
    }

    else if ( (node.getOpcode().equals("DIVI")) || (node.getOpcode().equals("DIVF")) )
    {
      //ensure operands
      //need only one reg, benefit provided by tiny Simulator
      Rx = ensure(node.getOperand1());
      Ry = ensure(node.getOperand2());

      //if operand1 and operand2 are dead, free the registers corresponding to Rx and Ry in the regAlloc
      if (!isLive(node.getOperand1()))
        free(Rx);
      if(!isLive(node.getOperand2()))
        free(Ry);

      //allocate register for result
      //Rz = allocate(node.getResult());
			
      //generate code for operator MUL				
      if((node.getOpcode().equals("DIVF")))
        holding_float_or_int = 'r';      
      else
        holding_float_or_int = 'i';
      
       //old statements kept for reference                                                
      //TINY_LIST.add(new TinyInstructionNode("move", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getResult())));
	    //TINY_LIST.add(new TinyInstructionNode("mul"+String.valueOf(holding_float_or_int), irToTinyOpCoverter(node.getOperand2()), irToTinyOpCoverter(node.getResult())));

      //TINY_LIST.add(new TinyInstructionNode("move", Rx, Rz));
	    TINY_LIST.add(new TinyInstructionNode("div"+String.valueOf(holding_float_or_int), Ry, Rx));

      //mark Register corresponding to Result dirty and change owner of Rx from operand to result
      regAlloc.get(Rx).setRegStatus(true);
      regAlloc.get(Rx).setRegValue(node.getResult());

    }

    else if ( (node.getOpcode().equals("SUBI")) || (node.getOpcode().equals("SUBF")) )
    {
      //ensure operands
      //need only one reg, benefit provided by tiny Simulator
      Rx = ensure(node.getOperand1());
      Ry = ensure(node.getOperand2());

      //if operand1 and operand2 are dead, free the registers corresponding to Rx and Ry in the regAlloc
      if (!isLive(node.getOperand1()))
        free(Rx);
      if(!isLive(node.getOperand2()))
        free(Ry);

      //allocate register for result
      //Rz = allocate(node.getResult());
			
      //generate code for operator MUL				
      if((node.getOpcode().equals("SUBF")))
        holding_float_or_int = 'r';      
      else
        holding_float_or_int = 'i';
      
       //old statements kept for reference                                                
      //TINY_LIST.add(new TinyInstructionNode("move", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getResult())));
	    //TINY_LIST.add(new TinyInstructionNode("mul"+String.valueOf(holding_float_or_int), irToTinyOpCoverter(node.getOperand2()), irToTinyOpCoverter(node.getResult())));

      //TINY_LIST.add(new TinyInstructionNode("move", Rx, Rz));
	    TINY_LIST.add(new TinyInstructionNode("sub"+String.valueOf(holding_float_or_int), Ry, Rx));

      //mark Register corresponding to Result dirty and change owner of Rx from operand to result
      regAlloc.get(Rx).setRegStatus(true);
      regAlloc.get(Rx).setRegValue(node.getResult());

    }							


    else if ( node.getOpcode().equals("WRITEI") )
    {
      if (node.getResult().indexOf("$L") != -1)
      {
      	//we have a local variable to read
      	TINY_LIST.add(new TinyInstructionNode("sys", "writei", localToMem(node.getResult())));
      }
      else
      {	
        TINY_LIST.add(new TinyInstructionNode("sys", "writei", node.getResult()));
      }
    }

    else if ( node.getOpcode().equals("WRITEF") )
    {
      if (node.getResult().indexOf("$L") != -1)
      {
      	//we have a local variable to read
      	TINY_LIST.add(new TinyInstructionNode("sys", "writer", localToMem(node.getResult())));
      }
      else
      {	
        TINY_LIST.add(new TinyInstructionNode("sys", "writer", node.getResult()));
      }
    }
    else if ( node.getOpcode().equals("WRITES") )
    {
      TINY_LIST.add(new TinyInstructionNode("sys", "writes", node.getResult()));
    }			

    else if ( node.getOpcode().equals("READI") )
    {
      if (node.getResult().indexOf("$L") != -1)
      {
      	//we have a local variable to read
      	TINY_LIST.add(new TinyInstructionNode("sys", "readi", localToMem(node.getResult())));
      }
      else
      {	
        TINY_LIST.add(new TinyInstructionNode("sys", "readi", node.getResult()));
      }
    }

    else if ( node.getOpcode().equals("READF") )
    {
      if (node.getResult().indexOf("$L") != -1)
      {
      	//we have a local variable to read
      	TINY_LIST.add(new TinyInstructionNode("sys", "readr", localToMem(node.getResult())));
      }
      else
      {	
        TINY_LIST.add(new TinyInstructionNode("sys", "readr", node.getResult()));
      }
    }

    //Control structure code generation
    
    else if (node.getOpcode().equals("LABEL"))
    {
      if (node.getOperand1().indexOf("label") != -1 || node.getResult().indexOf("label") != -1)
      {
      	//We dont have function label, we have a jump label
      	if (node.getResult().equals(""))
        TINY_LIST.add(new TinyInstructionNode("label", "", node.getOperand1()));
        else
        TINY_LIST.add(new TinyInstructionNode("label", "", node.getResult()));
      }
      else
      {     
        //This means that the label is of a function so initialize funcParamsCounter for that function
      	//System.out.println("IN LABEL 1ST IF*****");
      	if (node.getOperand1().equals(""))
      	{
          currentFuncParams = funcParamCount(node.getResult());
          TINY_LIST.add(new TinyInstructionNode("label", "", node.getResult()));
      	}
      	else
      	{	
      	  currentFuncParams = funcParamCount(node.getOperand1());
      	  currentFuncName = node.getOperand1();
      	  TINY_LIST.add(new TinyInstructionNode("label", "", node.getOperand1()));
      	  current_map = tinyCurrentMap;
      	}
      	//System.out.println("IN LABEL 1ST IF+++++");
      }
    }


    else if (node.getOpcode().equals("JUMP"))
    {
    	TINY_LIST.add(new TinyInstructionNode("jmp", "", node.getResult()));
    }

    else if (node.getOpcode().equals("EQ"))
    {
      if ((node.getOperand1().indexOf("$T") == -1) && (node.getOperand2().indexOf("$T") == -1))
      {
        //It means that both operands don't have a temporary
        //so simply do an ensure on operand 2
        if (varToType(node.getOperand1()).equals("INT"))    	
    	  TINY_LIST.add(new TinyInstructionNode("cmpi", irToTinyOpCoverter(node.getOperand1()), ensure(irToTinyOpCoverter(node.getOperand2()))));
        else 
      	  TINY_LIST.add(new TinyInstructionNode("cmpr", irToTinyOpCoverter(node.getOperand1()), ensure(irToTinyOpCoverter(node.getOperand2()))));
       }
       else
       {
         if (varToType(node.getOperand1()).equals("INT"))    	
    	     TINY_LIST.add(new TinyInstructionNode("cmpi", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getOperand2())));
         else 
      	   TINY_LIST.add(new TinyInstructionNode("cmpr", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getOperand2())));
       }     
     
      TINY_LIST.add(new TinyInstructionNode("jeq", "", node.getResult()));
    }

    else if (node.getOpcode().equals("GE"))
    {
      
      if ((node.getOperand1().indexOf("$T") == -1) && (node.getOperand2().indexOf("$T") == -1))
      {
        //It means that both operands don't have a temporary
        //so simply do an ensure on operand 2
        if (varToType(node.getOperand1()).equals("INT"))    	
    	  TINY_LIST.add(new TinyInstructionNode("cmpi", irToTinyOpCoverter(node.getOperand1()), ensure(irToTinyOpCoverter(node.getOperand2()))));
        else 
      	  TINY_LIST.add(new TinyInstructionNode("cmpr", irToTinyOpCoverter(node.getOperand1()), ensure(irToTinyOpCoverter(node.getOperand2()))));
       }
       else
       {
         if (varToType(node.getOperand1()).equals("INT"))    	
    	     TINY_LIST.add(new TinyInstructionNode("cmpi", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getOperand2())));
         else 
      	   TINY_LIST.add(new TinyInstructionNode("cmpr", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getOperand2())));
       }     

      TINY_LIST.add(new TinyInstructionNode("jge", "", node.getResult()));
    }

    else if (node.getOpcode().equals("LE"))
    {
      if ((node.getOperand1().indexOf("$T") == -1) && (node.getOperand2().indexOf("$T") == -1))
      {
        //It means that both operands don't have a temporary
        //so simply do an ensure on operand 2
        if (varToType(node.getOperand1()).equals("INT"))    	
    	  TINY_LIST.add(new TinyInstructionNode("cmpi", irToTinyOpCoverter(node.getOperand1()), ensure(irToTinyOpCoverter(node.getOperand2()))));
        else 
      	  TINY_LIST.add(new TinyInstructionNode("cmpr", irToTinyOpCoverter(node.getOperand1()), ensure(irToTinyOpCoverter(node.getOperand2()))));
       }
       else
       {
         if (varToType(node.getOperand1()).equals("INT"))    	
    	     TINY_LIST.add(new TinyInstructionNode("cmpi", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getOperand2())));
         else 
      	   TINY_LIST.add(new TinyInstructionNode("cmpr", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getOperand2())));
       }     

      TINY_LIST.add(new TinyInstructionNode("jle", "", node.getResult()));
    }	

    else if (node.getOpcode().equals("GT"))
    {
      if ((node.getOperand1().indexOf("$T") == -1) && (node.getOperand2().indexOf("$T") == -1))
      {
        //It means that both operands don't have a temporary
        //so simply do an ensure on operand 2
        if (varToType(node.getOperand1()).equals("INT"))    	
    	  TINY_LIST.add(new TinyInstructionNode("cmpi", irToTinyOpCoverter(node.getOperand1()), ensure(irToTinyOpCoverter(node.getOperand2()))));
        else 
      	  TINY_LIST.add(new TinyInstructionNode("cmpr", irToTinyOpCoverter(node.getOperand1()), ensure(irToTinyOpCoverter(node.getOperand2()))));
       }
       else
       {
         if (varToType(node.getOperand1()).equals("INT"))    	
    	     TINY_LIST.add(new TinyInstructionNode("cmpi", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getOperand2())));
         else 
      	   TINY_LIST.add(new TinyInstructionNode("cmpr", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getOperand2())));
       }     

      TINY_LIST.add(new TinyInstructionNode("jgt", "", node.getResult()));
    }	

    else if (node.getOpcode().equals("LT"))
    {
      if ((node.getOperand1().indexOf("$T") == -1) && (node.getOperand2().indexOf("$T") == -1))
      {
        //It means that both operands don't have a temporary
        //so simply do an ensure on operand 2
        if (varToType(node.getOperand1()).equals("INT"))    	
    	  TINY_LIST.add(new TinyInstructionNode("cmpi", irToTinyOpCoverter(node.getOperand1()), ensure(irToTinyOpCoverter(node.getOperand2()))));
        else 
      	  TINY_LIST.add(new TinyInstructionNode("cmpr", irToTinyOpCoverter(node.getOperand1()), ensure(irToTinyOpCoverter(node.getOperand2()))));
       }
       else
       {
         if (varToType(node.getOperand1()).equals("INT"))    	
    	     TINY_LIST.add(new TinyInstructionNode("cmpi", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getOperand2())));
         else 
      	   TINY_LIST.add(new TinyInstructionNode("cmpr", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getOperand2())));
       }     

      TINY_LIST.add(new TinyInstructionNode("jlt", "", node.getResult()));
    }	

    else if (node.getOpcode().equals("NE"))
    {
      //System.out.println("*****TYPE IS: "+ varToType(node.getOperand1()));
      
      if ((node.getOperand1().indexOf("$T") == -1) && (node.getOperand2().indexOf("$T") == -1))
      {
        //It means that both operands don't have a temporary
        //so simply do an ensure on operand 2
        if (varToType(node.getOperand1()).equals("INT"))    	
    	  TINY_LIST.add(new TinyInstructionNode("cmpi", irToTinyOpCoverter(node.getOperand1()), ensure(irToTinyOpCoverter(node.getOperand2()))));
        else 
      	  TINY_LIST.add(new TinyInstructionNode("cmpr", irToTinyOpCoverter(node.getOperand1()), ensure(irToTinyOpCoverter(node.getOperand2()))));
       }
       else
       {
         if (varToType(node.getOperand1()).equals("INT"))    	
    	     TINY_LIST.add(new TinyInstructionNode("cmpi", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getOperand2())));
         else 
      	   TINY_LIST.add(new TinyInstructionNode("cmpr", irToTinyOpCoverter(node.getOperand1()), irToTinyOpCoverter(node.getOperand2())));
       }     

      TINY_LIST.add(new TinyInstructionNode("jne", "", node.getResult()));
      
    }
 

    // FUNCTION call code generation

    else if (node.getOpcode().equals("LINK"))
    {
      TINY_LIST.add(new TinyInstructionNode("link", "", node.getResult()));
    }
    else if (node.getOpcode().equals("RET"))
    {
      //First clear current func params	
      //currentFuncParams = 0;
      //Then add unlnk and ret instructions
      TINY_LIST.add(new TinyInstructionNode("unlnk", "", ""));
      TINY_LIST.add(new TinyInstructionNode("ret", "", ""));
      if(currentFuncName.equals("main"))
      {
      	TINY_LIST.add(new TinyInstructionNode("end", "", ""));
      }
    }

    else if (node.getOpcode().equals("PUSH"))
    {
      if (node.getOperand1().equals(""))
      {	
        TINY_LIST.add(new TinyInstructionNode("push", "", ""));
      }
      else if (node.getOperand1().indexOf("$L") != -1)
      {
      	//push local variable
      	TINY_LIST.add(new TinyInstructionNode("push", localToMem(node.getOperand1()), ""));
      }
      else if (node.getOperand1().indexOf("$T") != -1)
      {
      	//push temp as register
      	TINY_LIST.add(new TinyInstructionNode("push", tempToRegister(node.getOperand1()), ""));
      }
    }

    else if (node.getOpcode().equals("POP"))
    {
      if (node.getOperand1().equals(""))
      {	
        TINY_LIST.add(new TinyInstructionNode("pop", "", ""));
      }
      else if (node.getOperand1().indexOf("$L") != -1)
      {
      	//push local variable
      	TINY_LIST.add(new TinyInstructionNode("pop", localToMem(node.getOperand1()), ""));
      }
      else if (node.getOperand1().indexOf("$T") != -1)
      {
      	//push temp as register
      	TINY_LIST.add(new TinyInstructionNode("pop", tempToRegister(node.getOperand1()), ""));
      }
    }

    else if (node.getOpcode().equals("JSR"))
    {
      //fixed pattern of pushing 4 regs
      TINY_LIST.add(new TinyInstructionNode("push", "r0", ""));
      TINY_LIST.add(new TinyInstructionNode("push", "r1", ""));
      TINY_LIST.add(new TinyInstructionNode("push", "r2", ""));
      TINY_LIST.add(new TinyInstructionNode("push", "r3", ""));
      //jsr function name
      TINY_LIST.add(new TinyInstructionNode("jsr", node.getOperand1(), ""));
      //fixed pattern of popping 4 regs
      TINY_LIST.add(new TinyInstructionNode("pop", "r3", ""));
      TINY_LIST.add(new TinyInstructionNode("pop", "r2", ""));
      TINY_LIST.add(new TinyInstructionNode("pop", "r1", ""));
      TINY_LIST.add(new TinyInstructionNode("pop", "r0", ""));
    }	

    }    	

  }
   
   //TODO! Might need to use a hashmap to see which registers have already been used or so. Note that one IR instruction gets translated to multiple Tiny Instructions.

//YJ -- print function for tinyCode
   public static void printTinyCode(){
			int numVars = VAR_LIST.size();
			int tinySize = TINY_LIST.size();
			int i=0;
			int j=0;

      

			System.out.println(";tiny code");

            //printing global function declrations
            SymbolTable.printGlobalTiny();

            //Fixed printing if there are functions in IRcode. NOTE: need to change for other steps compatibility!!!
            System.out.println("push");
            System.out.println("push r0");
            System.out.println("push r1");
            System.out.println("push r2");
            System.out.println("push r3");
            System.out.println("jsr main");
            System.out.println("sys halt");	
/*
			for(; i<numVars; i++)
			{
				System.out.println("var "+VAR_LIST.get(i));
			}
*/			
			for (; j<tinySize; j++)
			{
				TINY_LIST.get(j).printTinyInstructionNode();
			}
   
			System.out.println("sys halt");

   }

//YJ -- print function for tinyCode
   public static void printTinyCodeGlobal(){
			int numVars = VAR_LIST.size();
			int tinySize = TINY_LIST.size();
			int i=0;
			int j=0;

			System.out.println(";tiny code");

			for(; i<numVars; i++)
			{
				System.out.println("var "+VAR_LIST.get(i));
			}

			//Fixed printing if there are functions in IRcode
            System.out.println("push");
            System.out.println("push r0");
            System.out.println("push r1");
            System.out.println("push r2");
            System.out.println("push r3");
            System.out.println("jsr main");
            System.out.println("sys halt");				

			for (; j<tinySize; j++)
			{
				TINY_LIST.get(j).printTinyInstructionNode();
			}
   
			//System.out.println("sys halt");

   }
 

}



