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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.opensolaris.opengrok.OpenGrokLogger;
import org.opensolaris.opengrok.util.Executor;

/**
 * Access to a Git repository.
 * 
 */
public class GitRepository extends Repository {
    private static ScmChecker gitBinary = new ScmChecker(new String[] {
        getCommand(), "--help" });
    
    Process getHistoryLogProcess(File file) throws IOException {
        String abs = file.getAbsolutePath();
        String filename = "";
        String directoryName = getDirectoryName();
        if (abs.length() > directoryName.length()) {
            filename = abs.substring(directoryName.length() + 1);
        }
        
        String[] argv = new String[] {getCommand(), "log", "--name-only", "--pretty=fuller", filename};

        File directory = new File(getDirectoryName());
        return Runtime.getRuntime().exec(argv, null, directory);        
    }    
    
    /**
     * Get the name of the Git command that should be used
     * @return the name of the hg command in use
     */
    private static String getCommand() {
        return System.getProperty("org.opensolaris.opengrok.history.git", "git");
    }

    public InputStream getHistoryGet(String parent, String basename, String rev) {
        InputStream ret = null;

        String directoryName = getDirectoryName();
        File directory = new File(directoryName);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        
        String filename =  (new File(parent, basename)).getAbsolutePath().substring(directoryName.length() + 1);
        Process process = null;
        try {
            String argv[] = {getCommand(), "show", rev + ":" + filename};
            process = Runtime.getRuntime().exec(argv, null, directory);
            
            InputStream in = process.getInputStream();
            int len;
            
            while ((len = in.read(buffer)) != -1) {
                if (len > 0) {
                    output.write(buffer, 0, len);
                }
            }
            
            ret = new BufferedInputStream(new ByteArrayInputStream(output.toByteArray()));
        } catch (Exception exp) {
            OpenGrokLogger.getLogger().log(Level.SEVERE, "Failed to get history: " + exp.getClass().toString(), exp);
        } finally {
            // Clean up zombie-processes...
            if (process != null) {
                try {
                    process.exitValue();
                } catch (IllegalThreadStateException exp) {
                    // the process is still running??? just kill it..
                    process.destroy();
                }
            }
        }
        
        return ret;
    }
    
    public Class<? extends HistoryParser> getHistoryParser() {
        return GitHistoryParser.class;
    }

    public Class<? extends HistoryParser> getDirectoryHistoryParser() {
        return GitHistoryParser.class;
    }

    /**
     * Annotate the specified file/revision.
     *
     * @param file file to annotate
     * @param revision revision to annotate
     * @return file annotation
     */
    public Annotation annotate(File file, String revision) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean fileHasAnnotation(File file) {
        return false;
    }

    public boolean isCacheable() {
        return true;
    }
    
    public void update() throws IOException {
        File directory = new File(getDirectoryName());
        List<String> cmd = new ArrayList<String>();
        cmd.add(getCommand());
        cmd.add("config");
        cmd.add("--list");

        Executor executor = new Executor(cmd, directory);
        if (executor.exec() != 0) {
            throw new IOException(executor.getErrorString());
        }

        if (executor.getOutputString().indexOf("remote.origin.url=") != -1) {
            cmd.clear();
            cmd.add(getCommand());
            cmd.add("fetch");
            if (executor.exec() != 0) {
                throw new IOException(executor.getErrorString());
            }

            cmd.clear();
            cmd.add(getCommand());
            cmd.add("checkout");
            if (executor.exec() != 0) {
                throw new IOException(executor.getErrorString());
            }
        }
    }

    public boolean fileHasHistory(File file) {
        // Todo: is there a cheap test for whether Git has history
        // available for a file?
        // Otherwise, this is harmless, since Git's commands will just
        // print nothing if there is no history.
        return true;
    }

    @Override
    boolean isRepositoryFor(File file) {
        File f = new File(file, ".git");
        return f.exists() && f.isDirectory();
    }

    @Override
    protected boolean isWorking() {
        return gitBinary.available;
    }
}

