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
package org.opensolaris.opengrok.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides Ctags by having a running instance of ctags
 *
 * @author Chandan
 */
public class Ctags {

    private Process ctags;
    private OutputStreamWriter ctagsIn;
    private BufferedReader ctagsOut;
    private static final Logger log = Logger.getLogger(Ctags.class.getName());
    private String binary;
    private ProcessBuilder processBuilder;
    private Thread errThread;

    public void setBinary(String binary) {
        this.binary = binary;
    }

    public void close() throws IOException {
        if (ctagsIn != null) {
            ctagsIn.close();
        }
        if (ctags != null) {
            ctags.destroy();
        }
    }

    private void initialize() throws IOException {
        if (processBuilder == null) {
            List<String> command = new ArrayList<String>();
            command.add(binary);
            command.add("--c-kinds=+l");
            command.add("--java-kinds=+l");
            command.add("--sql-kinds=+l");
            command.add("--Fortran-kinds=+L");
            command.add("--C++-kinds=+l");
            command.add("--file-scope=yes");
            command.add("-u");
            command.add("--filter=yes");
            command.add("--filter-terminator=__ctags_done_with_file__\n");
            command.add("--fields=-anf+iKnS");
            command.add("--excmd=pattern");
            command.add("--regex-Asm=/^[ \\t]*(ENTRY|ENTRY2|ALTENTRY)[ \\t]*\\(([a-zA-Z0-9_]+)/\\2/f,function/");  // for assmebly definitions
            processBuilder = new ProcessBuilder(command);
        }

        ctags = processBuilder.start();
        ctagsIn = new OutputStreamWriter(ctags.getOutputStream());
        ctagsOut = new BufferedReader(new InputStreamReader(ctags.getInputStream()));

        final BufferedReader error = new BufferedReader(new InputStreamReader(ctags.getErrorStream()));

        errThread = new Thread(new Runnable() {

            public void run() {
                StringBuilder sb = new StringBuilder();
                try {
                    String s;
                    while ((s = error.readLine()) != null) {
                        sb.append(s);
                        sb.append('\n');
                    }
                } catch (IOException exp) {
                     log.log(Level.WARNING, "Got an exception reading ctags error stream: ", exp);                
                } finally {
                    try {
                        error.close();
                    } catch (IOException exp) {
                        log.log(Level.WARNING, "Got an exception closing error stream: ", exp);
                    }
                }
                if (sb.length() > 0) {
                     log.warning("Error from ctags: " + sb.toString());
                }
            }
        });
        errThread.setDaemon(true);
        errThread.start();
    }

    public Definitions doCtags(String file) throws IOException {
        boolean ctagsRunning = false;
        if (ctags != null) {
            try {
                ctags.exitValue();
                ctagsRunning = false;
                // ctags is dead! we must restart!!!
            } catch (IllegalThreadStateException exp) {
                ctagsRunning = true;
                // ctags is still running :)
            }
        }

        if (!ctagsRunning) {
            initialize();
        }

        Definitions ret = null;
        if (file.length() > 0 && !"\n".equals(file)) {
            //log.fine("doing >" + file + "<");
            ctagsIn.write(file);
            ctagsIn.flush();
            ret = new Definitions();
            readTags(ret);
        }

        return ret;
    }

    private void readTags(Definitions defs) {
        try {
            do {
                String tagLine = ctagsOut.readLine();
                //log.fine("Tagline:-->" + tagLine+"<----ONELINE");
                if (tagLine == null) {
                    log.warning("Unexpected end of file!");
                    try {
                        int val = ctags.exitValue();
                        log.warning("ctags exited with code: " + val);
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Ctags problem: ", e);
                    }
                    log.fine("Ctag read");
                    return;
                }

                if ("__ctags_done_with_file__".equals(tagLine)) {
                    return;
                }
                int p = tagLine.indexOf('\t');
                if (p <= 0) {
                    //log.fine("SKIPPING LINE - NO TAB");
                    continue;
                }
                String def = tagLine.substring(0, p);
                int mstart = tagLine.indexOf('\t', p + 1);
                String lnum = "-1";
                String signature = null;
                String kind = null;
                String inher = null;

                int lp = tagLine.length();
                while ((p = tagLine.lastIndexOf('\t', lp - 1)) > 0) {
                    //log.fine(" p = " + p + " lp = " + lp);
                    String fld = tagLine.substring(p + 1, lp);
                    //log.fine("FIELD===" + fld);
                    lp = p;
                    if (fld.startsWith("line:")) {
                        int sep = fld.indexOf(':');
                        lnum = fld.substring(sep + 1);
                    } else if (fld.startsWith("signature:")) {
                        int sep = fld.indexOf(':');
                        signature = fld.substring(sep + 1);
                    } else if (fld.indexOf(':') < 0) {
                        kind = fld;
                        break;
                    } else {
                        inher = fld;
                    }
                }

                final String match;
                if ((p > 0) && (p - mstart > 6)) {
                    match = tagLine.substring(mstart + 3, p - 4).
                            replaceAll("\\/", "/").replaceAll("[ \t]+", " ");
                } else {
                    continue;
                }

                final String type =
                        inher == null ? kind : kind + " in " + inher;
                defs.addTag(Integer.parseInt(lnum), def, type, match);
                if (signature != null) {
                    String[] args = signature.split("[ ]*[^a-z0-9_]+[ ]*");
                    for (String arg : args) {
                        //log.fine("Param = "+ arg);
                        int space = arg.lastIndexOf(' ');
                        if (space > 0 && space < arg.length()) {
                            if (arg.charAt(space + 1) == '*') {
                                int ptr = arg.lastIndexOf('*');
                                if (ptr > 0) {
                                    space = ptr;
                                }
                            }
                            String argDef = arg.substring(space + 1);
                            //log.fine("Param Def = "+ argDef);
                            defs.addTag(Integer.valueOf(lnum), argDef,
                                    "argument", def + signature);
                        }
                    }
                }
            //log.fine("Read = " + def + " : " + lnum + " = " + kind + " IS " + inher + " M " + match);
            } while (true);
        } catch (Exception e) {
            log.log(Level.WARNING, "CTags parsing problem: ", e);
        }
        log.severe("CTag reader cycle was interrupted!");
    }
}
