import java.util.*;
public class StartEndLines{
   int startLine;
   int endLine;
   HashSet<String> set;
   public StartEndLines(int startLine){
      this.startLine = startLine;
      set = new HashSet<String>();
   }
   public void setEndLine(int line){
      this.endLine = line;
   }  
   public HashSet<String> getSet(){
      return this.set; 
   }
   public void printStartEndLines(){
         System.out.println("start line "+ this.startLine );   
         System.out.println("end line "+ this.endLine );   
         System.out.println("defined in terms of "+ this.set);
   }
}

