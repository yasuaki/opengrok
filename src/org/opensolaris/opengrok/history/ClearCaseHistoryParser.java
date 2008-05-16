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
package org.opensolaris.opengrok.history;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Parse a stream of ClearCase log comments.
 */
class ClearCaseHistoryParser implements HistoryParser {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyyMMdd.HHmmss");

    public History parse(File file, ExternalRepository repos)
            throws IOException, ParseException {
        ClearCaseRepository mrepos = (ClearCaseRepository)repos;
        History history = new History();

        Exception exception = null;
        Process process = null;
        try {
            process = mrepos.getHistoryLogProcess(file);
            if (process == null) {
                return null;
            }

            ArrayList<HistoryEntry> entries = new ArrayList<HistoryEntry>();

            InputStream is = process.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));

            String s;
            HistoryEntry entry = null;
            while ((s = in.readLine()) != null) {
                if(!s.equals("create version") &&
                   !s.equals("create directory version"))
                {
                    // skip this history entry
                    while ((s = in.readLine()) != null && !s.equals("."))
                    {
                        // skip .. skip ..
                    }
                    continue;
                }

                entry = new HistoryEntry();
                if((s = in.readLine()) != null)
                {
                    entry.setDate(FORMAT.parse(s));
                }
                if((s = in.readLine()) != null)
                {
                    entry.setAuthor(s);
                }
                if((s = in.readLine()) != null)
                {
                    s = s.replace('\\', '/');
                    entry.setRevision(s);
                }

                StringBuffer message = new StringBuffer();
                String glue = "";
                while ((s = in.readLine()) != null && !s.equals("."))
                {
                    if(s.equals(""))
                    {
                        // avoid empty lines in comments
                        continue;
                    }
                    message.append(glue);
                    message.append(s.trim());
                    glue = "\n";
                }
                entry.setMessage(message.toString());

                entry.setActive(true);

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
