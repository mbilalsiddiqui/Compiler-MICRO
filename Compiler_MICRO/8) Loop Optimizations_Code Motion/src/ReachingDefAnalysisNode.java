import java.util.*;
/* Because IRNode and line number of code both define an entry in GEN and KILL Sets
*/
public class ReachingDefAnalysisNode{ 
   String variable;
   int lineNumber;
   
   public ReachingDefAnalysisNode(String variable, int line){
      this.variable = variable;
      this.lineNumber = line;
   
   }
  
   @Override
   public boolean equals(Object o){
      ReachingDefAnalysisNode temp = (ReachingDefAnalysisNode) o;
      if(temp.variable.equals(variable) && temp.lineNumber == lineNumber){
         return true;
      }
      return false;   

   }
    
   @Override
   public int hashCode(){
      
      return variable.hashCode()+ this.lineNumber;
   
   }
   public int getReachingDefLine(){
      return this.lineNumber;     
   }
   
   public String getReachingDefVar(){
      return this.variable;     
   }
   public void printReachingDefNode(){
      System.out.println("{ "+ this.variable + ","+ this.lineNumber + "}");
   } 
}
