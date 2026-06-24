grammar Simple;

program
    : PROGRAM ID BRACKET_OPEN sentence* BRACKET_CLOSE EOF
    ;

sentence
    : println
    | conditional
    | var_decl
    | var_assign
    | do_while
    ;

do_while
    : DO BRACKET_OPEN sentence* BRACKET_CLOSE WHILE PAR_OPEN comparison PAR_CLOSE SEMICOLON
    ;

println
    : PRINTLN comparison SEMICOLON
    ;

// Jerarquía: lógico → relacional → aritmético
// Así los operadores lógicos tienen menor precedencia que los relacionales.

comparison
    : left=logicalOr
      ( ( GT | LT | GEQ | LEQ | EQ | NEQ ) right=logicalOr )?
    ;

logicalOr
    : logicalAnd ( OR logicalAnd )*
    ;

logicalAnd
    : logicalNot ( AND logicalNot )*
    ;

logicalNot
    : NOT logicalNot
    | expression
    ;

conditional
    : IF PAR_OPEN comparison PAR_CLOSE
      BRACKET_OPEN sentence* BRACKET_CLOSE
      ELSE
      BRACKET_OPEN sentence* BRACKET_CLOSE
    ;

var_decl
    : VAR ID SEMICOLON
    ;

var_assign
    : ID ASSIGN comparison SEMICOLON
    ;

expression
    : term ( ( PLUS | MINUS ) term )*
    ;

term
    : factor ( ( MULT | DIV ) factor )*
    ;

factor
    : INTEGER
    | REAL
    | MINUS factor
    | NOT factor
    | ID
    | BOOLEAN
    | STRING
    | PAR_OPEN comparison PAR_CLOSE
    ;


PROGRAM : 'program';
VAR     : 'var';
PRINTLN : 'println';
IF      : 'if';
ELSE    : 'else';
DO      : 'do';
WHILE   : 'while';

PLUS  : '+';
MINUS : '-';
MULT  : '*';
DIV   : '/';

AND : '&&';
OR  : '||';
NOT : '!';

GT  : '>';
LT  : '<';
GEQ : '>=';
LEQ : '<=';
EQ  : '==';
NEQ : '!=';

ASSIGN : '=';

BRACKET_OPEN  : '{';
BRACKET_CLOSE : '}';
PAR_OPEN      : '(';
PAR_CLOSE     : ')';
SEMICOLON     : ';';

BOOLEAN : 'true' | 'false';
ID      : [a-zA-Z_][a-zA-Z0-9_]*;
INTEGER : [0-9]+;
REAL    : [0-9]+ '.' [0-9]+;
STRING  : '"' (~["\r\n])* '"';

BLOCK_COMMENT : '/*' .*? '*/' -> skip;
LINE_COMMENT  : '//' ~[\r\n]* -> skip;
WS            : [ \t\n\r]+ -> skip;
