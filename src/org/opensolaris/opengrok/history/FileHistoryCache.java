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

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.opensolaris.opengrok.OpenGrokLogger;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;

class FileHistoryCache implements HistoryCache {
    private final Object lock = new Object();

    static class FilePersistenceDelegate extends PersistenceDelegate {
        protected Expression instantiate(Object oldInstance, Encoder out) {
            File f = (File)oldInstance;
            return new Expression(oldInstance, f.getClass(), "new", new Object[] {f.toString()});
        }
    }
            
    /**
     * Get a <code>File</code> object describing the cache file.
     *
     * @param file the file to find the cache for
     * @return file that might contain cached history for <code>file</code>
     */
    private static File getCachedFile(File file) {
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        
        StringBuilder sb = new StringBuilder();
        sb.append(env.getDataRootPath());
        sb.append(File.separatorChar);
        sb.append("historycache");
        
        String sourceRoot = env.getSourceRootPath();
        if (sourceRoot == null) {
            return null;
        }

        try {
            String add = file.getCanonicalPath().substring(sourceRoot.length());
            if (add.length() == 0) {
                add = File.separator;
            }
            sb.append(add);
        } catch (IOException ex) {
            OpenGrokLogger.getLogger().log(Level.INFO, "Could not get path for: " + file, ex);
        }
        sb.append(".gz");
        
        return new File(sb.toString());
    }
        
    /**
     * Read history from a file.
     */
    private static History readCache(File file) throws IOException {
        XMLDecoder d = new XMLDecoder(
                new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))));
        Object obj = d.readObject();
        d.close();
        return (History) obj;
    }
    
    public void store(History history, File file) throws IOException {
        
        File cache = getCachedFile(file);

        File dir = cache.getParentFile();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Unable to create cache directory '" + dir + "'.");
        }

        // We have a problem that multiple threads may access the cache layer
        // at the same time. Since I would like to avoid read-locking, I just
        // serialize the write access to the cache file. The generation of the
        // cache file would most likely be executed during index generation, and
        // that happens sequencial anyway....
        // Generate the file with a temporary name and move it into place when
        // I'm done so I don't have to protect the readers for partially updated
        // files...
        File output = File.createTempFile("oghist", null, dir);
        XMLEncoder e = new XMLEncoder(
                new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(output))));
        e.setPersistenceDelegate(File.class, new FilePersistenceDelegate());
        e.writeObject(history);
        e.close();
        synchronized (lock) {
            if (!cache.delete() && cache.exists()) {
                if (!output.delete()) {
                    OpenGrokLogger.getLogger().log(Level.WARNING, "Failed to remove temporary history cache file");
                }
                throw new IOException(
                        "Cachefile exists, and I could not delete it.");
            }
            if (!output.renameTo(cache)) {
                if (!output.delete()) {
                    OpenGrokLogger.getLogger().log(Level.WARNING, "Failed to remove temporary history cache file");
                }
                throw new IOException("Failed to rename cache tmpfile.");
            }
        }
    }

    public History get(File file, Repository repository) throws IOException {
        Class<? extends HistoryParser> parserClass;
        parserClass = repository.getHistoryParser();
        File cache = getCachedFile(file);
        boolean hasCache = (cache != null) && cache.exists();
        if (hasCache && file.lastModified() < cache.lastModified()) {
            try {
                return readCache(cache);
            } catch (Exception e) {
                OpenGrokLogger.getLogger().log(Level.WARNING, 
                        "Error when reading cache file '" + cache, e);
            }
        }
        
        History history = null;
        long time;
        try {
            HistoryParser parser = parserClass.newInstance();
            time = System.currentTimeMillis();
            history = parser.parse(file, repository);
            time = System.currentTimeMillis() - time;
        } catch (InstantiationException ex) {
            throw new IOException("Could not create history parser "+ ex.getMessage());
        } catch (IllegalAccessException ex) {
            throw new IOException("No access permissions to create history parser "+ ex.getMessage());
        } catch (UnsupportedOperationException e) {
            // In this case, we've found a file for which the SCM has no history
            // An example is a non-SCCS file somewhere in an SCCS-controlled
            // workspace.
            return null;
        }

        if (repository.isCacheable() && !file.isDirectory()) {
            // Don't cache history-information for directories, since the 
            // history information on the directory may change if a file in
            // a sub-directory change. This will cause us to present a stale
            // history log until a the current directory is updated and 
            // invalidates the cache entry.           
            RuntimeEnvironment env = RuntimeEnvironment.getInstance();
            if (env.useHistoryCache() && (cache != null) &&
                        (cache.exists() ||
                             (time > env.getHistoryReaderTimeLimit()))) {
                    // retrieving the history takes too long, cache it!
                try {
                    store(history, file);
                } catch (IOException e) {
                    OpenGrokLogger.getLogger().log(Level.WARNING, 
                            "Error when writing cache file '" + cache + "':", e);
                }
            }
        }        
        return history;
    }
}
