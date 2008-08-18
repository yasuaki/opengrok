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
package org.opensolaris.opengrok.history;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;

/**
 * An interface for an external repository. 
 *
 * @author Trond Norbye
 */
public abstract class Repository {
    private String directoryName;

    /**
     * Get a parser capable of getting history log elements from this repository.
     * @return a specialized parser for this kind of repository
     */
    abstract Class<? extends HistoryParser> getHistoryParser();
    
    abstract Class<? extends HistoryParser> getDirectoryHistoryParser();
    
    abstract boolean fileHasHistory(File file);
    
    /**
     * Get an input stream that I may use to read a speciffic version of a
     * named file.
     * @param parent the name of the directory containing the file
     * @param basename the name of the file to get
     * @param rev the revision to get
     * @return An input stream containing the correct revision.
     */
    abstract InputStream getHistoryGet(
            String parent, String basename, String rev);

    /**
     * Checks whether this parser can annotate files.
     *
     * @return <code>true</code> if annotation is supported
     */
    abstract boolean fileHasAnnotation(File file);

    /**
     * Annotate the specified revision of a file.
     *
     * @param file the file to annotate
     * @param revision revision of the file
     * @return an <code>Annotation</code> object
     * @throws java.lang.Exception if an error occurs
     */
    abstract Annotation annotate(File file, String revision) throws Exception;

    /**
     * Check whether the parsed history should be cached.
     *
     * @return <code>true</code> if the history should be cached
     */
    abstract boolean isCacheable();
    
    /**
     * Get the name of the root directory for this repository.
     * @return the name of the root directory
     */
    public String getDirectoryName() {
        return directoryName;
    }

    /**
     * Specify the name of the root directory for this repository.
     * @param directoryName the new name of the root directory
     */
    public void setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
    }

    /**
     * Create a history log cache for all of the files in this repository.
     * Some SCM's have a more optimal way to query the log information, so
     * the concrete repository could implement a smarter way to generate the
     * cache instead of creating it for each file being accessed. The default
     * implementation uses the history parser returned by
     * {@code getDirectoryHistoryParser()} to parse the repository's history.
     * If {@code getDirectoryHistoryParser()} returns {@code null}, this
     * method is a no-op.
     *
     * @throws Exception on error
     */
    void createCache(HistoryCache cache) throws Exception {
        if (!isWorking()) {
            return;
        }
        Class<? extends HistoryParser> pClass = getDirectoryHistoryParser();

        // If we don't have a directory parser, we can't create the cache
        // this way. Just give up and return.
        if (pClass == null) {
            return;
        }

        HistoryParser p = pClass.newInstance();
        File directory = new File(getDirectoryName());
        History history = p.parse(directory, this);
        if (history != null && history.getHistoryEntries() != null) {
            HashMap<String, List<HistoryEntry>> map =
                    new HashMap<String, List<HistoryEntry>>();

            for (HistoryEntry e : history.getHistoryEntries()) {
                for (String s : e.getFiles()) {
                    List<HistoryEntry> list = map.get(s);
                    if (list == null) {
                        list = new ArrayList<HistoryEntry>();
                        map.put(s, list);
                    }
                    list.add(e);
                }
            }

            File root = RuntimeEnvironment.getInstance().getSourceRootFile();
            for (Map.Entry<String, List<HistoryEntry>> e : map.entrySet()) {
                for (HistoryEntry ent : e.getValue()) {
                    ent.strip();
                }
                History hist = new History();
                hist.setHistoryEntries(e.getValue());
                File file = new File(root, e.getKey());
                if (!file.isDirectory()) {
                    cache.store(hist, file);
                }
            }
        }
    }
    
    /**
     * Update the content in this repository by pulling the changes from the
     * upstream repository..
     * @throws Exception if an error occurs.
     */
    abstract void update() throws Exception;
    
    /**
     * Check if this it the right repository type for the given file.
     * 
     * @param file File to check if this is a repository for.
     * @return true if this is the correct repository for this file/directory.
     */
    abstract boolean isRepositoryFor(File file);
    
    /**
     * Returns true if this repository supports sub reporitories (a.k.a. forests).
     * 
     * @return true if this repository supports sub repositories
     */
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    boolean supportsSubRepositories() {
        return false;
    }
    
    /**
     * Returns true if this repository is usable in this context (for SCM
     * systems that use external binaries, the binary must be availabe etc)
     * 
     * @return true if the HistoryGuru may use the repository
     */
    protected boolean isWorking() {
        return true;
    }
}
