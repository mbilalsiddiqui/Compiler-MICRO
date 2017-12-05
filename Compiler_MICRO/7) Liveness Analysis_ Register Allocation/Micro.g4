/*If only lexer is wanted then use 'lexer' word before 'grammar' */
grammar Micro;


/*---------------------------------------------------  PARSER RULES (following parser writing conventions "4 spaces")------------------------------------------------*/

/* Global Object Declaration. */
@parser::members {
  SymbolMap map;
}


/* Program */

program           
    :    'PROGRAM'  id 'BEGIN' pgm_body 'END' {FunctionSemanticHandler.printIRCode();} //ControlFlowGraph.printCFG(); } // YJ addition for step6
    ; 

id                
    :    IDENTIFIER 
    ;

pgm_body
          
    :    {SymbolTable.createGlobalScope();FunctionSymbolTable curr = new FunctionSymbolTable("GLOBAL"); FunctionSemanticHandler.currentActiveFunctions.add(curr);} decl {map = SymbolTable.peekScope();FunctionSemanticHandler.symbolTables.put(FunctionSymbolTable.functionName,FunctionSymbolTable.functionTable);}func_declarations //Need to hold current scope in a map. 
    ;

decl
    :    string_decl decl 
    |    var_decl decl 
    |                 // empty is represented as simple whitespaces in ANTLR grammars. #StackOverflow post 
    ; 



/* Global String Declaration */
string_decl
    :    'STRING' id ':=' str ';' {SymbolTable.insertString($id.text, "STRING", $str.text); FunctionSymbolTable current = FunctionSemanticHandler.checkLastFunction();current.localScope=true; FunctionSymbolTable.insertFunctionTableLocal($id.text, "STRING", $str.text);/*System.out.println("String is "+$id.text+current.functionName);*/current.localScope=true;}
    ;       

str
    :    STRINGLITERAL 
    ;

/* Variable Declaration */
var_decl          
    :    var_type id_list ';'    {SymbolTable.insertVariableType($id_list.text, $var_type.text);  FunctionSymbolTable current = FunctionSemanticHandler.checkLastFunction();current.localScope=true; FunctionSymbolTable.insertFunctionTableLocal($id_list.text, $var_type.text,""); current.localScope=false;}
    ;

var_type          
    :    'FLOAT' | 'INT' 
    ;

any_type          
    :    var_type | 'VOID' 
    ;

id_list           
    :    id id_tail 
    ;

id_tail 
    : ',' id id_tail |          // empty in parser rule of grammar again treated as whitespace 
    ;

/* Function Paramater List */
param_decl_list   
    :    param_decl param_decl_tail |
    ;

param_decl        
    :    var_type id  {SymbolTable.insertVariableType($id.text, $var_type.text);  FunctionSymbolTable current = FunctionSemanticHandler.checkLastFunction();current.localScope = false; FunctionSymbolTable.incParamCount(); FunctionSymbolTable.insertFunctionTableLocal($id.text, $var_type.text,"");}
    ;

param_decl_tail  
    :    ',' param_decl param_decl_tail |
    ;


/* Function Declarations */
func_declarations  
    :    func_decl func_declarations |   
    ;

func_decl         
    : 'FUNCTION' any_type id {SemanticRoutines.initiateFunction($id.text);}'('param_decl_list')' {SemanticRoutines.setParamCount();} 'BEGIN' func_body 'END'  { FunctionSemanticHandler.checkLastStatement();}
    ;

func_body 
    :    decl {FunctionSemanticHandler.symbolTables.put(FunctionSymbolTable.functionName,FunctionSymbolTable.functionTable);FunctionSemanticHandler.declareFunction();} stmt_list 
    ;

/* Statement List */
stmt_list         
    :    stmt stmt_list | 
    ;

stmt              
    :    base_stmt | if_stmt | for_stmt
    ;

base_stmt         
    :    assign_stmt | read_stmt | write_stmt | return_stmt
    ;

/* Basic Statements */
assign_stmt       
    :    assign_expr ';' 
    ;

assign_expr       
    :    id ':=' expr  {FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction());SemanticRoutines.leftRightValuestoIR($id.text,map);SemanticRoutines.simpleAssignmentToIR(map);}
    ;

read_stmt         
    :    'READ' '(' id_list ')' ';' {FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction());SemanticRoutines.readIRNodes($id_list.text,map);}
    ;

write_stmt        
    :    'WRITE' '(' id_list ')' ';' {FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction());SemanticRoutines.writeIRNodes($id_list.text, map);}
    ;

return_stmt       
    :    'RETURN' expr ';'  {FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction()); SemanticRoutines.returnToIR(map);}
    ;

/* Expressions: Had to rewrite first four of these rules to support Expression buildup in postorder format. */ 
expr              
    :  factor expr_prefix 
    ;

expr_prefix       
    :   addop factor {FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction()); SemanticRoutines.handleOperator($addop.text, map); }  expr_prefix  |/*{System.out.println("the current add is "+ $addop.text);}*/
    ;

factor            
    :  postfix_expr factor_prefix 
    ;

factor_prefix     
    :    mulop postfix_expr {FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction()); SemanticRoutines.handleOperator($mulop.text,map); } factor_prefix |
    ;

postfix_expr     
    :    primary | call_expr {FunctionSemanticHandler.currentFuncTable.pushCountTrack = 0;}
    ;

call_expr          
    :    id {FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction());FunctionSemanticHandler.currentFuncTable.pushCountTrack = 0;}'(' expr_list ')' {FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction()); SemanticRoutines.addJsrAndPoptoIR($id.text);}
    ;

expr_list         
    :    expr {FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction());SemanticRoutines.addPushToIR($expr.text);} expr_list_tail |
    ;

expr_list_tail    
    :   ',' expr {FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction());SemanticRoutines.addPushToIR($expr.text);} expr_list_tail |
    ;

primary           
    :    '('expr')' | id {FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction());SemanticRoutines.leftRightValuestoIR($id.text,map);}| INTLITERAL{FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction()); SemanticRoutines.constantToIR($INTLITERAL.text);} | FLOATLITERAL{FunctionSemanticHandler.setCurrent(FunctionSemanticHandler.checkLastFunction()); SemanticRoutines.constantToIR($FLOATLITERAL.text);}
    ;

addop            
    :    '+' | '-'
    ;

mulop            
    :    '*' | '/'
    ;

/* Complex Statements and Condition */ 
if_stmt           
    :    'IF' {SemanticRoutines.pushOnLabelStack(); SemanticRoutines.pushOnLabelStack(); } '(' cond ')' {SymbolTable.createBlockScope();}  decl stmt_list 
     {SemanticRoutines.makeJumpIRCode(SemanticRoutines.peekSecondLabel());SemanticRoutines.makeLabelIRCode(SemanticRoutines.popLabelStack());} elif_part 'ENDIF' {SemanticRoutines.makeLabelIRCode(SemanticRoutines.popLabelStack());} 
    ;

elif_part         
    :    'ELIF' {SemanticRoutines.pushOnLabelStack();} '(' cond ')' {SymbolTable.createBlockScope();} decl stmt_list  { SemanticRoutines.makeJumpIRCode(SemanticRoutines.peekSecondLabel()); SemanticRoutines.makeLabelIRCode(SemanticRoutines.popLabelStack());} elif_part | else_part
    ;

else_part       
    :    'ELSE' {SemanticRoutines.pushOnLabelStack();} {SymbolTable.createBlockScope();} decl stmt_list {SemanticRoutines.makeJumpIRCode(SemanticRoutines.peekSecondLabel()); SemanticRoutines.makeLabelIRCode(SemanticRoutines.popLabelStack());} | 
    ;

cond              
    :    lit cond_suffix 
    ;

cond_suffix       
    :    'OR' lit cond_suffix | 'AND' lit cond_suffix | 
    ;

lit               
    :    'NOT' basic_cond | basic_cond
    ;

basic_cond        
    :    expr compop expr {SemanticRoutines.addConditionalStatementIRCode($compop.text,map);} | 'TRUE' | 'FALSE' 
    ;

compop            
    :    '<' | '>' | '=' | '!=' | '<=' | '>='
    ;


/* For Statements */
init_stmt         
    :    assign_expr | 
    ;

incr_stmt       
    :    assign_expr | 
    ;

for_stmt       
    :    'FOR' {SemanticRoutines.enterForLoop();}'(' init_stmt ';' {SemanticRoutines.makeLabelIRCode(SemanticRoutines.peekSecondLabel());} cond ';' incr_stmt ')' {SymbolTable.createBlockScope();SemanticRoutines.forLoopStart();}  decl aug_stmt_list  'ENDFOR'{SemanticRoutines.endForLoop();}
    ;

aug_stmt_list     
    :    aug_stmt aug_stmt_list | 
    ;

aug_stmt         
    :    base_stmt | aug_if_stmt | for_stmt | 'CONTINUE'';' | 'BREAK'';'
    ;

aug_if_stmt       
    :    'IF'  {SemanticRoutines.pushOnLabelStack(); SemanticRoutines.pushOnLabelStack(); }'(' cond ')' {SymbolTable.createBlockScope();} decl aug_stmt_list  {SemanticRoutines.makeJumpIRCode(SemanticRoutines.peekSecondLabel());SemanticRoutines.makeLabelIRCode(SemanticRoutines.popLabelStack());} aug_elif_part 'ENDIF' {SemanticRoutines.makeLabelIRCode(SemanticRoutines.popLabelStack());} 
    ;

aug_elif_part     
    :    'ELIF'  {SemanticRoutines.pushOnLabelStack();} '(' cond ')' {SymbolTable.createBlockScope();} decl aug_stmt_list {SemanticRoutines.makeJumpIRCode(SemanticRoutines.peekSecondLabel());SemanticRoutines.makeLabelIRCode(SemanticRoutines.popLabelStack());} aug_elif_part | aug_else_part
    ;

aug_else_part     
    :    'ELSE'  {SemanticRoutines.pushOnLabelStack();}{SymbolTable.createBlockScope();} decl aug_stmt_list  {SemanticRoutines.makeJumpIRCode(SemanticRoutines.peekSecondLabel());SemanticRoutines.makeLabelIRCode(SemanticRoutines.popLabelStack());}| 
    ;


/*---------------------------------------------------  LEXER RULES --------------------------------------------------------------------------------------*/

/* Helper fragments */

// Digits
fragment D
    : '0'..'9'
    ;
//Letters
fragment LETTER
    :    [A-Za-z]
    ;       

/* Ordering Important */
KEYWORD
    : 'PROGRAM'|'BEGIN'|'END'|'FUNCTION'|'READ'|'WRITE'|'IF'|'ELIF'|'ELSE'|'ENDIF'|'FOR'|'ENDFOR'|'CONTINUE'|'BREAK'|'RETURN'|'INT'|'VOID'|'STRING'|'FLOAT'
    ;

OPERATOR
    :    '+'|'-'|'*'|'/'|'='|':='|'!='|'='|'<'|'>'|'('|')'|';'|','|'<='|'>='|'OR'|'AND'|'NOT'|'TRUE'|'FALSE'
    ;

// Integers and Floats. Can be positive or negative. 
INTLITERAL
    :    D+
    ;

// Handles both type of floats. And also either positive or negative.
FLOATLITERAL
    : D+'.'D+|'.'D+
    ;

// NOT is '~'. 0 or more occurences of '"' not allowed. LIMIT TO 80 length (NOT YET INCLUDED).  
STRINGLITERAL
    :    '"' ~('"')* '"' 
    ;
// Anything untill end of line is a comment.
COMMENT
    :    '--' ~('\n')*'\n' -> skip
    ;

// Whitespace handler recommended in documentation. Ignore white spaces
WHITESPACE
    :    (' '|'\r'|'\t'|'\n') -> skip
    ; 

// Identifiers. 

IDENTIFIER
    :    LETTER(LETTER|D)* 
    ;
