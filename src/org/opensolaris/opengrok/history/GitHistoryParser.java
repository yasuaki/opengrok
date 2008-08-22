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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import org.opensolaris.opengrok.OpenGrokLogger;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;

/**
 * Parse a stream of Git log comments.
 */
class GitHistoryParser implements HistoryParser {

    private enum ParseState {
        HEADER, MESSAGE, FILES
    };      
            
            
    public History parse(File file, Repository repos)
            throws IOException {

        GitRepository mrepos = (GitRepository) repos;
        History history = new History();
        
        Process process = null;
        BufferedReader in = null;
        try {
            process = mrepos.getHistoryLogProcess(file);
            if (process == null) {
                return null;
            }

            SimpleDateFormat df =
                    new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy ZZZZ", Locale.getDefault());
            ArrayList<HistoryEntry> entries = new ArrayList<HistoryEntry>();

            InputStream is = process.getInputStream();
            in = new BufferedReader(new InputStreamReader(is));
            String mydir = mrepos.getDirectoryName() + File.separator;
            int rootLength = RuntimeEnvironment.getInstance().getSourceRootPath().length();
            String s;
            HistoryEntry entry = null;
            ParseState state = ParseState.HEADER;
            while ((s = in.readLine()) != null) {
                if (state == ParseState.HEADER) {

                    if (s.startsWith("commit")) {
                        if (entry != null) {
                            entries.add(entry);
                        }
                        entry = new HistoryEntry();
                        entry.setActive(true);
                        String commit = s.substring("commit".length()).trim();
                        entry.setRevision(commit);
                    } else if (s.startsWith("Author:") && entry != null) {
                        entry.setAuthor(s.substring("Author:".length()).trim());
                    } else if (s.startsWith("AuthorDate:") && entry != null) {
                        Date date = new Date();
                        try {
                            df.parse(s.substring("AuthorDate:".length()).trim());
                        } catch (ParseException pe) {
                            OpenGrokLogger.getLogger().log(Level.INFO, "Failed to parse author date: " + s, pe);
                        }
                        entry.setDate(date);
                    } else if (s.trim().equals("")) {
                        // We are done reading the heading, start to read the message
                        state = ParseState.MESSAGE;
                    }

                }
                if (state == ParseState.MESSAGE) {
                    if (s.trim().startsWith("git-svn-id:")) {
                        // file listing
                        state = ParseState.FILES;
                        
                        // next line is empty, discard it
                        s = in.readLine();
                        continue;
                    } else if (entry != null) {
                        entry.appendMessage(s);
                    }
                }
                if (state == ParseState.FILES) {
                    if (s.trim().equals("")) {
                        state = ParseState.HEADER;
                    } else {
                        if (entry != null) {
                            File f = new File(mydir, s);
                            String name = f.getCanonicalPath().substring(rootLength);
                            entry.addFile(name);
                        }
                    }
                }
            }

            if (entry != null) {
                entries.add(entry);
            }

            history.setHistoryEntries(entries);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }

            if (process != null) {
                try {
                    process.exitValue();
                } catch (IllegalThreadStateException exp) {
                    // the process is still running??? just kill it..
                    process.destroy();
                }
            }
        }

        return history;
    }
}
