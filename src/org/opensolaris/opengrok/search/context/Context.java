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
 * Copyright 2010 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */

/**
 * This is supposed to get the matching lines from sourcefile.
 * since lucene does not easily give the match context.
 */
package org.opensolaris.opengrok.search.context;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.apache.lucene.search.Query;
import org.opensolaris.opengrok.OpenGrokLogger;
import org.opensolaris.opengrok.analysis.Definitions;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;
import org.opensolaris.opengrok.search.Hit;
import org.opensolaris.opengrok.web.Util;

public class Context {

    private final LineMatcher[] m;
    static final int MAXFILEREAD = 1024 * 1024;
    private char[] buffer;
    PlainLineTokenizer tokens;
    String queryAsURI;
    private static Set<String> tokenFields = new HashSet<String>(3);

    static {
        tokenFields.add("full");
        tokenFields.add("refs");
        tokenFields.add("defs");
    }

    /**
     * Constructs a context generator
     * @param query the query to generate the result for
     * @param queryStrings map from field names to queries against the fields
     */
    public Context(Query query, Map<String, String> queryStrings) {
        QueryMatchers qm = new QueryMatchers();
        m = qm.getMatchers(query, tokenFields);
        if (m != null) {
            buildQueryAsURI(queryStrings);
            //System.err.println("Found Matchers = "+ m.length + " for " + query);
            buffer = new char[MAXFILEREAD];
            tokens = new PlainLineTokenizer((Reader) null);
        }
    }

    public boolean isEmpty() {
        return m == null;
    }

    /**
     * Build the {@code queryAsURI} string that holds the query in a form
     * that's suitable for sending it as part of a URI.
     *
     * @param subqueries a map containing the query text for each field
     */
    private void buildQueryAsURI(Map<String, String> subqueries) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : subqueries.entrySet()) {
            String field = entry.getKey();
            String queryText = entry.getValue();
            if (!first) {
                sb.append('&');
            }
            sb.append(field).append("=").append(Util.URIEncode(queryText));
            first = false;
        }
        queryAsURI = sb.toString();
    }

    private boolean alt = true;

        /**
     *
     * @param in File to be matched
     * @param out to write the context
     * @param morePrefix to link to more... page
     * @param path path of the file
     * @param tags format to highlight defs.
     * @param limit should the number of matching lines be limited?
     * @return Did it get any matching context?
     */
    public boolean getContext(Reader in, Writer out, String urlPrefix,
            String morePrefix, String path, Definitions tags,
            boolean limit, List<Hit> hits) {
        alt = !alt;
        if (m == null) {
            return false;
        }
        boolean anything = false;
        TreeMap<Integer, String[]> matchingTags = null;
        if (tags != null) {
            matchingTags = new TreeMap<Integer, String[]>();
            try {
                for (Definitions.Tag tag : tags.getTags()) {
                    for (int i = 0; i < m.length; i++) {
                        if (m[i].match(tag.symbol) == LineMatcher.MATCHED) {
                            /*
                             * desc[1] is line number
                             * desc[2] is type
                             * desc[3] is  matching line;
                             */
                            String[] desc = {
                                tag.symbol,
                                Integer.toString(tag.line),
                                tag.type,
                                tag.text,};
                            if (in == null) {
                                if (out == null) {
                                    Hit hit = new Hit(path,
                                            Util.htmlize(desc[3]).replaceAll(
                                            desc[0], "<b>" + desc[0] + "</b>"),
                                            desc[1], false, alt);
                                    hits.add(hit);
                                    anything = true;
                                } else {
                                    out.write("<a class=\"s\" href=\"");
                                    out.write(urlPrefix);
                                    out.write(path);
                                    out.write("#");
                                    out.write(desc[1]);
                                    out.write("\"><span class=\"l\">");
                                    out.write(desc[1]);
                                    out.write("</span> ");
                                    out.write(Util.htmlize(desc[3]).replaceAll(
                                            desc[0], "<b>" + desc[0] + "</b>"));
                                    out.write("</a> <i> ");
                                    out.write(desc[2]);
                                    out.write(" </i><br/>");
                                }
                            } else {
                                matchingTags.put(tag.line, desc);
                            }
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                if (hits != null) {
                    // @todo verify why we ignore all exceptions?
                    OpenGrokLogger.getLogger().log(Level.WARNING, "Could not get context for " + path, e);
                }
            }
        }
        /**
         * Just to get the matching tag send a null in
         */
        if (in == null) {
            return anything;
        }
        int charsRead = 0;
        boolean truncated = false;

        boolean lim = limit;
        if (!RuntimeEnvironment.getInstance().isQuickContextScan()) {
            lim = false;
        }

        if (lim) {
            try {
                charsRead = in.read(buffer);
                if (charsRead == MAXFILEREAD) {
                    // we probably only read parts of the file, so set the
                    // truncated flag to enable the [all...] link that
                    // requests all matches
                    truncated = true;
                    // truncate to last line read (don't look more than 100
                    // characters back)
                    for (int i = charsRead - 1; i > charsRead - 100; i--) {
                        if (buffer[i] == '\n') {
                            charsRead = i;
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                OpenGrokLogger.getLogger().log(Level.WARNING, "An error occured while reading data", e);
                return anything;
            }
            if (charsRead == 0) {
                return anything;
            }

            tokens.reInit(buffer, charsRead, out, urlPrefix + path + "#", matchingTags);
        } else {
            tokens.reInit(in, out, urlPrefix + path + "#", matchingTags);
        }

        if (hits != null) {
            tokens.setAlt(alt);
            tokens.setHitList(hits);
            tokens.setFilename(path);
        }

        try {
            String token;
            int matchState = LineMatcher.NOT_MATCHED;
            int matchedLines = 0;
            while ((token = tokens.yylex()) != null && (!lim || matchedLines < 10)) {
                for (int i = 0; i < m.length; i++) {
                    matchState = m[i].match(token);
                    if (matchState == LineMatcher.MATCHED) {
                        tokens.printContext();
                        matchedLines++;
                        //out.write("<br> <i>Matched " + token + " maxlines = " + matchedLines + "</i><br>");
                        break;
                    } else if (matchState == LineMatcher.WAIT) {
                        tokens.holdOn();
                    } else {
                        tokens.neverMind();
                    }
                }
            }
            anything = matchedLines > 0;
            tokens.dumpRest();
            if (lim && (truncated || matchedLines == 10) && out != null) {
                out.write("&nbsp; &nbsp; [<a href=\"" + morePrefix + path + "?" + queryAsURI + "\">all</a>...]");
            }
        } catch (IOException e) {
            OpenGrokLogger.getLogger().log(Level.WARNING, "Could not get context for " + path, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    OpenGrokLogger.getLogger().log(Level.WARNING, "An error occured while closing stream", e);
                }
            }
            if (out != null) {
                try {
                    out.flush();
                } catch (IOException e) {
                    OpenGrokLogger.getLogger().log(Level.WARNING, "An error occured while flushing stream", e);
                }
            }
        }
        return anything;
    }
}
