import java.util.*;

public class CodeStatementInfo{
   
   // Will hold all definitions of Variable. If defined more than one then its size will be greater than 1.
   ArrayList<StartEndLines> definedInTermsOf;
   // Can be more than one definitions.
   //StartEndLines track;
   boolean isLoopInvariant;
   boolean isWithInLoop;
   boolean isConstant;
   boolean hasNoOtherDefInsideLoop;    
   public CodeStatementInfo(){
      //track = new StartEndLines(startLine);
      definedInTermsOf  = new ArrayList<StartEndLines>();
      
   }

  
   
   public void setLoopInvariant(boolean set){
      this.isLoopInvariant = set;
   }
   
   public boolean getLoopInvariant(){
      return this.isLoopInvariant;
   }
   
   
   public boolean isInLoop(){
      return this.isWithInLoop;
   }
   
   public boolean getIsConstant(){
      return this.isConstant;
   }
   
   public void printStatementInfo(){
      System.out.println("Is loop Invariant "+ isLoopInvariant);
   }
}


