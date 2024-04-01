grammar FilterGrammar;

// Parser Rules

filter : expression EOF ;

expression
    : notOpExpression                         # NotOperationExpression
    | '(' expression ')'                      # ParenExpression
    | expression logicalOperator expression   # LogicalExpression
    | predicate                               # PredicateExpression
    ;

notOpExpression
    : NOT expression   # NotExpression
    ;

predicate
    : field operator value                           # SimplePredicate
    | field NULL_OPERATOR                            # NullPredicate
    | field NOT_NULL_OPERATOR                        # NotNullPredicate
    | field IN_OPERATOR valueList                    # InListPredicate
    | field NOT_IN_OPERATOR valueList                # NotInListPredicate
    | HAVING field '(' expression ')'                # HavingPredicate
    | HAVING function '(' field ')' operator NUMBER  # HavingFunctionPredicate
    ;

field
    : ESCAPED_STRING  # FieldName
    ;

value
    : ESCAPED_STRING  # StringValue
    | NUMBER          # NumericValue
    | DATETIME        # DateTimeValue
    | DATE            # DateValue
    | YEAR            # YearValue
    | MONTH_DAY       # MonthDayValue
    | TIME            # TimeValue
    ;

valueList
    : '[' value (',' value)* ']' # Values
    ;

operator
    : EQUAL                  # Eq
    | NOT_EQUAL              # Ne
    | GREATER_THAN           # Gt
    | GREATER_THAN_OR_EQUAL  # Gte
    | LESS_THAN              # Lt
    | LESS_THAN_OR_EQUAL     # Lte
    | LIKE                   # Like
    ;

logicalOperator
    : AND  # And
    | OR   # Or
    ;

function : 'COUNT' | 'count' | 'SUM' | 'sum' | 'AVG' | 'avg' | 'MIN' | 'min' | 'MAX' | 'max' ;

// Lexer Rules

NULL_OPERATOR : '$null:' ;
NOT_NULL_OPERATOR : '$notnull:' ;
EQUAL : '$eq:' ;
NOT_EQUAL : '$ne:' ;
GREATER_THAN : '$gt:' ;
GREATER_THAN_OR_EQUAL : '$gte:' ;
LESS_THAN : '$lt:' ;
LESS_THAN_OR_EQUAL : '$lte:' ;
LIKE : '$like:' ;
AND : '$and:' ;
OR : '$or:' ;
IN_OPERATOR : '$in:' ;
NOT_IN_OPERATOR : '$nin:' ;
NOT : '$not:' ;
HAVING : '$having:' ;

NUMBER : '-'? [0-9]+ ( '.' [0-9]+ )? ;

ESCAPED_STRING
    : (NORMAL_CHAR | ESCAPED_SEQUENCE | ASTERISK | QUESTION_MARK)+
    ;

DATETIME
    : DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT 'T' DIGIT DIGIT ':' DIGIT DIGIT ':' DIGIT DIGIT ('.' FRACTION)? ('Z')?
    ;

DATE
    : DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT
    ;

YEAR
    : ('-')? DIGIT DIGIT DIGIT DIGIT '--'
    ;

MONTH_DAY
    : ('--')? DIGIT DIGIT '-' DIGIT DIGIT
    ;

TIME
    : HOUR ':' MINUTE ( ':' SECOND ( '.' FRACTION )? )?
    ;

fragment HOUR
    : DIGIT DIGIT
    ;

fragment MINUTE
    : DIGIT DIGIT
    ;

fragment SECOND
    : DIGIT DIGIT
    ;

fragment FRACTION
    : DIGIT+   // one or more digits to represent the fraction part
    ;

fragment NORMAL_CHAR
    : ~[()*,?$[\]:-] // Match any character except reserved and special characters
    ;

fragment DIGIT : [0-9];  // Define a fragment for a single digit

fragment ESCAPED_SEQUENCE
    : '$$' // escape for $
    | '$*' // escape for *
    | '$?' // escape for ?
    | '$(' // escape for (
    | '$)' // escape for )
    | '$,' // escape for ,
    | '$[' // escape for [
    | '$]' // escape for ]
    | '$:' // escape for :
    | '$-' // escape for -
    | '$ ' // escape for space
    ;

fragment ASTERISK
    : '*' // standalone asterisk
    ;

fragment QUESTION_MARK
    : '?' // standalone question mark
    ;

// Skip spaces, tabs, newlines, etc., if present in input between tokens
WS : [ \t\n\r]+ -> skip ;
