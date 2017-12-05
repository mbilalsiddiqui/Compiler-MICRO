/*If only lexer is wanted then use 'lexer' word before 'grammar' */
grammar Micro;


/*---------------------------------------------------  PARSER RULES (following parser writing conventions "4 spaces")------------------------------------------------*/

/* Program */
program           
    :    'PROGRAM' id 'BEGIN' pgm_body 'END' 
    ; 

id                
    :    IDENTIFIER 
    ;

pgm_body          
    :    decl func_declarations 
    ;

decl
    :    string_decl decl 
    |    var_decl decl 
    |                 // empty is represented as simple whitespaces in ANTLR grammars. #StackOverflow post 
    ; 



/* Global String Declaration */
string_decl
    :    'STRING' id ':=' str ';' 
    ;       

str
    :    STRINGLITERAL 
    ;

/* Variable Declaration */
var_decl          
    :    var_type id_list ';' 
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
    :    var_type id
    ;

param_decl_tail  
    :    ',' param_decl param_decl_tail |
    ;


/* Function Declarations */
func_declarations  
    :    func_decl func_declarations |   
    ;

func_decl         
    : 'FUNCTION' any_type id '('param_decl_list')' 'BEGIN' func_body 'END' 
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
    :    id ':=' expr 
    ;

read_stmt         
    :    'READ' '(' id_list ')' ';' 
    ;

write_stmt        
    :    'WRITE' '(' id_list ')' ';' 
    ;

return_stmt       
    :    'RETURN' expr ';' 
    ;

/* Expressions */
expr              
    :    expr_prefix factor
    ;

expr_prefix       
    :    expr_prefix factor addop |
    ;

factor            
    :    factor_prefix postfix_expr
    ;

factor_prefix     
    :    factor_prefix postfix_expr mulop |
    ;

postfix_expr     
    :    primary | call_expr
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
    :    'IF' '(' cond ')' decl stmt_list elif_part 'ENDIF'
    ;

elif_part         
    :    'ELIF' '(' cond ')' decl stmt_list elif_part | else_part
    ;

else_part       
    :    'ELSE' decl stmt_list | 
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
    :    expr compop expr | 'TRUE' | 'FALSE'
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
    :    'FOR' '(' init_stmt ';' cond ';' incr_stmt ')' decl aug_stmt_list 'ENDFOR'
    ;

aug_stmt_list     
    :    aug_stmt aug_stmt_list | 
    ;

aug_stmt         
    :    base_stmt | aug_if_stmt | for_stmt | 'CONTINUE'';' | 'BREAK'';'
    ;

aug_if_stmt       
    :    'IF' '(' cond ')' decl aug_stmt_list aug_elif_part 'ENDIF'
    ;

aug_elif_part     
    :    'ELIF' '(' cond ')' decl aug_stmt_list aug_elif_part | aug_else_part
    ;

aug_else_part     
    :    'ELSE' decl aug_stmt_list | 
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
