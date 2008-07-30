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

package org.opensolaris.opengrok.history;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Access to an RCS repository.
 */
public class RCSRepository extends Repository {

    @Override
    Class<? extends HistoryParser> getHistoryParser() {
        return RCSHistoryParser.class;
    }

    @Override
    Class<? extends HistoryParser> getDirectoryHistoryParser() {
        return null;
    }

    @Override
    boolean fileHasHistory(File file) {
        return getRCSFile(file).exists();
    }

    @Override
    InputStream getHistoryGet(String parent, String basename, String rev) {
        try {
            return new RCSget(new File(parent, basename).getPath(), rev);
        } catch (IOException ioe) {
            System.err.println("Failed to retrieve revision " + rev +
                               " of " + basename);
            ioe.printStackTrace();
            return null;
        }
    }

    @Override
    boolean fileHasAnnotation(File file) {
        // TODO
        return false;
    }

    @Override
    Annotation annotate(File file, String revision) throws Exception {
        // TODO
        return null;
    }

    @Override
    boolean isCacheable() {
        return false;
    }

    @Override
    void update() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    boolean isRepositoryFor(File file) {
        File rcsDir = new File(file, "RCS");
        return rcsDir.isDirectory();
    }

    /**
     * Get a {@code File} object that points to the file that contains
     * RCS history for the specified file.
     *
     * @param file the file whose corresponding RCS file should be found
     * @return the file which contains the RCS history
     */
    File getRCSFile(File file) {
        File dir = new File(file.getParentFile(), "RCS");
        String baseName = file.getName();
        return new File(dir, baseName + ",v");
    }
}
