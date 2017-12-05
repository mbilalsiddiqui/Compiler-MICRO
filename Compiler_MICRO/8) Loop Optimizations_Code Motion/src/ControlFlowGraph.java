import java.util.*;

public class ControlFlowGraph{
   // FOR Debugging print statements
   static boolean  DEBUG = false;
   // Special boolean value
   static boolean DO_CODE_MOTION = true;
   // Each IR node needs to have a successor and predecessor set. Defined that in IRNode class.
   // HashMap for Jumps and their labels:  SemanticRoutines.conditionalUnconditionalJumpTarget
   // Building per statement flow graph instead of per block.
   // Opcodes for Conditional and Unconditional Jumps
   private static Set<String> condUncondJumps = new HashSet<>(Arrays.asList("JUMP","GE","NE","LE","EQ","LT","GT"));
   // In result or IR node, these instructions define result. Will be used in KILL set generation
   private static Set<String> instructionsThatDefineResult = new HashSet<>(Arrays.asList   ("ADDI","ADDF","SUBI","SUBF","MULTI","MULTF","DIVI","DIVF","READI","READF","READS","STOREI","STOREF","STORES","POP"));
   
   private static Set<String> instructionsThatUseOperand = new HashSet<>(Arrays.asList   ("ADDI","ADDF","SUBI","SUBF","MULTI","MULTF","DIVI","DIVF","WRITEI","WRITEF","WRITES","STOREI","STOREF","STORES","PUSH","GE","NE","LE","EQ","LT","GT"));

   
   // CODE MOTION Inits: Need to skip these statement for determining the block of IR statements for a single code statement: e.g. a=b*c + 8 
   // SemanticRoutines.temporaryToConstantMap
   public static LoopInfo loopInformation = null;
   private static Set<String> skipForSingleStatement = new HashSet<>(Arrays.asList   ("LABEL","JUMP","GE","NE","LE","EQ","LT","GT","WRITEI","WRITES","WRITEF","RET","LINK","JSR","PUSH","POP","READI","READF","READS"));
   private static Set<String> arithmeticOperations = new HashSet<>(Arrays.asList   ("ADDI","ADDF","SUBI","SUBF","MULTI","MULTF","DIVI","DIVF"));

   static int offset = 0;
   static int ending = 0;
    //HashMap containing info regarding local variables; Key is the local variable name
   static HashMap<String, CodeStatementInfo> localVar = new LinkedHashMap<String, CodeStatementInfo>();
   // Used to check which Temporaries contain Constants. For CODE MOTION.
   static HashMap<String,String> temporaryToConstantMap = new HashMap<String,String>();
   
   // Sets for containing all definitions of whole program for Reaching Def Analysis.
   public static HashSet<ReachingDefAnalysisNode> allDefs = new LinkedHashSet<ReachingDefAnalysisNode>();
   // This will have all sets of a particular variable or a temporary.
   public static HashMap<String, HashSet<ReachingDefAnalysisNode>> mapping = new HashMap<String, HashSet<ReachingDefAnalysisNode>>();
   

   public static void buildControlFlowGraph(){
      // Intializing IRNodes for statements.
      IRNode current = null;
      IRNode previous = null;
      IRNode next = null;
      
      // Traversring the Function IR List of every available function
      //System.out.println("THE LIST SIZE IS " + FunctionSemanticHandler.currentActiveFunctions.size() );
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
                //DEBUG!TODO..........
                if(DEBUG && current.getOpcode().equals("JUMP")){ 
                   System.out.println("Checking Jumps Successor and Predecessor Sets");
                   printSet(current.successor); 
                   printSet(next.predecessor); 
                   
                }
             
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
   // For conditional jumps. Op1 should not be used. 
   public static HashSet<String> GEN(IRNode statement){
      HashSet<String> genSet = new HashSet<String>();
      if(instructionsThatUseOperand.contains(statement.getOpcode())){
         String operand1 = statement.getOperand1();
         String operand2 = statement.getOperand2();
         //if(!operand1.isEmpty() && isValidOperandForGenKillSet(operand1) ){
         
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
      LinkedList<IRNode> workList = new LinkedList<IRNode>();
      int count = 1;

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
               if(count==1){
                  count=0;
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

/*------------------------------- Reaching Definition Analysis. NOTE: It does not handle cross function calls -----------------------------*/
   
   // Takes IR NODE and returns the GEN Set for Reaching DEF. 
   public static HashSet<ReachingDefAnalysisNode> setReachingDefGEN(IRNode statement, int lineNum){
      HashSet<ReachingDefAnalysisNode> reachingGenSet = new HashSet<ReachingDefAnalysisNode>();
   
      if(instructionsThatDefineResult.contains(statement.getOpcode())){
         String result = statement.getResult();
         if(!result.isEmpty() && isValidOperandForGenKillSet(result)){
             reachingGenSet.add(new ReachingDefAnalysisNode(result,lineNum+1));
         }
      }

      return reachingGenSet;
   }
   // Takes IR NODE and returns the KILL Set for Reaching DEF. 
   public static HashSet<ReachingDefAnalysisNode> setReachingDefKILL(IRNode statement, int lineNum){
      HashSet<ReachingDefAnalysisNode> reachingKillSet = new HashSet<ReachingDefAnalysisNode>();
   
      if(instructionsThatDefineResult.contains(statement.getOpcode())){
         String result = statement.getResult();
         if(!result.isEmpty() && isValidOperandForGenKillSet(result)){
             ReachingDefAnalysisNode temp = new ReachingDefAnalysisNode(result,lineNum+1);
             // Both methods will work. Either work with deep copy or use choose unchoose strategy.
             // HashMap<String, HashSet<ReachingDefAnalysisNode>> currentSet = cloneMap(mapping);
             // UnChoose defined set
             mapping.get(result).remove(temp);
             //currentSet.get(result).remove(temp);
             reachingKillSet.addAll(mapping.get(result));
             //reachingKillSet.addAll(currentSet.get(result));
             
             // Choose back defined set
             mapping.get(result).add(temp);
           
          }
      }

      return reachingKillSet;
   }
   // to create a deep copy  
   public static HashMap<String, HashSet<ReachingDefAnalysisNode>> cloneMap(HashMap<String, HashSet<ReachingDefAnalysisNode>> map){
     HashMap<String, HashSet<ReachingDefAnalysisNode>> currentSet = new HashMap<String, HashSet<ReachingDefAnalysisNode>>();
      for(Map.Entry<String, HashSet<ReachingDefAnalysisNode>> entry: map.entrySet()){
         currentSet.put(entry.getKey(), new HashSet<ReachingDefAnalysisNode>(entry.getValue()));
        
      }     
      return currentSet;
    
   }


   public static void initializeDefSet(){
      IRNode current = null;
      for(int i=0; i < FunctionSemanticHandler.currentActiveFunctions.size() ; i++){
         FunctionSymbolTable temp = FunctionSemanticHandler.currentActiveFunctions.get(i);
         for(int j=0; j < temp.functionIRList.size();j++){
            current = temp.functionIRList.get(j);
            if(instructionsThatDefineResult.contains(current.getOpcode())){
                String result = current.getResult();
                if(!result.isEmpty() && isValidOperandForGenKillSet(result)){
                   ReachingDefAnalysisNode temporary = new ReachingDefAnalysisNode(result,j+1);
                   allDefs.add(temporary);
                   if(!mapping.containsKey(result)){
                      HashSet<ReachingDefAnalysisNode> currentSet = new HashSet<ReachingDefAnalysisNode>();
                      currentSet.add(temporary);
                      mapping.put(result, currentSet);
                   }
                   else {
                      mapping.get(result).add(temporary);
                   }
                }
            }
      
         
         }
      }
   }

   public static void reachingDefAnalysis(){
      // Initializing GEN and KILl sets here because I don't want to save line numbers in IR node.
      intializeGENandKILLforReachingDef();
      IRNode current = null;
      FunctionSymbolTable temp;
      LinkedList<IRNode> workList = new LinkedList<IRNode>();
      boolean change = false;
      int i,j;
      // Its a Forward analysis. Statements are added in opposite order. e.g. RET of a func will be added first.
      for(i = 0; i<FunctionSemanticHandler.currentActiveFunctions.size(); i++){
         // Cleared for each new function.
         workList.clear();  
         temp = FunctionSemanticHandler.currentActiveFunctions.get(i);
         for(j = 0; j<temp.functionIRList.size(); j++){
            IRNode currentt = temp.functionIRList.get(j); 
            //Initialize OUT set as empty for RET??
            if(currentt.getOpcode().equals("RET")){
               currentt.ReachingOUT.clear();
            }
          
            workList.add(currentt);
         }
         // Now processing each node. We don't know when this will converge.
         Iterator<IRNode> iter = workList.iterator();
         while(iter.hasNext()){
            //
            //System.out.println("I am not done");  
            //iter.next().printIRNode(); 
             change = false;
              
            IRNode curr = workList.remove();
            //curr.printIRNode();
            
            //Implementing equation 1 from Dataflow analysis. FIRST IN HAS TO BE COMPUTED. BECAUSE "FORWARD ANALYSIS" 
            Iterator<IRNode> it1 = curr.predecessor.iterator();
            while(it1.hasNext()){
               curr.ReachingIN.addAll(it1.next().ReachingOUT); //Union of OUT sets of all predecessors
            }
          
             
            /* Implementing Eq 2 from notes and if boolean true is returned from either operation means IN changed so call updateWorkList
               Operations in order. Also. From Set Theory (Wikipedia): C U (B-A) = (B U C) - (A - C)
               GEN U (IN - KILL) = ( GEN U IN ) - (KILL - GEN) : Since In reaching def, GEN and KILL = Empty Set hence:
               GEN U (IN - KILL) = ( GEN U IN ) - KILL
               OUT(s) = GEN(s) U IN(s) - KILL(s)
            */
            /*
            HashSet<ReachingDefAnalysisNode> temporary = new HashSet<ReachingDefAnalysisNode>();
            temporary.addAll(curr.ReachingIN);
            temporary.addAll(curr.ReachingGEN);
            temporary.removeAll(curr.ReachingKILL);
             
            // boolean changed = curr.IN.addAll(temporary);
            // If IN set changes then "true" is returned i.e. a new element is added.
            // System.out.println("Set before change:" + curr.ReachingOUT);
            
            if(curr.ReachingOUT.addAll(temporary)){
               //System.out.println("changed set:" + curr.ReachingOUT);
               updateWorkListReachingDef(curr, workList);
            }
            */
            if(curr.ReachingOUT.addAll(curr.ReachingIN)){
               change = true;
            }
            if(curr.ReachingOUT.addAll(curr.ReachingGEN)){
               change = true;
            }
            if(curr.ReachingOUT.removeAll(curr.ReachingKILL)){
               change = true;
            }
            if(change){
               updateWorkListReachingDef(curr, workList);
         
            }
            
                 
         }
          
      }
   
   }
    // Update WorkList Depending on Successors.
   public static void updateWorkListReachingDef(IRNode instruction, LinkedList<IRNode> workList){
      // Need to iterate whole set of IR Nodes and make changes.
      Iterator<IRNode> it = instruction.successor.iterator();
      while(it.hasNext()){
         IRNode current = it.next();
         if(!workList.contains(current)){
            workList.add(current);            
         }
         else {
         }
      }
   }
   
   public static void intializeGENandKILLforReachingDef(){
      FunctionSymbolTable temp;
      int i=0;
      for(; i<FunctionSemanticHandler.currentActiveFunctions.size() ; i++){
         temp = FunctionSemanticHandler.currentActiveFunctions.get(i);
         for(int j=0; j< temp.functionIRList.size();j++){
             IRNode node = temp.functionIRList.get(j); 
             node.ReachingGEN = setReachingDefGEN(node,j);
             node.ReachingKILL = setReachingDefKILL(node,j);
         }
  
      }   

   }

/*-------------------------------------------------------------------------------------------*/
   public static void dataFlowAnalysis() {
      buildControlFlowGraph();
      livenessAnalysis();
      initializeDefSet();
      reachingDefAnalysis();
      
   }
   
    // Print Control Flow Graph Utility
   public static void processIRList(){ 
      //instructionsThatDefineResult.removeAll(instructionsThatUseOperand) ;
      //System.out.println(";SetDifference:"+ instructionsThatDefineResult);
      // Will contain list of vars a local var is defined in terms of
      HashSet<String> listVar = new HashSet<String>();
      CodeStatementInfo varInfo = null;
      StartEndLines tracking = null;
      boolean defineNewCodeInfo = true;
      IRNode loopEnd = null;
      //Assuming only one loop construct will be there:
      boolean loopEnter = false;
      if(FunctionSemanticHandler.currentActiveFunctions.size()>2) return;
      
      //printDefSet(allDefs);
      FunctionSymbolTable temp;
      if(DEBUG)System.out.println(";Processing IR List");
      int i=0;
      for(; i<FunctionSemanticHandler.currentActiveFunctions.size() ; i++){
         temp = FunctionSemanticHandler.currentActiveFunctions.get(i);
         for(int j=0; j< temp.functionIRList.size();j++){
            IRNode node = temp.functionIRList.get(j);
           
            if(node.isLoopNode() && node.predecessor.size()==2){
               // Only detect the back edge for the loop.
               loopEnter = true;
               //System.out.println("Crash");
               loopInformation = new LoopInfo(node,j+1);
               Iterator<IRNode> iterate = node.predecessor.iterator();
               while(iterate.hasNext()){
                  IRNode temporary = iterate.next();
                  if(temporary.getOpcode().equals("JUMP")){ 
                     loopEnd = temporary;                  
                  } 
                  if(temporary.getOpcode().contains("STORE")){
                     loopInformation.setLoopCounterVariable(temporary.getResult());
                  } 
               }
               //node.printIRNode();
            }
            // Set loop end info
            
            if(loopEnd !=null && node.equals(loopEnd)){
               loopInformation.setEndLineNumber(j+1);
               loopInformation.setEndNode(loopEnd);
            }
             // Gathering local variables info:
            if( checkNotIfStatmentNode(node) || skipForSingleStatement.contains(node.getOpcode())){
               continue;
            }
            else {
               // Process Nodes
               
               if(defineNewCodeInfo) {
                  //Sets start line of local var IR statement
                  //varInfo = new CodeStatementInfo(j+1);
                  tracking = new StartEndLines(j+1);
                  varInfo = new CodeStatementInfo();
                  defineNewCodeInfo = false;
               }
               // Handle STORE COnstant Temporary
               if(node.getOpcode().contains("STORE") && isConstant(node.getOperand1()) ){
                  temporaryToConstantMap.put(node.getResult(), node.getOperand1());
                  listVar.add(node.getResult());
                  //System.out.println(listVar);
               }
               
               //If its ADD MULT SUB DIV
               if(arithmeticOperations.contains(node.getOpcode())){
                  listVar.add(node.getOperand1());
                  listVar.add(node.getOperand2());
                  //listVar.add(node.getResult());
                    
               }
               if(DEBUG)node.printIRNode();               
               // Handle STORE Temporary LocalVariable
              
               
               if(node.getOpcode().contains("STORE") && node.getResult().contains("$L")) {
                  // Clear the set every time we store the list of a local variable
                  tracking.set = new HashSet<String>(listVar);
                  tracking.setEndLine(j+1);
                  varInfo.definedInTermsOf.add(tracking);
                
                  //To check for two or definitions of variable.
                  if(!localVar.containsKey(node.getResult())){
                     localVar.put(node.getResult(),varInfo);
                    
                  }  
                  else{
                     //add second or 3rd or so on redefinitions
                     localVar.get(node.getResult()).definedInTermsOf.add(tracking);
                  }
                 
                  defineNewCodeInfo = true;
                  listVar.clear();
               }
            }
                          
            
         }
      }
      
      if(loopEnter && DEBUG){
         System.out.println("Is loop start: "+ loopInformation.getStartLine());
         loopInformation.getStartNode().printIRNode();
         System.out.println("Is loop end: " + loopInformation.getEndLine());
         loopInformation.getEndNode().printIRNode();  
         System.out.println("Is loop counter variable: " + loopInformation.getLoopCounterVariable());      
         //loopEnd.printIRNode();
      }
      
      // TODO! NOTE: ONLY CARE ABOUT LOCAL VARS AND TEMPORARIES WHICH ARE CONSTANT IF SOME VAR IS DEFINED IN TERMS OF SOME TEMPORARY
      if(DO_CODE_MOTION) {  
         if(localVar.keySet().size()==0) return; 
         processLocalVariables(); 
         detectLoopInvariant();
         processingForLeftOvers();
         //TODO! DO it all if size is 1
         offset = loopInformation.getStartLine()-1; 
         ending = loopInformation.getEndLine()-1;
         moveIR(offset,ending);
      }
   }  
   
   // This function processes again and again for finding loop invariants that might have missed previous passes.
   public static void processingForLeftOvers(){
      int countOfFalseInv = 0;
      Iterator<String> it = localVar.keySet().iterator();
      while(it.hasNext()){ 
         String tempprary = it.next();
         if(!localVar.get(tempprary).isLoopInvariant) countOfFalseInv++;
      }    
      
      for(int y=0; y< countOfFalseInv;y++){
         doAdditionalPasses();
      }
      Iterator<String> iterate = localVar.keySet().iterator();
      while(iterate.hasNext()){ 
         String temporary = iterate.next();
         if(DEBUG)localVar.get(temporary).printStatementInfo();
      }
   } 

   public static boolean isConstant(String operand){
       if(operand.matches("[0-9]+") || operand.matches("[0-9]*\\.[0-9]+")){
         return true;
       } 
       else {
         return false;
       }
   
   }
 
    // Print Control Flow Graph Utility
   public static void printCFG(){ 
      if(!DEBUG) return;
      IRNode loopEnd = null;
      //Assuming only one loop construct will be there:
      boolean loopEnter = false;
      
      //printDefSet(allDefs);
      FunctionSymbolTable temp;
      System.out.println(";Control Flow Graph");
      int i=0;
      for(; i<FunctionSemanticHandler.currentActiveFunctions.size() ; i++){
         temp = FunctionSemanticHandler.currentActiveFunctions.get(i);
         for(int j=0; j< temp.functionIRList.size();j++){
            IRNode node = temp.functionIRList.get(j); 
            System.out.print("Predecessor Set: ");
            printSet(node.predecessor);
            System.out.print("Successor Set: ");
            printSet(node.successor);
            System.out.println(" KILL Set is "+ KILL(node));
            System.out.println(" GEN Set is "+ GEN(node));
            //TODO! I have removed temporaries from the step for now.
            //System.out.println("Liveness IN Set is "+ node.IN);
            //System.out.println("Liveness OUT Set is "+ node.OUT);
            System.out.println("Liveness IN Set is ");
            printLocalVarSet(node.IN);
            System.out.println("Liveness OUT Set is ");
            printLocalVarSet(node.OUT);
            
            System.out.println(" GEN Set for Reaching Def is ");
            printDefSet(setReachingDefGEN(node,j));
            
            System.out.println(" KILL Set for Reaching Def is ");
            printDefSet(setReachingDefKILL(node,j));
            
            System.out.println(" IN Set for Reaching Def is ");
            printDefSet(node.ReachingIN);
            
            System.out.println(" OUT Set for Reaching Def is ");
            printDefSet(node.ReachingOUT);
            
            System.out.println();
            System.out.println();
        }
      }      
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
    // Only checks simple loop condition
    public static boolean checkNotIfStatmentNode(IRNode nd){
      
      Iterator<IRNode> it = nd.successor.iterator();
     
      while(it.hasNext()){
         IRNode temp = it.next();
         if(!temp.getOpcode().equals("JUMP") && condUncondJumps.contains(temp.getOpcode()) && (temp.getOperand2().equals(nd.getResult()))){
             return true;
         }         
         
      }
      return false;
    
   }

   public static void printLocalVarSet(Set<String> set){
      Iterator<String> it = set.iterator();
      System.out.print("{");
      while(it.hasNext()){
         String temp = it.next();
         if(temp.contains("$T")) continue;
         System.out.print(temp+",");
      }
      System.out.print("}");
   
   }
    public static void printDefSet(HashSet<ReachingDefAnalysisNode> set){
      Iterator<ReachingDefAnalysisNode> it = set.iterator();
      
      while(it.hasNext()){
         ReachingDefAnalysisNode temp = it.next();
         if(temp.variable.contains("$T")) continue;
         temp.printReachingDefNode();
             
      }
      
   }
   /* Steps for making passes easier.
            1) In first pass check, if any constant variables are there who have only one definition.
            2) Checking which variables are in the loop. 
            3) Check if any other definition inside loop.

   */
   public static void processLocalVariables(){ 
      //helper variable to check constant
      boolean constantChecker = false;
      StartEndLines helper = null;
      Iterator<String> it = localVar.keySet().iterator();
      while(it.hasNext()){ 
         String temp = it.next();
         for(int i=0; i<localVar.get(temp).definedInTermsOf.size();i++){
            helper =  localVar.get(temp).definedInTermsOf.get(i);        
            if(DEBUG)helper.printStartEndLines();
            
            if(helper.startLine > loopInformation.getStartLine() && helper.endLine < loopInformation.getEndLine()){
               if(DEBUG)System.out.println("Variabe is within loop"+ temp);
               localVar.get(temp).isWithInLoop = true;
               
            }
               
            //Check if any other definition inside loop. If yes, then changes the variable to false;
            if(localVar.get(temp).definedInTermsOf.size()>=1 && helper.startLine > loopInformation.getStartLine() && helper.endLine < loopInformation.getEndLine()){                                
                  if(localVar.get(temp).hasNoOtherDefInsideLoop){
                     localVar.get(temp).hasNoOtherDefInsideLoop = false;
                     if(DEBUG)System.out.println("Has More than one def "+ temp);               
                     break;
                  }
                  localVar.get(temp).hasNoOtherDefInsideLoop = true;                           
            }
            // Before Loop Constants 
            if(localVar.get(temp).definedInTermsOf.size()==1 && helper.getSet().size()==1){
                localVar.get(temp).isConstant = true;
             
            }
         
           // Have only one definition AND Defined in terms of one other variable.
            if(localVar.get(temp).hasNoOtherDefInsideLoop && helper.getSet().size()==1){
               Iterator<String> iter = helper.getSet().iterator();
               if(temporaryToConstantMap.containsKey(iter.next())){
                  //System.out.println("This is constant "+ temp);
                  localVar.get(temp).isConstant = true;
              
               }
            }
         
         }   
    
      }
      
   }
   // CODE MOTION:
   public static void codeMotion(String live){
       //helper variable to check constant
      boolean constantChecker = false;
      StartEndLines helper = null;
      Iterator<String> it = localVar.keySet().iterator();
      while(it.hasNext()){ 
         
         String temp = it.next();
         if(!temp.equals(live)) continue;
         //System.out.println("local var name: "+temp);   
          for(int i=0; i<localVar.get(temp).definedInTermsOf.size();i++){
            helper =  localVar.get(temp).definedInTermsOf.get(i);
            //        
            if(localVar.get(temp).isLoopInvariant && helper.startLine > loopInformation.getStartLine() && helper.endLine < loopInformation.getEndLine() ){
               if(DEBUG)System.out.println("local var name: "+temp);   
               if(DEBUG)System.out.println("Starting line: "+ helper.startLine + " Ending line "+helper.endLine);   
              
               readjustIR(helper.startLine, helper.endLine);
            }
          }
      }
   
   }
   
   // Move IRLIST:
   public static void moveIR(int start, int end){
      FunctionSymbolTable temp;
      int i,j;
      for(i = 0; i<FunctionSemanticHandler.currentActiveFunctions.size(); i++){
         temp = FunctionSemanticHandler.currentActiveFunctions.get(i);
         for(j = start-1; j<temp.functionIRList.size(); j++){
            if(j>=loopInformation.getEndLine()) break;
            IRNode currentt = temp.functionIRList.get(j); 
            if(currentt.getOpcode().contains("STORE") && currentt.getResult().contains("$L")){
               String live = currentt.getResult();
               // Checking moving condition
               if(localVar.get(live).isLoopInvariant && !currentt.IN.contains(live)){ 
                  if(DEBUG)System.out.println("Movable Invariant " + live);
                  codeMotion(live);
               } 
            }
         }
      }
   }
   // Move IRLIST: TODO! ONLY CHECKED removing 2 nodes. Record of Offset has to be kept.
   public static void readjustIR(int start, int end){
      FunctionSymbolTable temp;
      ArrayList<IRNode> removed = new ArrayList<IRNode>();
     
      int i,j;
      for(i = 0; i<FunctionSemanticHandler.currentActiveFunctions.size(); i++){
         temp = FunctionSemanticHandler.currentActiveFunctions.get(i);
        
         for(j = start-1; j<temp.functionIRList.size(); j++){
           
            if(j>= end ) break;
            IRNode current = temp.functionIRList.get(j); 
            removed.add(current);
            //temp.functionIRList.remove(j);
            //starting = starting +1;
            //temp.functionIRList.add(starting,current);
            //System.out.println("Removed size: "+ temp.functionIRList.size());   
        
         }
         //System.out.println("Removed size: "+ removed.size());   
         if(removed.size()!=0){
            temp.functionIRList.subList(start-1,end).clear();
            temp.functionIRList.addAll(offset, removed);
            offset = offset + removed.size();
            ending = ending + removed.size();
           
         }
      }
     
   }
   
   // Detect Loop Invariants:
   
   public static void detectLoopInvariant(){ 
      System.out.println();
      if(DEBUG)System.out.println("---:Detecting Loop Invariants: Handle for Empty Lists Too---");
      boolean constantChecker = false;
      StartEndLines helper = null;
      Iterator<String> it = localVar.keySet().iterator();
      while(it.hasNext()){ 
         String temp = it.next();
         //System.out.println("local var name: "+temp);   
         for(int i=0; i<localVar.get(temp).definedInTermsOf.size();i++){
            helper =  localVar.get(temp).definedInTermsOf.get(i);        
            
            // If its within loop and has no other definition inside loop. And currently has not been classified as constant
            if(localVar.get(temp).isWithInLoop && localVar.get(temp).hasNoOtherDefInsideLoop){
               Iterator<String> iter = helper.getSet().iterator();
               //System.out.println("crash check" +temp);
               while(iter.hasNext()) { 
                  // Traversing all other vars in which terms teh current local variable is defined.
                  String candidate = iter.next(); 
                  
                
                  // TODO! If variable is defined in terms of itself then cant be loop invariant;
                  if(candidate.equals(temp)){
                    localVar.get(temp).isLoopInvariant = false;
                    //System.out.println("Temp equals cand "+ temp);
                  }
                  if(candidate.contains("$T") && temporaryToConstantMap.get(candidate)==null) continue;
                  if((candidate.contains("$T") && temporaryToConstantMap.containsKey(candidate)) || localVar.get(candidate).getIsConstant()){
                      constantChecker = true;
                      //System.out.println("urrent cand is"+ candidate);
                  }
                  else {
                      constantChecker = false;
                      //System.out.println("Not a loop"+candidate);
              
                      localVar.get(temp).isConstant = false;
                      break;
                  }
                  
                  
               }
               if(constantChecker && !temp.equals(loopInformation.loopCounterVariable)){
                     localVar.get(temp).isLoopInvariant = true;
                     //System.out.println("Loop invariant: "+ temp);
               }
                
               
            }
            
         }
      }
   }
  
   // Checking any loop invariants that might have missed previous passes.
   public static void doAdditionalPasses(){ 
      System.out.println();
      if(DEBUG)System.out.println("---:Additional Passes---");
      boolean constantChecker = false;
      StartEndLines helper = null;
      Iterator<String> it = localVar.keySet().iterator();
      while(it.hasNext()){ 
         String temp = it.next();
         //Some Unprocessed Invariant
         if(!localVar.get(temp).isLoopInvariant){  
            for(int i=0; i<localVar.get(temp).definedInTermsOf.size();i++){
               helper =  localVar.get(temp).definedInTermsOf.get(i);        
            
               // If its within loop and has no other definition inside loop. And currently has not been classified as constant
               if(localVar.get(temp).isWithInLoop && localVar.get(temp).hasNoOtherDefInsideLoop){
                  Iterator<String> iter = helper.getSet().iterator();
                  //System.out.println("crash check" +temp);
                  while(iter.hasNext()) { 
                     // Traversing all other vars in which terms teh current local variable is defined.
                     String candidate = iter.next(); 
                     if(candidate.equals(temp)){
                       localVar.get(temp).isLoopInvariant = false;
                       if(DEBUG)System.out.println("Temp equals cand "+ temp);
                     }
                     if(candidate.contains("$T") && temporaryToConstantMap.get(candidate)==null) continue;
                     if((candidate.contains("$T") && temporaryToConstantMap.containsKey(candidate)) || localVar.get(candidate).getIsConstant() || localVar.get(candidate).isLoopInvariant){
                         constantChecker = true;
                        //System.out.println("urrent cand is"+ candidate);
                     }
                     else {
                        constantChecker = false;
                        if(DEBUG)System.out.println("Not a loop"+candidate);
              
                        localVar.get(temp).isConstant = false;
                        break;
                     }
                  
                  
                  }
                  if(constantChecker && !temp.equals(loopInformation.loopCounterVariable)){
                      localVar.get(temp).isLoopInvariant = true;
                     //System.out.println("Loop invariant: "+ temp);
                  }                 
                 
       
               }
            }
         }
      }
   }
}
