import java.util.*;

public class ControlFlowGraph{

   // Each IR node needs to have a successor and predecessor set. Defined that in IRNode class.
   // HashMap for Jumps and their labels:  SemanticRoutines.conditionalUnconditionalJumpTarget
   // Building per statement flow graph instead of per block.
   // Opcodes for Conditional and Unconditional Jumps
   private static Set<String> condUncondJumps = new HashSet<>(Arrays.asList("JUMP","GE","NE","LE","EQ","LT","GT"));
   // In result or IR node, these instructions define result. Will be used in KILL set generation
   private static Set<String> instructionsThatDefineResult = new HashSet<>(Arrays.asList   ("ADDI","ADDF","SUBI","SUBF","MULTI","MULTF","DIVI","DIVF","READI","READF","READS","STOREI","STOREF","STORES","POP"));
   
   private static Set<String> instructionsThatUseOperand = new HashSet<>(Arrays.asList   ("ADDI","ADDF","SUBI","SUBF","MULTI","MULTF","DIVI","DIVF","WRITEI","WRITEF","WRITES","STOREI","STOREF","STORES","PUSH","GE","NE","LE","EQ","LT","GT"));
   
   // Worklist for liveness analysis. Clear for every function
   // private static LinkedList<IRNode> workList = new LinkedList<IRNode>();

   public static void buildControlFlowGraph(){
      // Intializing IRNodes for statements.
      IRNode current = null;
      IRNode previous = null;
      IRNode next = null;
      
      // Traversring the Function IR List of every available function
      
      for(int i=0; i < FunctionSemanticHandler.currentActiveFunctions.size() ; i++){
         FunctionSymbolTable temp = FunctionSemanticHandler.currentActiveFunctions.get(i);
         for(int j=0; j < temp.functionIRList.size();j++){
            
            current = temp.functionIRList.get(j);
            // If the current statement is either kind of Jumps. Add target statement as Successor and current statement as their predecessors.
            // The case where the next statement in the code is added as successor will be handled separately.
            if(condUncondJumps.contains(current.getOpcode())){
                // In IRNode, result parameter contains label names.
                String target = current.getResult();
                // next statment obtained from Mapping
                next = SemanticRoutines.conditionalUnconditionalJumpTarget.get(target);
                current.successor.add(next);
                next.predecessor.add(current);
             
            }
            
            // Handling the above mentioned case. See NOTE 2 also for clarification.  RET does not have any successor but has a predecessor 
            if(previous != null && !previous.getOpcode().equals("JUMP") && !previous.getOpcode().equals("RET") ){
               if(current!=null){
                  previous.successor.add(current);
               }
               current.predecessor.add(previous);
            
            }
         
            // JSR Function call has no predecessor (Clear next statement predecessor). Only one successor node.
            if(current.getOpcode().equals("JSR")){
               String functionName = current.getResult();
               // next statment obtained from Mapping
               next = SemanticRoutines.conditionalUnconditionalJumpTarget.get(functionName);
               next.predecessor.clear();
              
            }
            
            
            // NOTE 2: Need this to assign for Conditional Jump Statements. e.g. if(a<b) a=b; else {..}
            // When we are at a=b. We need to add "if" statement as its predecessor
            previous = current;
            
        }
      } 
   
   }
 
   /* Takes in IR NODE and returns the GEN Set.
    Recall: Arguments of IR Node : String opcode, String operand1, String operand2, String result
    NOTE: String variables are also included in GEN and KILL Sets.
   */
   
   public static HashSet<String> GEN(IRNode statement){
      HashSet<String> genSet = new HashSet<String>();
      if(instructionsThatUseOperand.contains(statement.getOpcode())){
         String operand1 = statement.getOperand1();
         String operand2 = statement.getOperand2();
         if(!operand1.isEmpty() && isValidOperandForGenKillSet(operand1)){
            genSet.add(operand1);
         }
         
         if(!operand2.isEmpty() && isValidOperandForGenKillSet(operand2)){
            genSet.add(operand2);
         }
         
      }
       if(statement.getOpcode().contains("WRITE")){
         genSet.add(statement.getResult());
      }
      
      
      // JSR requires special handling. Its GEN Set is set of all global variables. addAll method used because a set is returned from argument
      if(statement.getOpcode().equals("JSR")){
         genSet.addAll(FunctionSemanticHandler.symbolTables.get("GLOBAL").map.keySet());
      
      }  
    


      return genSet;
   }
  
   // Takes IR NODE and returns the KILL Set. For JSR, kill set returned will be empty.
   public static HashSet<String> KILL(IRNode statement){
      HashSet<String> killSet = new HashSet<String>();
   
      if(instructionsThatDefineResult.contains(statement.getOpcode())){
         String result = statement.getResult();
         if(!result.isEmpty() && isValidOperandForGenKillSet(result)){
            killSet.add(result);
         }
      }

      return killSet;
   }
   
   /* Operands can be Variable or Temporay or Constants. We don't want to include Constants in GEN
    and KILL Set i.e. INT or FLOAT literals are not valid. All other $T,$R, $L , $P or IDENTIFIER(global scope) are valid */
   private static boolean isValidOperandForGenKillSet(String operand){
      if(operand.matches("[0-9]+") || operand.matches("[0-9]*\\.[0-9]+")){
         return false;
      } 
      else {
         return true;
      }
   }

   /* LIVENESS ANALYSIS: 1) IN(statement) = GEN(statement) U [ OUT(statement) - KILL(statement)]
                         2) OUT(statment) = Union(t E successors)[ IN(t) ]; Union spans all 't' belonging to successor
      Notes: If an instruction's IN set changes, all of the instruction's predecessors need to be added to the worklist.
      Implemented in a function below updateWorkList().
 
   */

   public static void livenessAnalysis(){
      IRNode current = null;
      FunctionSymbolTable temp;
      boolean signal = false;
      LinkedList<IRNode> workList = new LinkedList<IRNode>();
      boolean flag = true;
      int i,j;
      // Its a backward analysis. Statements are added in opposite order. e.g. RET of a func will be added first.
      for(i = FunctionSemanticHandler.currentActiveFunctions.size()-1; i>=0; i--){
         // Cleared for each new function.
         workList.clear();  
         temp = FunctionSemanticHandler.currentActiveFunctions.get(i);
         for(j = temp.functionIRList.size()-1; j>=0; j--){
            IRNode currentt = temp.functionIRList.get(j); 
            //RET handled special case. Intialize OUT set with Global vars.
            if(currentt.getOpcode().equals("RET")){
               if(flag){
                  flag=false;
                  continue;
               }
               currentt.OUT.addAll(FunctionSemanticHandler.symbolTables.get("GLOBAL").map.keySet());
            }
          
            workList.add(currentt);
         }
         // Now processing each node. Better flexibilty to use Iterator here. Becasue worklist will get updated and we don't know when this will converge. Idea from example in homework
         Iterator<IRNode> iter = workList.iterator();
         while(iter.hasNext()){
            //
            //System.out.println("I am not done");  
            //iter.next().printIRNode();
            IRNode curr = workList.remove();
            //curr.printIRNode();
            
            //Implementing equation 2 from above. FIRST OUT HAS TO BE COMPUTED. BECAUSE "BACKWARD ANALYSIS"
            Iterator<IRNode> it1 = curr.successor.iterator();
            while(it1.hasNext()){
               curr.OUT.addAll(it1.next().IN); //Union of IN sets of all successors
            }
          
             
            /* Implementing Eq 1 from above and if boolean true is returned from either operation means IN changed so call updateWorkList
               Operations in order. Also. From Set Theory (Wikipedia): C U (B-A) = (B U C) - (A - C)
               GEN U (OUT - KILL) = ( GEN U OUT ) - (KILL - GEN) : Since In liveness, GEN and KILL = Empty Set hence:
               GEN U (OUT - KILL) = ( GEN U OUT ) - KILL
               IN(s) = GEN(s) U OUT(s) - KILL(s)
            */
            HashSet<String> temporary = new HashSet<String>();
            temporary.addAll(curr.OUT);
            temporary.addAll(GEN(curr));
            temporary.removeAll(KILL(curr));
            // boolean changed = curr.IN.addAll(temporary);
            // If IN set changes then "true" is returned i.e. a new element is added.
            // System.out.println("Set before change:" + curr.IN);
            
            if(curr.IN.addAll(temporary)){
               //System.out.println("changed set:" + curr.IN);
               updateWorkList(curr, workList);
            }
            
            /*             
            if(curr.IN.addAll(curr.OUT)){
               signal=true;
            }
            if(curr.IN.addAll(GEN(curr))){
               signal=true;
            }
            
              
            
            // removeAll means "Difference in Sets"
            if(curr.IN.removeAll(KILL(curr))){
               signal = true;
            }
            */
             
          
         }
          
      }
   
   }

   
   // Update WorkList:
   public static void updateWorkList(IRNode instruction, LinkedList<IRNode> workList){
      // Need to iterate whole set of IR Nodes and make changes.
      Iterator<IRNode> it = instruction.predecessor.iterator();
      while(it.hasNext()){
         IRNode current = it.next();
         if(!workList.contains(current)){
            workList.add(current);            
         }
         else {
         }
      }
   }



    // Print Control Flow Graph Utility
   public static void printCFG(){ 
      //instructionsThatDefineResult.removeAll(instructionsThatUseOperand) ;
      //System.out.println(";SetDifference:"+ instructionsThatDefineResult);
    
      //YJ-STEP7: They are called in printIRCode FunctionSemanticHandler
      //buildControlFlowGraph();
      //livenessAnalysis();
      FunctionSymbolTable temp;
      System.out.println(";Control Flow Graph");
      int i=0;
      for(; i<FunctionSemanticHandler.currentActiveFunctions.size() ; i++){
         temp = FunctionSemanticHandler.currentActiveFunctions.get(i);
         for(int j=0; j< temp.functionIRList.size();j++){
            IRNode node = temp.functionIRList.get(j);
            System.out.println("INSTRUCTION (IR node): ");
						node.printIRNode(); 
            System.out.print("Predecessor Set: ");
            printSet(node.predecessor);
            System.out.print("Successor Set: ");
            printSet(node.successor);
            System.out.println(" KILL Set is "+ KILL(node));
            System.out.println(" GEN Set is "+ GEN(node));
            System.out.println(" IN Set is "+ node.IN);
            System.out.println(" OUT Set is "+ node.OUT);
            System.out.println();
            System.out.println();
        }
      }
      //Calling print TinyCode here for step4 output
      //tinyInterpreter(map);
      //printTinyCode();
      
   }
   public static void printSet(Set<IRNode> set){
      Iterator<IRNode> it = set.iterator();
      System.out.print("{");
      while(it.hasNext()){
         it.next().printIRNode();
         System.out.print("  ");
         
      }
      System.out.print("} ");
    
   }




}
