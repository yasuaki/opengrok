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

package org.opensolaris.opengrok.analysis;

import java.io.InputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opensolaris.opengrok.analysis.FileAnalyzer.Genre;
import org.opensolaris.opengrok.history.Annotation;

/**
 * Factory class which creates a {@code FileAnalyzer} object and
 * provides information about this type of analyzers.
 */
public class FileAnalyzerFactory {
    /** Cached analyzer object for the current thread (analyzer objects can be
     * expensive to allocate). */
    private final ThreadLocal<FileAnalyzer> cachedAnalyzer;

    /** List of file extensions on which this kind of analyzer should be
     * used. */
    private final List<String> suffixes;

    /** List of magic strings used to recognize files on which this kind of
     * analyzer should be used. */
    private final List<String> magics;

    /** List of matchers which delegate files to different types of
     * analyzers. */
    private final List<Matcher> matchers;

    /** The content type for the files recognized by this kind of analyzer. */
    private final String contentType;

    /** The genre for files recognized by this kind of analyzer. */
    private final Genre genre;

    /**
     * Create an instance of {@code FileAnalyzerFactory}.
     */
    FileAnalyzerFactory() {
        this(null, null, null, null, null);
    }

    /**
     * Construct an instance of {@code FileAnalyzerFactory}. This constructor
     * should be used by subclasses to override default values.
     *
     * @param suffixes list of suffixes to recognize (possibly {@code null})
     * @param magics list of magic strings to recognize (possibly {@code null})
     * @param matcher a matcher for this analyzer (possibly {@code null})
     * @param contentType content type for this analyzer (possibly {@code null})
     * @param genre the genre for this analyzer (if {@code null}, {@code
     * Genre.DATA} is used)
     */
    protected FileAnalyzerFactory(String[] suffixes, String[] magics,
                                  Matcher matcher, String contentType,
                                  Genre genre) {
        cachedAnalyzer = new ThreadLocal<FileAnalyzer>();
        this.suffixes = asList(suffixes);
        this.magics = asList(magics);
        if (matcher == null) {
            this.matchers = Collections.emptyList();
        } else {
            this.matchers = Collections.singletonList(matcher);
        }
        this.contentType = contentType;
        this.genre = (genre != null) ? genre : Genre.DATA;
    }

    /**
     * Helper method which wraps an array in a list.
     *
     * @param a the array to wrap ({@code null} means an empty array)
     * @return a list which wraps the array
     */
    private static <T> List<T> asList(T[] a) {
        if (a == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(a));
    }

    /**
     * Get the list of file extensions recognized by this analyzer.
     * @return list of suffixes
     */
    public final List<String> getSuffixes() {
        return suffixes;
    }

    /**
     * Get the list of magic strings recognized by this analyzer. If a file
     * starts with one of these strings, an analyzer created by this factory
     * should be used to analyze it.
     *
     * <p><b>Note:</b> Currently this assumes that the file is encoded with
     * ISO-8859-1.
     *
     * @return list of magic strings
     */
    public final List<String> getMagicStrings() {
        return magics;
    }

    /**
     * Get matchers that map file contents to analyzer factories
     * programmatically.
     *
     * @return list of matchers
     */
    public final List<Matcher> getMatchers() {
        return matchers;
    }

    /**
     * Get the content type (MIME type) for analyzers returned by this factory.
     * @return content type (could be {@code null} if it is unknown)
     */
    public final String getContentType() {
        return contentType;
    }

    /**
     * The genre this analyzer factory belongs to.
     * @return a genre
     */
    public final Genre getGenre() {
        return genre;
    }

    /**
     * Get an analyzer. If the same thread calls this method multiple times on
     * the same factory object, the exact same analyzer object will be returned
     * each time. Subclasses should not override this method, but instead
     * override the {@code newAnalyzer()} method.
     *
     * @return a {@code FileAnalyzer} instance
     * @see #newAnalyzer()
     */
    public final FileAnalyzer getAnalyzer() {
        FileAnalyzer fa = cachedAnalyzer.get();
        if (fa == null) {
            fa = newAnalyzer();
            cachedAnalyzer.set(fa);
        }
        return fa;
    }

    /**
     * Create a new analyzer.
     * @return an analyzer
     */
    protected FileAnalyzer newAnalyzer() {
        return new FileAnalyzer(this);
    }

    /**
     * Interface for matchers which map file contents to analyzer factories.
     */
    protected interface Matcher {
        /**
         * Try to match the file contents with an analyzer factory.
         * @param contents the first few bytes of a file
         * @return an analyzer factory if the contents match, or {@code null}
         * if they don't match any factory known by this matcher
         */
        FileAnalyzerFactory isMagic(byte[] contents);
    }

    /**
     * Write a cross referenced HTML file. Reads the source from {@code in}.
     * @param in input source
     * @param out output xref writer
     * @param annotation annotation for the file (could be {@code null})
     */
    public void writeXref(InputStream in, Writer out, Annotation annotation)
        throws IOException
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
