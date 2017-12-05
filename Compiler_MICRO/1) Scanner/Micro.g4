/*If only lexer is wanted then use 'lexer' word before 'grammar' */
grammar Micro;
l:'hello';
/* Helper fragments */

// Digits
fragment D: '0'..'9';
//Letters
fragment LETTER: [A-Za-z];       

/* Ordering Important */
KEYWORD: 'PROGRAM'|'BEGIN'|'END'|'FUNCTION'|'READ'|'WRITE'|'IF'|'ELIF'|'ELSE'|'ENDIF'|'FOR'|'ENDFOR'|'CONTINUE'|'BREAK'|'RETURN'|'INT'|'VOID'|'STRING'|'FLOAT';

OPERATOR: '+'|'-'|'*'|'/'|'='|':='|'!='|'='|'<'|'>'|'('|')'|';'|','|'<='|'>='|'OR'|'AND'|'NOT'|'TRUE'|'FALSE';

// Integers and Floats. Can be positive or negative. 
INTLITERAL: D+;

// Handles both type of floats. And also either positive or negative.
FLOATLITERAL: D+'.'D+|'.'D+;

// NOT is '~'. 0 or more occurences of '"' not allowed. LIMIT TO 80 length (NOT YET INCLUDED).  
STRINGLITERAL: '"' ~('"')* '"' ;
// Anything untill end of line is a comment.
COMMENT:  '--' ~('\n')*'\n' -> skip;

// Whitespace handler recommended in documentation. Ignore white spaces
WHITESPACE: (' '|'\r'|'\t'|'\n') -> skip; 

// Identifiers. 

IDENTIFIER: LETTER(LETTER|D)* ;
