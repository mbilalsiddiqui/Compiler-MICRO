/* Class for Tiny Code node Object */
public class TinyInstructionNode{

   private String opcode, operand1, operand2;
   
   /* Constructor */
   public TinyInstructionNode(String opcode, String operand1, String operand2){
    
      this.opcode = opcode; 
      this.operand1 = operand1;
      this.operand2 = operand2;
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

 //YJ  
   /* Print Method for Tiny Instruction Node*/
   public void printTinyInstructionNode(){
    
      if (this.operand1.equals(""))
        System.out.println(this.opcode +" "+ this.operand2);
      else	
        System.out.println(this.opcode +" "+ this.operand1 +" "+ this.operand2);
   }

}
