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
 * ident	"@(#)IgnoredNames.java 1.2     06/02/22 SMI"
 */
package org.opensolaris.opengrok.index;

import java.io.File;
import java.util.*;
import java.io.FileFilter;
import org.apache.oro.io.GlobFilenameFilter;

/**
 * Comment that describes the contents of this IgnoredNames.java
 * Created on November 8, 2005
 *
 * @author Chandan
 */
public class IgnoredNames {
    public static final String[] IGNORE = {
        "SCCS",
        "CVS",
        "RCS",
        "cscope.in.out",
        "cscope.out.po",
        "cscope.out.in",
        "cscope.po.out",
        "cscope.po.in",
        "cscope.files",
        "cscope.out",
        "Codemgr_wsdata",
        ".cvsignore",
        "CVSROOT",
        "TAGS",
        "tags",
        ".svn",
        ".hg",
        ".hgtags",
        "*~",
    };
    private static Set<String> ignore = new HashSet<String>();
    private static List<FileFilter> patterns = new ArrayList<FileFilter>();
    
    static {
        for(String ig : IGNORE) {
            add(ig);
        }
    }
    
    public static FileFilter glob = new GlobFilenameFilter("*");
    
    public static void add(String pattern) {
        if (pattern.indexOf('*') != -1 || pattern.indexOf('?') != -1) {
            patterns.add(new GlobFilenameFilter(pattern));
        } else {
            ignore.add(pattern);
        }
    }
    
    public static boolean ignore(File file) {
        boolean ret = false;
        
        if (ignore.contains(file.getName())) {
            ret = true;
        } else {
            for (FileFilter fe : patterns) {
                if (fe.accept(file)) {
                    ret = true;
                    break;
                }
            }
        }
        
        return ret;        
    }
    
    public static boolean ignore(String name) {
        return ignore(new File(name));
    }
}
