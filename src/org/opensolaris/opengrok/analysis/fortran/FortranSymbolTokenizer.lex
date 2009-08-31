/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").  
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright 2008 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */
package org.opensolaris.opengrok.analysis.fortran;

import java.io.Reader;
import org.opensolaris.opengrok.analysis.JFlexTokenizer;
import org.apache.lucene.analysis.Token;


%%
%public
%class FortranSymbolTokenizer
%extends JFlexTokenizer
%unicode
%type Token 

%{
  private Token reuseToken=new Token();

  public void close() {
  }

  public void reInit(char[] buf, int len) {
  	yyreset((Reader) null);
  	zzBuffer = buf;
  	zzEndRead = len;
	zzAtEOF = true;
	zzStartRead = 0;
  }
%}
Identifier = [a-zA-Z_] [a-zA-Z0-9_]*
Label = [0-9]+

%state STRING COMMENT SCOMMENT QSTRING

%%

<YYINITIAL> {
 ^{Label} { }
 ^[^ \t\f\r\n]+	{ yybegin(SCOMMENT); }
{Identifier} {String id = yytext();
		if(!Consts.kwd.contains(id.toLowerCase())) {
                        reuseToken.reinit(zzBuffer, zzStartRead, zzMarkedPos-zzStartRead, zzStartRead, zzMarkedPos);
                        return reuseToken; }
              }
 \"	{ yybegin(STRING); }
 \'	{ yybegin(QSTRING); }
 \!	{ yybegin(SCOMMENT); }
}

<STRING> {
 \"	{ yybegin(YYINITIAL); }
\\\\ | \\\"	{}
}

<QSTRING> {
 \'	{ yybegin(YYINITIAL); }
}

<COMMENT> {
"*/"	{ yybegin(YYINITIAL);}
}

<SCOMMENT> {
\n	{ yybegin(YYINITIAL);}
}

<YYINITIAL, STRING, COMMENT, SCOMMENT, QSTRING> {
<<EOF>>   { return null;} 
.|\n	{}
}
