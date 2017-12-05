/*If only lexer is wanted then use 'lexer' word before 'grammar' */
grammar Micro;


/*---------------------------------------------------  PARSER RULES (following parser writing conventions "4 spaces")------------------------------------------------*/

/* Global Object Declaration. */
@parser::members {
  SymbolMap map;
}


/* Program */

program           
    :    'PROGRAM'  id 'BEGIN' pgm_body 'END' { /*SymbolTable.printTable();*/SemanticRoutines.identifyVars(map); SemanticRoutines.printIRCode(map); } 
    ; 

id                
    :    IDENTIFIER 
    ;

pgm_body
          
    :    {SymbolTable.createGlobalScope();} decl {map = SymbolTable.popScope();}func_declarations //Need to hold current scope in a map. 
    ;

decl
    :    string_decl decl 
    |    var_decl decl 
    |                 // empty is represented as simple whitespaces in ANTLR grammars. #StackOverflow post 
    ; 



/* Global String Declaration */
string_decl
    :    'STRING' id ':=' str ';' {SymbolTable.insertString($id.text, "STRING", $str.text);}
    ;       

str
    :    STRINGLITERAL 
    ;

/* Variable Declaration */
var_decl          
    :    var_type id_list ';'    {SymbolTable.insertVariableType($id_list.text, $var_type.text);}
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
    :    var_type id  {SymbolTable.insertVariableType($id.text, $var_type.text);}
    ;

param_decl_tail  
    :    ',' param_decl param_decl_tail |
    ;


/* Function Declarations */
func_declarations  
    :    func_decl func_declarations |   
    ;

func_decl         
    : 'FUNCTION' any_type id {SymbolTable.createFunctionScope($id.text);}'('param_decl_list')' 'BEGIN' func_body 'END' 
    ;

func_body 
    :    decl stmt_list 
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
    :    id ':=' expr {SemanticRoutines.postfixToIRList($id.text, SemanticRoutines.infixToPostfixExpression($expr.text), map, false, "");}
    ;

read_stmt         
    :    'READ' '(' id_list ')' ';' {SemanticRoutines.readIRNodes($id_list.text, map);}
    ;

write_stmt        
    :    'WRITE' '(' id_list ')' ';' {SemanticRoutines.writeIRNodes($id_list.text, map);}
    ;

return_stmt       
    :    'RETURN' expr ';' 
    ;

/* Expressions */
expr              
    :    expr_prefix factor //{System.out.println("In expr layer: Expression prefix:"+$expr_prefix.text);System.out.println("factor:"+$factor.text);}
    ;

expr_prefix       
    :    expr_prefix factor addop | //{System.out.println("in expr_prefix layer: factor:"+$factor.text );System.out.println("addop:"+$addop.text);}
    ;

factor            
    :    factor_prefix postfix_expr //{System.out.println("In factor layer: factor prefix:"+$factor_prefix.text);System.out.println("post fix expr:"+$postfix_expr.text);}
    ;

factor_prefix     
    :    factor_prefix postfix_expr mulop |
    ;

postfix_expr     
    :    primary | call_expr //{System.out.println("call_expr:"+$call_expr.text);}
    ;

call_expr          
    :    id '(' expr_list ')'
    ;

expr_list         
    :    expr expr_list_tail | 
    ;

expr_list_tail    
    :    ',' expr expr_list_tail | 
    ;

primary           
    :    '('expr')' | id | INTLITERAL | FLOATLITERAL
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
    :    'ELIF' {SemanticRoutines.pushOnLabelStack();} '(' cond ')' {SymbolTable.createBlockScope();} decl stmt_list  {SemanticRoutines.makeJumpIRCode(SemanticRoutines.peekSecondLabel());SemanticRoutines.makeLabelIRCode(SemanticRoutines.popLabelStack());} elif_part /*{SemanticRoutines.elifStmnt($cond.text);}*/| else_part
    ;

else_part       
    :    'ELSE' {SemanticRoutines.pushOnLabelStack();} {SymbolTable.createBlockScope();} decl stmt_list {SemanticRoutines.makeJumpIRCode(SemanticRoutines.peekSecondLabel());SemanticRoutines.makeLabelIRCode(SemanticRoutines.popLabelStack());} | 
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
    :    expr{SemanticRoutines.conditionalOperatorStack($expr.text);} compop expr {SemanticRoutines.conditionalOperatorStack($expr.text); SemanticRoutines.addConditionalStatementIRCode($compop.text,map);} | 'TRUE' | 'FALSE' 
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
    :    'FOR' {SemanticRoutines.pushOnLabelStack(); SemanticRoutines.pushOnLabelStack(); }'(' init_stmt ';' {SemanticRoutines.makeLabelIRCode(SemanticRoutines.peekSecondLabel());} cond ';' incr_stmt ')' {SymbolTable.createBlockScope();} {SemanticRoutines.forLoopStart();} decl aug_stmt_list  'ENDFOR'{SemanticRoutines.endForLoop($incr_stmt.text, map);}
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
