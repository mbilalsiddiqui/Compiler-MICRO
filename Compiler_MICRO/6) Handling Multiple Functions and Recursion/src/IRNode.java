import java.util.*;
/* Class for IR node objects */
public class IRNode{

   private String opcode, operand1, operand2, result;
   // Successor and Predecessor Sets Required for Control FLow Graph. Can define them in constructor also.
   // HashSet does unordered insertions. NOTE! If order of insertion required later the use LinkedHashSet.
   public Set<IRNode> successor = new HashSet<IRNode>();
   public Set<IRNode> predecessor = new HashSet<IRNode>();
   public Set<String> IN = new HashSet<String>();
   public Set<String> OUT = new HashSet<String>();
   
   
  
   // IMPORTANT NOTE! Overriding these two methods is important according to Java Objects Contract. Otherwise Set of IRNodes 
   // can behave wierdly. If the underlying object is nit "primitive", override these two methods.
   // Adding elements to "predecessor" and "successor" sets without overriding them creates sets with duplicate entries.
   @Override
   public boolean equals(Object o){
      IRNode temp = (IRNode) o;
      if(temp.opcode.equals(opcode) && temp.operand1.equals(operand1) && temp.operand2.equals(operand2) && temp.result.equals(result)){
         return true;
      }
      return false;   

   }
    
   @Override
   public int hashCode(){
      return opcode.hashCode();
   
   }
 
   /* Constructor */
   public IRNode(String opcode, String operand1, String operand2, String result){
    
      this.opcode = opcode; 
      this.operand1 = operand1;
      this.operand2 = operand2;
      this.result = result;
      IN.clear();
      OUT.clear();
   }
   
   /* Getter Methods for Private variables */
   public String getOpcode(){
      return this.opcode;
   }

   
   public String getOperand1(){
      return this.operand1;
   }


   public String getOperand2(){
      return this.operand2;
   }

   public String getResult(){
      return this.result;
   }

   /* Print Method for IR Node*/ 
   public void printIRNode(){
      String toPrint = "";
      if(!operand1.equals("")){
	toPrint = " " + operand1;
      }
      if (!operand2.equals("")){
	toPrint = " " + operand2;
      }
      if (!result.equals("")){
	toPrint = " " + result;
      }
//YJ -- the above print method was not working
      //System.out.println(";" + opcode  + toPrint);
			if (operand2.equals(""))
				System.out.println(";"+this.opcode+" "+this.operand1+" "+this.result);
			else if (operand1.equals("") && operand2.equals(""))
				System.out.println(";"+this.opcode+" "+this.result);			
			else
				System.out.println(";"+this.opcode+" "+this.operand1+" "+this.operand2+" "+this.result);
     
   }

}

