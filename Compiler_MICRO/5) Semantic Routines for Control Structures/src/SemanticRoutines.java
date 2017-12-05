import java.util.*;

public class SemanticRoutines{

   public static ArrayList<IRNode> IR_LIST = new ArrayList<IRNode>();
   // To store the variables defined in the program in the right order 
   public static ArrayList<String> VAR_LIST = new ArrayList<String>();
   // To store TinyInstructionNodes 
   public static ArrayList<TinyInstructionNode> TINY_LIST = new ArrayList<TinyInstructionNode>();
   /* Building Temporay Registers for use in IR Code Generation. Will increment count on every time*/
   private static int current_temporay_register = 1;
   private static int current_register = 0;
   private static int label_counter = 1;
   public SymbolMap current_symbol_map = new SymbolMap();
   
   public static Stack<String> conditional_statement_operator = new Stack<String>();
   public static Stack<String> labels = new Stack<String>();

   static IRNode for_init;
   static IRNode for_incr;   
   // To pop only once
   private static int symbol_map_pop = 1;

   /** From assignment statement rule. Converting infix to postfix format for expression evaluation
       Parser Rule --> assign_expr : id ':=' expr
       This method assumes valid input.
       @param infix : $expr.text
   */
   public static String infixToPostfixExpression(String infix){
       /* Appending with StringBuilder saves alot of time. String makes a new copy each time '+' is called.*/
      StringBuilder sb = new StringBuilder();
      Stack<Character> stack = new Stack<Character>();
      boolean offset = false;
      int i=0;
      char c = ' ';
      for(; i<infix.length(); i++){
         
         c = infix.charAt(i);
         
         // ignore whitespaces
         if (c==' ') continue;
         
         // Handle digits with one or more characters. And Also Float characters now.
         if(Character.isDigit(c) || c =='.'){
             while(i<infix.length() && (Character.isDigit(infix.charAt(i)) || infix.charAt(i)=='.')){
                sb.append(String.valueOf(infix.charAt(i)));
                i++;
                offset = true;
                //System.out.println("Number "+sb.toString()+ "and i "+ i);
             }
             sb.append(String.valueOf(" "));
         }
         else if(Character.isLetter(c)){

               
            while(i<infix.length() && ( Character.isLetter(infix.charAt(i)) || Character.isDigit(infix.charAt(i)) ) ) {
                sb.append(String.valueOf(infix.charAt(i)));
                i++;
                offset = true;
            }
         


            //sb.append(String.valueOf(c)); 
            sb.append(String.valueOf(" "));
            
         }
         
         else if(c == '(')
            stack.push(c);
            
         else if(c == ')'){
 
             while (!stack.isEmpty() && stack.peek() != '('){
                    sb.append(String.valueOf(stack.pop()));
                    sb.append(String.valueOf(" ")); 
                   
             }
             // Popping "(": TODO! Invalid Input:Can Check for error here that while stack not empty and peek()!= '(': ERROR.
             stack.pop();
         }
         // Should be an addop or mulop. 
         else{
             while (!stack.isEmpty() && precedenceOperator(c) <= precedenceOperator(stack.peek())){
                    sb.append(String.valueOf(stack.pop()));
                    sb.append(String.valueOf(" ")); 
             }
             //System.out.println(c);
             stack.push(c);
         }
    
         // Number with more than one digit will offset i by 1. So adjusting that.
         if(offset){
            i = i-1;
            offset = false;
         }
            
      }
     
      // Remaining Operators
      while (!stack.isEmpty()){
         sb.append(String.valueOf(stack.pop()));
         sb.append(String.valueOf(" ")); 
      }

      // trim() because dont want any trailing spaces
      return sb.toString().trim();

   }
   
   /* Helper function for infixToPostfix function. Mul/Div have higher precedence. */
   public static int precedenceOperator(char op){
      if(op=='+' || op=='-')
         return 1;
      else if(op=='*' || op=='/')
         return 2;
      else return 0;
   }

   /** Generate IR List from postfix expression. Building an AST serves the same purpose. 
       I only need to do post-order traversal of AST which can be attained through postfix
       expression evaluation. Only care about global scope here so popping in start and saving
       in global variable satisfies the purpose.
       @param postfix: postfix expression returned from infix expression
   */ 
   public static void postfixToIRList(String lvalue, String postfix, SymbolMap current_map){
      
      Stack<String> expression = new Stack<String>();
      String result = postfix;
      int i = 0; 
      char c = ' ';
			String temp;

		//YJ
			//Check the type of lvalue
			String exprType = current_map.map.get(lvalue).getType();
     
      if(postfix.matches("[0-9]+")){ 
         // Its a STOREI Instruction. Just add to IR node and RETURN From method.
         //System.out.println("Holla Its an integer");

			//YJ
				 temp = makeTemporary();
				 IR_LIST.add(new IRNode("STOREI", postfix, "",temp));
				 IR_LIST.add(new IRNode("STOREI", temp, "",lvalue));
      }
      else if(postfix.matches("[0-9]*\\.[0-9]+")){
         //Its a STOREF Instruction. Just add to IR node and RETURN From method.
         //System.out.println("Holla Its a Float");

			//YJ
				 temp = makeTemporary();
				 IR_LIST.add(new IRNode("STOREF", postfix, "",temp));
				 IR_LIST.add(new IRNode("STOREF", temp, "",lvalue));
       }

      /** CAN USE THE FORMULATION BELOW TO ACCESS MAP.
      System.out.println("Type of the current literal is "+ current_map.map.get(lvalue).getType());
      if(symbol_map_pop==1) {
         printCurrentSymbolMap(current_map);
         symbol_map_pop++;
      }
      */

			//YJ -- To separate assignment from expresssions
		else 
		{

      for(; i<postfix.length(); i++){
         
         
         c = postfix.charAt(i);
         //ignore whitespace
         if(c==' ') continue;


         else if(Character.isDigit(c) || c =='.'){
              StringBuilder sb = new StringBuilder(); // Every time make a new string builder.
              while(i<postfix.length() && (Character.isDigit(postfix.charAt(i)) || postfix.charAt(i)=='.')){
             
                sb.append(String.valueOf(postfix.charAt(i)));
                i++;
                //No need to take care of offset here because there will be a whitespace as opposed to infix function
                //System.out.println("Number "+sb.toString()+ "and i "+ i);
             }
             // Push the operand on stack
             //expression.push(sb.toString());

					//YJ -- The above statement is not correct
						 temp = makeTemporary();
				 		 if (exprType.equals("INT")) 
							 IR_LIST.add(new IRNode("STOREI", sb.toString(), "",temp));
						 else if (exprType.equals("FLOAT")) 
							 IR_LIST.add(new IRNode("STOREF", sb.toString(), "",temp));
						 expression.push(temp);
                    
         }
         else if(Character.isLetter(c)){
             // Handling multicharacter variables
             StringBuilder var = new StringBuilder(); // Every time make a new string builder.
            while(i<postfix.length() && ( Character.isLetter(postfix.charAt(i)) || Character.isDigit(postfix.charAt(i)) ) ) {
                var.append(String.valueOf(postfix.charAt(i)));
                i++;
            }
            expression.push(var.toString());
            
         }
         /* An operator encountered. Here We need to generate code and put things in IR List. */
         else{
           
              String op2 = expression.pop();
              //System.out.println("Operand 1 "+op2);
              String op1 = expression.pop();
              //System.out.println("Operand 2 "+op1);
              switch(c)
                {
                    case '+':
                    /* Generate IR Code and push back result as a temporary or whatever is required back on stack. e.g: "a+b"*c: ADDI a b $T4
                       ;MULTI c $T4 $T5. Use makeTemporary() to get a new temporary every time. ALSO ADD TO IR_LIST as.
                       IR_LIST.add(new IRNode(opcode, operand1, operand2, result))  IMP: Use null for instruction that dnt require it.*/
		       temp = makeTemporary();
		       if (exprType.equals("INT")) 
		           IR_LIST.add(new IRNode("ADDI", op1, op2,temp));
		       else if (exprType.equals("FLOAT")) 
		           IR_LIST.add(new IRNode("ADDF", op1, op2,temp));
		       expression.push(temp);
                    
                    break;
                     
                    case '-':
                    /* TODO! Generate IR Code and push back result as a temporary or whatever is required back on stack. ALSO ADD TO IR_LIST 
                             
                    */

									//YJ
										temp = makeTemporary();
										if (exprType.equals("INT")) 
											IR_LIST.add(new IRNode("SUBI", op1, op2,temp));
										else if (exprType.equals("FLOAT"))
											IR_LIST.add(new IRNode("SUBF", op1, op2,temp));
										expression.push(temp);
                    break;
                     
                    case '/':
                    /* TODO! Generate IR Code and push back result as a temporary or whatever is required back on stack. 
                             ALSO ADD TO IR_LIST 
                    */

									//YJ
			temp = makeTemporary();
										if (exprType.equals("INT")) 
											IR_LIST.add(new IRNode("DIVI", op1, op2,temp));
										else if (exprType.equals("FLOAT"))
											IR_LIST.add(new IRNode("DIVF", op1, op2,temp));
										expression.push(temp);
                 
                    break;
                     
                    case '*':
                    /* TODO! Generate IR Code and push back result as a temporary or whatever is required back on stack. ALSO ADD TO IR_LIST 
                             
                    */

									//YJ
										temp = makeTemporary();
										if (exprType.equals("INT")) 
											IR_LIST.add(new IRNode("MULTI", op1, op2,temp));
										else if (exprType.equals("FLOAT"))
											IR_LIST.add(new IRNode("MULTF", op1, op2,temp));
										expression.push(temp);                 
                    break;
              }

         }


      }

		//YJ -- The loop has ended so assign the last temp in stack to the lvalue
		if (exprType.equals("INT")) 
		IR_LIST.add(new IRNode("STOREI", expression.pop(), "",lvalue));
		else if (exprType.equals("FLOAT")) 
		IR_LIST.add(new IRNode("STOREF", expression.pop(), "",lvalue));
		
		
		}
      //printIRCode(IR_LIST);      
   }

	 public static void readIRNodes(String memVariable, SymbolMap current_map)
	 {

			String readType = current_map.map.get(memVariable).getType();
			
			if (readType.equals("INT")) 
				IR_LIST.add(new IRNode("READI", "", "",memVariable));
			else if (readType.equals("FLOAT")) 
				IR_LIST.add(new IRNode("READF", "", "",memVariable));

	 }
   
	 public static void writeIRNodes(String memVariable, SymbolMap current_map)
	 {

			String writeType = current_map.map.get(memVariable).getType();
			
			if (writeType.equals("INT")) 
				IR_LIST.add(new IRNode("WRITEI", "", "",memVariable));
			else if (writeType.equals("FLOAT")) 
				IR_LIST.add(new IRNode("WRITEF", "", "",memVariable));

	 }

  /* A for loop has an initialization of the iterator, the condition and the increment expression 
  of the iterator. Both init and incr are assign expressions and are handled by that specific semantic 
  routine. Challenge is to add incr related IR nodes after the for loop stmt list, for that we need to
	manipulate the IR array list at the end of the loop. Condition is handled here, logic is first identify
  the operator and then the operands and based on the operater generate the specific IR node */

  //NOTE1: the statement list of for loop is not working correctly, probably there is some issue with step4 code
	// or infix to postfix

  //NOTE2: this code was designed for a single for loop, it might work for nested fors as well but didn't test that
	//as there is no test case given for that and probably we won't see that as well for future steps
 
   public static String labelGenerator(){
       
      String temporary = "label" + Integer.toString(label_counter);
      label_counter++;
      return temporary;
   }


   public static String pushOnLabelStack(){
      // We can keep track of depth of nesting using stack of labels. Parser rules will  make sure that correct label was popped or peek at(in case of jump don't want to pop).
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
   

   public static void makeJumpIRCode(String label){
   
       IR_LIST.add(new IRNode("JUMP", "", "",label));
	
   }

   public static void makeLabelIRCode(String label){
   
       IR_LIST.add(new IRNode("LABEL", "", "",label));
	
   }
   
   public static void conditionalOperatorStack(String expr){
      conditional_statement_operator.push(expr.trim());
   }

   public static void addConditionalStatementIRCode(String compop, SymbolMap map){
      // expr compop expr : op1 is expr on right and op2 is expr on left
      String op1 = conditional_statement_operator.pop();
      String op2 = conditional_statement_operator.pop();
      //System.out.println(op1);
      //System.out.println(op2);

      /* TODO! handle Expressions
      System.out.println(infixToPostfixExpression(op1));
      System.out.println(infixToPostfixExpression(op2));
      String t1 = makeTemporary();
      postfixToIRList(t1, infixToPostfixExpression(op1), map);*/
      
      // TODO!BAD WAY Currently Assuming right hand side is an INT or FLOAT
      String temp = makeTemporary();
      String current_label = peekOnLabelStack();
      System.out.println("Current Label:" + current_label);
      
      if(op1.contains(".")){
         IR_LIST.add(new IRNode("STOREF", op1, "",temp));  
      }
      else{
         IR_LIST.add(new IRNode("STOREI", op1, "",temp));  
      }
      
      
      if (compop.equals("<")) {
	  IR_LIST.add(new IRNode("GE", op2 , temp, current_label));
      }
      else if (compop.equals(">")) {
	  IR_LIST.add(new IRNode("LE", op2 , temp, current_label));
      }
      else if (compop.equals("=")) {
	  IR_LIST.add(new IRNode("NE", op2 , temp, current_label));
      }
      else if (compop.equals("!=")) {
	  IR_LIST.add(new IRNode("EQ", op2 , temp, current_label));
      }
      else if (compop.equals("<=")) {
	  IR_LIST.add(new IRNode("GT", op2 , temp, current_label));
      }
      else if (compop.equals(">=")) {
	  IR_LIST.add(new IRNode("LT", op2 , temp, current_label));
      } 
      
      
   }

   public static void forLoopStart() 
   {
      for_incr = IR_LIST.remove(IR_LIST.size()-1);
      for_init = IR_LIST.remove(IR_LIST.size()-1);
   }

   
   public static void endForLoop(String incr_stmt, SymbolMap map)
   {
      // Adding the init and incr statement IR nodes
      IR_LIST.add(for_init);
      /* Handling two generated labels in FOR loop rule start. 
         Here label 1: LOOP start and label2: OUT and currently on top of stack is label2
         No label for INCR statement */
      IR_LIST.add(for_incr);
   
      makeJumpIRCode(peekSecondLabel());
      makeLabelIRCode(popLabelStack());
      // poplabelStack() called two times will empty for loop labels.
      popLabelStack();
   }  

	
/* If statements can be handled by similarly, just need to identify where to add jumps
and labels. Below are some test functions use to identify specific areas where the labels, jumps
could be added */

/*
	 public static void ifStmnt(String cond)
	 {
			System.out.println(cond);
	 }

	public static void ifStmntList(String cond)
	 {
			System.out.println("Statement list of: "+cond);
	 }   

	public static void endifStmnt(String stmnt)
	 {
			System.out.println("im in endif of: "+stmnt);
	 }

	public static void elifStmnt(String stmnt)
	 {
			System.out.println("im in elif of: "+stmnt);
	 }

	public static void elseStmnt(String stmnt)
	 {
			System.out.println("im in else of: "+stmnt);
	 }
*/
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
   
 	 public static void printIRCode(){
      int size = IR_LIST.size();
      int i=0; 

      System.out.println(";IR code");
   
      for(; i<size ; i++){
        
         IRNode node = IR_LIST.get(i);
         node.printIRNode();
     
      }
		//Calling print TinyCode here for step4 output
		tinyInterpreter();
		printTinyCode();
      
   }

//YJ -- will convert temps to regs but will do nothing to vars
	public static String tempToRegister(String temp)
	{
		if (temp.indexOf("$") == -1)
			return temp;			

		String tempNumber = temp.substring(temp.indexOf("T")+1);
		int regNumber = Integer.parseInt(tempNumber) - 1;
		String register = "r" + Integer.toString(regNumber);
		return register; 
	}

	//YJ -- Convert the 3AC to Tiny Code and store in a separate data structure
	 public static void tinyInterpreter()
	 {
			int size = IR_LIST.size();
      int i=0; 

      for(; i<size ; i++){
        
         IRNode node = IR_LIST.get(i);
         char holding_float_or_int = 'i';

         if ( (node.getOpcode().equals("STOREI")) || (node.getOpcode().equals("STOREF")) )
				 {
				 	if ( (node.getResult().indexOf("$")) != -1)
							TINY_LIST.add(new TinyInstructionNode("move", node.getOperand1(), tempToRegister(node.getResult())));
				 	else if ( (node.getOperand1().indexOf("$")) != -1 )
							TINY_LIST.add(new TinyInstructionNode("move", tempToRegister(node.getOperand1()), node.getResult()));
				 }
		
				 else if ( (node.getOpcode().equals("MULTI")) || (node.getOpcode().equals("MULTF")) )
				 {
							
                                                        if((node.getOpcode().equals("MULTF"))){
                                                              holding_float_or_int = 'r';
                                                        }
                                                        else{
                                                              holding_float_or_int = 'i';
                                                        }
                                                        
                                                        TINY_LIST.add(new TinyInstructionNode("move", tempToRegister(node.getOperand1()), tempToRegister(node.getResult())));
							TINY_LIST.add(new TinyInstructionNode("mul"+String.valueOf(holding_float_or_int), tempToRegister(node.getOperand2()), tempToRegister(node.getResult())));
				 }		

				 else if ( (node.getOpcode().equals("ADDI")) || (node.getOpcode().equals("ADDF")) )
 				 {
							if((node.getOpcode().equals("ADDF"))){
                                                              holding_float_or_int = 'r';
                                                        }
                                                        else{
                                                              holding_float_or_int = 'i';
                                                        }
                                                        
                                                        TINY_LIST.add(new TinyInstructionNode("move", tempToRegister(node.getOperand1()), tempToRegister(node.getResult())));
							TINY_LIST.add(new TinyInstructionNode("add"+String.valueOf(holding_float_or_int), tempToRegister(node.getOperand2()), tempToRegister(node.getResult())));
				 }

				 else if ( (node.getOpcode().equals("DIVI")) || (node.getOpcode().equals("DIVF")) )
				 {
                                                        if((node.getOpcode().equals("DIVF"))){
                                                              holding_float_or_int = 'r';
                                                        }
                                                        else{
                                                              holding_float_or_int = 'i';
                                                        }
                                                        
							TINY_LIST.add(new TinyInstructionNode("move", tempToRegister(node.getOperand1()), tempToRegister(node.getResult())));
							TINY_LIST.add(new TinyInstructionNode("div"+String.valueOf(holding_float_or_int), tempToRegister(node.getOperand2()), tempToRegister(node.getResult())));
				 }

				else if ( (node.getOpcode().equals("SUBI")) || (node.getOpcode().equals("SUBF")) )
				 {
	                                                if((node.getOpcode().equals("SUBF"))){
                                                              holding_float_or_int = 'r';
                                                        }
                                                        else{
                                                              holding_float_or_int = 'i';
                                                        }
                                                        						
                                                        TINY_LIST.add(new TinyInstructionNode("move", tempToRegister(node.getOperand1()), tempToRegister(node.getResult())));
							TINY_LIST.add(new TinyInstructionNode("sub"+String.valueOf(holding_float_or_int), tempToRegister(node.getOperand2()), tempToRegister(node.getResult())));
				 }							
     		
				else if ( node.getOpcode().equals("WRITEI") )
				 {
							TINY_LIST.add(new TinyInstructionNode("sys", "writei", node.getResult()));
				 }

				else if ( node.getOpcode().equals("WRITEF") )
				 {
							TINY_LIST.add(new TinyInstructionNode("sys", "writer", node.getResult()));
				 }		

				else if ( node.getOpcode().equals("READI") )
				 {
							TINY_LIST.add(new TinyInstructionNode("sys", "readi", node.getResult()));
				 }

				else if ( node.getOpcode().equals("READF") )
				 {
							TINY_LIST.add(new TinyInstructionNode("sys", "readr", node.getResult()));
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
	

			for(; i<numVars; i++)
			{
				System.out.println("var "+VAR_LIST.get(i));
			}
			
			for (; j<tinySize; j++)
			{
				TINY_LIST.get(j).printTinyInstructionNode();
			}
   
			System.out.println("sys halt");

   }

   public static void main(String[] args){

      String exp = "a b *";
      SymbolMap map = new SymbolMap();
      System.out.println(exp);
      
      //System.out.println(infixToPostfixExpression(exp));
      postfixToIRList("x",exp,map);
           
   }

}

/* Class for IR node objects */
class IRNode{

   private String opcode, operand1, operand2, result;
   
   /* Constructor */
   public IRNode(String opcode, String operand1, String operand2, String result){
    
      this.opcode = opcode; 
      this.operand1 = operand1;
      this.operand2 = operand2;
      this.result = result;
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



/* Class for Tiny Code node Object */
class TinyInstructionNode{

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
    
      System.out.println(this.opcode +" "+ this.operand1 +" "+ this.operand2);
     


   }

}

