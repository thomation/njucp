parser grammar SysYParser;

options {
	tokenVocab = SysYLexer;
}
program: compUnit;

// 编译单元 CompUnit → [ CompUnit ] ( Decl | FuncDef ) 
compUnit: (funcDef | decl)+ EOF;

// 声明 Decl → ConstDecl | VarDecl 
decl: constDecl | varDecl;
// 常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';' 
constDecl: CONST btype constDef (COMMA constDef)* SEMICOLON;
// 基本类型 BType → 'int'
btype: INT;
// 常数定义 ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
constDef:
	IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal;
// 常量初值 ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}
constInitVal:
	constExp
	| L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE;
// 变量声明 VarDecl → BType VarDef { ',' VarDef } ';'
varDecl: btype varDef (COMMA varDef)* SEMICOLON;
// 变量定义 VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
varDef:
	IDENT (L_BRACKT constExp R_BRACKT)*
	| IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN initVal;
// 变量初值 InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
initVal: exp | L_BRACE (initVal (COMMA initVal)*)? R_BRACE;
// 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block 
funcDef: funcType IDENT L_PAREN funcFParams? R_PAREN block;
// 函数类型 FuncType → 'void' | 'int
funcType: VOID | INT;
// 函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
funcFParams: funcFParam (COMMA funcFParam)*;
// 函数形参 FuncFParam → BType Ident ['[' ']' { '[' Exp ']' }] 
funcFParam:
	btype IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)?;
// 语句块 Block → '{' { BlockItem } '}' 
block: L_BRACE blockItem* R_BRACE;
// 语句块项 BlockItem → Decl | Stmt
blockItem: decl | stmt;
// 语句 Stmt → LVal '=' Exp ';' | [Exp] ';' | Block | 'if' '( Cond ')' Stmt [ 'else' Stmt ] | 'while'
// '(' Cond ')' Stmt | 'break' ';' | 'continue' ';' | 'return' [Exp] ';'
stmt:
	lVal ASSIGN exp SEMICOLON
	| exp? SEMICOLON
	| block
	| IF L_PAREN cond R_PAREN stmt (ELSE stmt)?
	| WHILE L_PAREN cond R_PAREN stmt
	| BREAK SEMICOLON
	| CONTINUE SEMICOLON
	| RETURN exp? SEMICOLON;
exp:
	L_PAREN exp R_PAREN
	| lVal
	| number
	| IDENT L_PAREN funcRParams? R_PAREN
	| unaryOp exp
	| exp (MUL | DIV | MOD) exp
	| exp (PLUS | MINUS) exp;

cond:
	exp
	| cond (LT | GT | LE | GE) cond
	| cond (EQ | NEQ) cond
	| cond AND cond
	| cond OR cond;

lVal: IDENT (L_BRACKT exp R_BRACKT)*;

number: INTEGER_CONST;

unaryOp: PLUS | MINUS | NOT;

funcRParams: param (COMMA param)*;

param: exp;

constExp: exp;