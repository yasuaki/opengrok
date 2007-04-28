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
 * Copyright 2005 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */

/*
 * ident	"@(#)CAnalyzer.java 1.1     05/11/11 SMI"
 */

package org.opensolaris.opengrok.analysis.c;

import org.apache.lucene.document.*;
import org.apache.lucene.analysis.*;
import java.io.*;
import org.opensolaris.opengrok.analysis.*;
import org.opensolaris.opengrok.analysis.plain.*;
import org.opensolaris.opengrok.history.Annotation;

/**
 * An Analyzer for C/C++/Java type of files
 *
 * Created on September 21, 2005
 * @author Chandan
 */
public class CAnalyzer extends PlainAnalyzer {
    /** Creates a new instance of CAnalyzer */
    CSymbolTokenizer cref;
    CXref xref;
    Reader dummy = new StringReader("");
    public static String[] suffixes = {
        "C",
                "H",
                "CPP",
                "CC",
                "C++",
                "HH",
                "JAVA",
                "I",
                "CXX",
                "L",
                "Y",
                "LEX",
                "YACC",
                "D",
                "S",
                "XS",	// Mainly found in perl directories
		"PHP"
    };
    public static String[] magics = {
        "/*"
    };
    
    public CAnalyzer() {
        super();
        cref = new CSymbolTokenizer(dummy);
        xref = new CXref(dummy);
    }

    public void analyze(Document doc, InputStream in) {
        super.analyze(doc, in);
        doc.add(Field.Text("refs", dummy));
    }    
    
    public TokenStream tokenStream(String fieldName, Reader reader) {
        if("refs".equals(fieldName)) {
            cref.reInit(super.content, super.len);
            return cref;
        }
        return super.tokenStream(fieldName, reader);
    }
    
    public static String[] getMagics() {
        return magics;
    }
    
    public static String[] getSuffixes() {
        return suffixes;
    }

    /**
     * Write a cross referenced HTML file.
     * @param out Writer to write HTML cross-reference
     */
    public void writeXref(Writer out) throws IOException {
        xref.reInit(content, len);
        xref.setDefs(defs);
        xref.write(out);
        //lines = xref.getLine();
    }
    
    /**
     * Write a cross referenced HTML file reads the source from in
     * @param in Input source
     * @param out Output xref writer
     * @param annotation annotation for the file (could be null)
     */
    public static void writeXref(InputStream in, Writer out,
                                 Annotation annotation) throws IOException {
        CXref xref = new CXref(in);
        xref.annotation = annotation;
        xref.write(out);
    }
}
