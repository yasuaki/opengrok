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
 * Copyright 2010 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */

package org.opensolaris.opengrok.analysis.document;
import java.io.IOException;
import java.io.Reader;
import org.opensolaris.opengrok.analysis.JFlexTokenizer;


%%

%public
%class TroffFullTokenizer
%extends JFlexTokenizer
%unicode
%type boolean
%eofval{
return false;
%eofval}
%caseless

%{
    public void reInit(char[] buf, int len) {
  	yyreset((Reader) null);
  	zzBuffer = buf;
  	zzEndRead = len;
	zzAtEOF = true;
	zzStartRead = 0;
    }

    @Override
    public void close() throws IOException {
       	yyclose();
    }
%}

//WhiteSpace     = [ \t\f\r]+|\n
Identifier = [a-zA-Z_] [a-zA-Z0-9_]*
Number = [0-9]+|[0-9]+\.[0-9]+| "0[xX]" [0-9a-fA-F]+
Printable = [\@\$\%\^\&\-+=\?\.\:]

%%
^\.(EQ|in|sp|ne|rt|br|pn|ds|de|if|ig|el|ft|hy|ie|ll|ps|rm|ta|ti|NH|DT|EE)[^\n]* {}
^.[a-zA-Z]{1,2} {}
\\f[ABCIR]  {}
^"...\\\"" {}

\\&.        {setAttribs(".", zzStartRead, zzMarkedPos);return true;}
{Identifier}|{Number}|{Printable}	{setAttribs(yytext().toLowerCase(), zzStartRead, zzMarkedPos);return true;}
<<EOF>>   { return false;}
.|\n	{}
