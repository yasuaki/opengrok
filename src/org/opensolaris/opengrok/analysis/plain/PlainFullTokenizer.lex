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
 * Copyright 2009 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */
package org.opensolaris.opengrok.analysis.plain;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Locale;
import org.opensolaris.opengrok.analysis.JFlexTokenizer;
import org.apache.lucene.analysis.Token;
%%

%public
%class PlainFullTokenizer
%extends JFlexTokenizer
%unicode
%type Token 
%caseless
%switch

%{
  public void close() throws IOException {
  	yyclose();
  }

  public void reInit(char[] buf, int len) {
  	yyreset((Reader) null);
  	zzBuffer = buf;
  	zzEndRead = len;
	zzAtEOF = true;
	zzStartRead = 0;
  }

%}

//WhiteSpace     = [ \t\f\r]+|\n
Identifier = [a-zA-Z_] [a-zA-Z0-9_]*
Number = [0-9]+|[0-9]+\.[0-9]+| "0[xX]" [0-9a-fA-F]+
Printable = [\@\$\%\^\&\-+=\?\.\:]

%%
{Identifier}|{Number}|{Printable} { // below assumes locale from the shell/container, instead of just US
                        reuseToken.reinit(yytext().toLowerCase(Locale.getDefault()), zzStartRead, zzMarkedPos);
                        return reuseToken; }
<<EOF>>   { return null;} 
.|\n	{}
