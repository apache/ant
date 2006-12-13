// Not really a great extension, but it is only a test after all!

class ExtendedCalcParser extends CalcParser;

exprList
    : LPAREN (expr)* RPAREN
    ;
