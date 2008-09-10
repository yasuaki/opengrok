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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opensolaris.opengrok.OpenGrokLogger;
import org.opensolaris.opengrok.util.Executor;

/**
 * Access to a Mercurial repository.
 *
 */
public class MercurialRepository extends Repository {
    private static final String template = "changeset: {rev}:{node|short}\\n{branches}{tags}{parents}user: {author}\\ndate: {date|isodate}\\ndescription:\\n{desc|strip}\\n";

    private static ScmChecker hgBinary = new ScmChecker(new String[] {
        System.getProperty("org.opensolaris.opengrok.history.Mercurial", "hg"),
        "--help" });
    
    /**
     * Get the name of the Mercurial command that should be used
     * @return the name of the hg command in use
     */
    private String getCommand() {
        return System.getProperty("org.opensolaris.opengrok.history.Mercurial", "hg");
    }

    /**
     * Get an executor to be used for retrieving the history log for the
     * named file.
     * 
     * @param file The file to retrieve history for
     * @return An Executor ready to be started
     */
    Executor getHistoryLogExecutor(final File file) {
        String abs = file.getAbsolutePath();
        String filename = "";
        String directoryName = getDirectoryName();
        if (abs.length() > directoryName.length()) {
            filename = abs.substring(directoryName.length() + 1);
        }
        
        List<String> cmd = new ArrayList<String>();
        cmd.add(getCommand());
        cmd.add("log");
        cmd.add("--template");
        StringBuilder sb = new StringBuilder(template);

        if (file.isDirectory()) {
            sb.append("files: {files}{file_copies}\\n");
        }
        cmd.add(sb.toString());
        cmd.add(filename);
        
        return new Executor(cmd, new File(getDirectoryName()));
    }    
    
    public InputStream getHistoryGet(String parent, String basename, String rev) {
        InputStream ret = null;

        String directoryName = getDirectoryName();
        File directory = new File(directoryName);

        String filename =  (new File(parent, basename)).getAbsolutePath().substring(directoryName.length() + 1);
        Process process = null;
        InputStream in = null;
        try {
            String argv[] = {getCommand(), "cat", "-r", rev, filename};
            process = Runtime.getRuntime().exec(argv, null, directory);
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[32 * 1024];
            in = process.getInputStream();
            int len;
            
            while ((len = in.read(buffer)) != -1) {
                if (len > 0) {
                    out.write(buffer, 0, len);
                }
            }
            
            ret = new BufferedInputStream(new ByteArrayInputStream(out.toByteArray()));
        } catch (Exception exp) {
            OpenGrokLogger.getLogger().log(Level.SEVERE, "Failed to get history: " + exp.getClass().toString());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    OpenGrokLogger.getLogger().log(Level.WARNING, "An error occured while closing stream", e);
                }
            }
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
        return MercurialHistoryParser.class;
    }

    public Class<? extends HistoryParser> getDirectoryHistoryParser() {
        return MercurialHistoryParser.class;
    }

    /** Pattern used to extract author/revision from hg annotate. */
    private final static Pattern ANNOTATION_PATTERN =
        Pattern.compile("^\\s*(\\S+)\\s+(\\d+)\\s");

    /**
     * Annotate the specified file/revision.
     *
     * @param file file to annotate
     * @param revision revision to annotate
     * @return file annotation
     */
    public Annotation annotate(File file, String revision) throws IOException {
        ArrayList<String> argv = new ArrayList<String>();
        argv.add(getCommand());
        argv.add("annotate");
        argv.add("-u");
        argv.add("-n");
        argv.add("-f");
        if (revision != null) {
            argv.add("-r");
            argv.add(revision);
        }
        argv.add(file.getName());
        ProcessBuilder pb = new ProcessBuilder(argv);
        pb.directory(file.getParentFile());
        Process process = null;
        BufferedReader in = null;
        Annotation ret = null;
        try {
            process = pb.start();
            in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            ret = new Annotation(file.getName());
            String line;
            int lineno = 0;
            while ((line = in.readLine()) != null) {
                ++lineno;
                Matcher matcher = ANNOTATION_PATTERN.matcher(line);
                if (matcher.find()) {
                    String author = matcher.group(1);
                    String rev = matcher.group(2);
                    ret.addLine(rev, author, true);
                } else {
                    OpenGrokLogger.getLogger().log(Level.SEVERE, "Error: did not find annotation in line " + 
                            lineno + ": [" + line + "]");
                }
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    OpenGrokLogger.getLogger().log(Level.WARNING, "An error occured while closing stream", e);
                }
            }
            if (process != null) {
                try {
                    process.exitValue();
                } catch (IllegalThreadStateException e) {
                    // the process is still running??? just kill it..
                    process.destroy();
                }
            }
        }
        return ret;
    }

    public boolean fileHasAnnotation(File file) {
        return true;
    }

    public boolean isCacheable() {
        return true;
    }
    
    public void update() throws IOException {
        File directory = new File(getDirectoryName());

        List<String> cmd = new ArrayList<String>();
        cmd.add(getCommand());
        cmd.add("showconfig");
        Executor executor = new Executor(cmd, directory);
        if (executor.exec() != 0) {
            throw new IOException(executor.getErrorString());
        }

        if (executor.getOutputString().indexOf("paths.default=") != -1) {
            cmd.clear();
            cmd.add(getCommand());
            cmd.add("pull");
            cmd.add("-u");
            executor = new Executor(cmd, directory);
            if (executor.exec() != 0) {
                throw new IOException(executor.getErrorString());
            }
        }
    }

    public boolean fileHasHistory(File file) {
        // Todo: is there a cheap test for whether mercurial has history
        // available for a file?
        // Otherwise, this is harmless, since mercurial's commands will just
        // print nothing if there is no history.
        return true;
    }
    
    @Override
    boolean isRepositoryFor(File file) {
        File f = new File(file, ".hg");
        return f.exists() && f.isDirectory();
    }

    @Override
    boolean supportsSubRepositories() {
        // The forest-extension in Mercurial adds repositories inside the
        // repositories.
        return true;
    }

    @Override
    protected boolean isWorking() {
        return hgBinary.available;
    }
}
