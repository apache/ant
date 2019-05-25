/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
class CalcParser extends Parser;
options {
	buildAST = true;	// uses CommonAST by default
}

expr
	:	mexpr (PLUS^ mexpr)* SEMI!
	;

mexpr
	:	atom (STAR^ atom)*
	;

atom:	INT
	;

class CalcLexer extends Lexer;

WS	:	(' '
	|	'\t'
	|	'\n'
	|	'\r')
		{ _ttype = Token.SKIP; }
	;

LPAREN:	'('
	;

RPAREN:	')'
	;

STAR:	'*'
	;

PLUS:	'+'
	;

SEMI:	';'
	;

protected
DIGIT
	:	'0'..'9'
	;

INT	:	(DIGIT)+
	;

class CalcTreeWalker extends TreeParser;

expr returns [float r]
{
	float a,b;
	r=0;
}
	:	#(PLUS a=expr b=expr)	{r = a+b;}
	|	#(STAR a=expr b=expr)	{r = a*b;}
	|	i:INT			{r = (float)Integer.parseInt(i.getText());}
	;

