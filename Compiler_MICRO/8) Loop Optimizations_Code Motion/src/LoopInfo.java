public class LoopInfo{
   IRNode startNode;
   IRNode endNode;
   int startlineNum;
   int endLineNum;
   String loopCounterVariable;
   public LoopInfo(IRNode node, int startlineNum) {
      this.startNode= node;  
      this.startlineNum = startlineNum;
   }  
   public LoopInfo(){
   
   }
  
   public int getStartLine(){
      return this.startlineNum;
   }
   public int getEndLine(){
      return this.endLineNum;
   }
 
   
   public IRNode getStartNode(){
      return this.startNode;
   }
 
   public IRNode getEndNode(){
      return this.endNode;
   }

   public void setLoopCounterVariable(String var){  
      this.loopCounterVariable = var;
   }   
   
   public String getLoopCounterVariable(){  
      return this.loopCounterVariable;
   }   
   
   public void setEndLineNumber(int line){
      this.endLineNum = line;
   }
   public void setEndNode(IRNode temp){
      this.endNode = temp;
   }
   
}
