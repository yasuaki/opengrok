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
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;

/**
 * Parse a stream of mercurial log comments.
 */
class MercurialHistoryParser implements HistoryParser {
    
    public History parse(File file, Repository repos)
            throws IOException, ParseException {
        MercurialRepository mrepos = (MercurialRepository)repos;
        History history = new History();
        
        Exception exception = null;
        Process process = null;
        try {
            process = mrepos.getHistoryLogProcess(file);
            if (process == null) {
                return null;
            }
            
            SimpleDateFormat df =
                    new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy ZZZZ");
            ArrayList<HistoryEntry> entries = new ArrayList<HistoryEntry>();
            
            InputStream is = process.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String mydir = mrepos.getDirectoryName() + File.separator;
            int rootLength = RuntimeEnvironment.getInstance().getSourceRootPath().length();
            String s;
            boolean description = false;
            HistoryEntry entry = null;
            while ((s = in.readLine()) != null) {
                if (s.startsWith("changeset:")) {
                    if (entry != null) {
                        entries.add(entry);
                    }
                    entry = new HistoryEntry();
                    entry.setActive(true);
                    String rev = s.substring("changeset:".length()).trim();
                    if (rev.indexOf(':') != -1) {
                        rev = rev.substring(0, rev.indexOf(':'));
                    }
                    entry.setRevision(rev);
                    description = false;
                } else if (s.startsWith("user:") && entry != null) {
                    entry.setAuthor(s.substring("user:".length()).trim());
                    description = false;
                } else if (s.startsWith("date:") && entry != null) {
                    Date date = df.parse(s.substring("date:".length()).trim());
                    entry.setDate(date);
                    description = false;
                } else if (s.startsWith("files:") && entry != null) {
                    description = false;
                    String[] strings = s.split(" ");
                    for (int ii = 1; ii < strings.length; ++ii) {
                        if (strings[ii].length() > 0) {
                            File f = new File(mydir, strings[ii]);
                            String name = f.getCanonicalPath().substring(rootLength);
                            entry.addFile(name);
                        }
                    }
                } else if (s.startsWith("summary:") && entry != null) {
                    entry.setMessage(s.substring("summary:".length()).trim());
                    description = false;
                } else if (s.startsWith("description:") && entry != null) {
                    description = true;
                } else if (description && entry != null) {
                    entry.appendMessage(s);
                }
            }
            
            if (entry != null) {
                entries.add(entry);
            }
            
            history.setHistoryEntries(entries);            
        } catch (Exception e) {
            exception = e;
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
        
        if (exception != null) {
            if (exception instanceof IOException) {
                throw (IOException)exception;
            } else if (exception instanceof ParseException) {
                throw (ParseException)exception;
            } else {
                System.err.println("Got exception while parsing history for: " + file.getAbsolutePath());
                exception.printStackTrace();
            }
        }
        
        return history;
    }
}
