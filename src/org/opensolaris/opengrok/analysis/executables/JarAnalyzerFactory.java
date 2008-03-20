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
 * Copyright 2007 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */

package org.opensolaris.opengrok.analysis.executables;

import org.opensolaris.opengrok.analysis.FileAnalyzer;
import org.opensolaris.opengrok.analysis.FileAnalyzer.Genre;
import org.opensolaris.opengrok.analysis.FileAnalyzerFactory;

public class JarAnalyzerFactory extends FileAnalyzerFactory {
    public static final JarAnalyzerFactory DEFAULT_INSTANCE =
            new JarAnalyzerFactory();

    private static final String[] SUFFIXES = {
        "JAR", "WAR", "EAR"
    };

    private JarAnalyzerFactory() {
        // no magics for jar files, ZipAnalyzerFactory will handle it for us
        super(SUFFIXES, null, null, null, Genre.XREFABLE);
    }

    @Override
    protected FileAnalyzer newAnalyzer() {
        return new JarAnalyzer(this);
    }
}
