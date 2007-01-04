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
 * Copyright 2006 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */
package org.opensolaris.opengrok.history;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;

/**
 *
 * @author Trond Norbye
 */
public class MercurialRepository implements ExternalRepository {
    private File directory;
    private String directoryName;
    private String command;
    private boolean verbose;
    private boolean daemon;
    private boolean useCache;
    
    /**
     * Creates a new instance of MercurialRepository
     */
    public MercurialRepository(String directory) {
        this.directory = new File(directory);
        directoryName = this.directory.getAbsolutePath();
        command = System.getProperty("org.opensolaris.opengrok.history.Mercurial", "hg");
        daemon = false;
        useCache = RuntimeEnvironment.getInstance().useHistoryCache();
        Socket sock = null;
        try {
            sock = new Socket("localhost", 4242);
            OutputStream out = sock.getOutputStream();
            InputStream in = sock.getInputStream();
            out.write(("verify " + directory + "\n").getBytes());
            out.flush();
            int ret = in.read();
            
            if (ret == '+') {
                System.out.println("Using daemon");
                daemon = true;
            } else {
                System.out.println("Not using daemon... " + ret);
            }
        } catch (IOException ex) {
            if (ex instanceof ConnectException) {
                System.err.println("No Mercurial cache daemon available at localhost:4242");
            } else {
                System.err.println("Failed to access Mercurial cache daemon");
                ex.printStackTrace(System.err);
            }
        } finally {
            try {
                if (sock != null) {
                    sock.close();
                }
            } catch (Exception e) {
                ;
            }
        }
        if (daemon) {
            System.out.println("Using Mercurial cache daemon");
        }
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public String getCommand() {
        return command;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
        directoryName = directory.getAbsolutePath();
    }
    
    public File getDirectory() {
        return directory;
    }
    
    public boolean isVerbose() {
        return verbose;
    }
    
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    InputStream getHistoryStream(File file) {
        InputStream ret = null;
        String filename = file.getAbsolutePath().substring(directoryName.length() + 1);
        
        if (daemon) {
            // Try to use daemon
            Socket socket = null;
            
            try {
                StringBuilder command = new StringBuilder();
                command.append("log ");
                command.append(filename);
                command.append(" ");
                command.append(directoryName);
                command.append("\n");
                
                socket = new Socket("localhost", 4242);
                socket.getOutputStream().write(command.toString().getBytes());
                socket.getOutputStream().flush();
                
                InputStream in = socket.getInputStream();
                byte[] buffer = new byte[8192];
                if (in.read() == '+' && in.read() != -1) {
                    int size;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    while ((size = in.read(buffer)) != -1) {
                        out.write(buffer, 0, size);                        
                    }
                    
                    ret = new ByteArrayInputStream(out.toByteArray());
                }
            } catch (Exception ex) {
                System.err.println("Failed to use Mercurial daemon: ");
                ex.printStackTrace(System.err);
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception e) {
                    ;
                }
            }
        }
        
        if (ret == null) {
            String argv[];
            if (verbose) {
                argv = new String[] { command, "log", "-v", filename };
            } else {
                argv = new String[] { command, "log", filename };
            }
            try {
                Process process =
                        Runtime.getRuntime().exec(argv, null, directory);
                ret = process.getInputStream();
                process.waitFor();
            } catch (Exception ex) {
                System.err.println("An error occured while executing hg log:");
                ex.printStackTrace(System.err);
                ret = null;
            }
        }
        
        return ret;
    }
    
    public InputStream getHistoryGet(String parent, String basename, String rev) {
        MercurialGet ret = null;
        String filename =  (new File(parent, basename)).getAbsolutePath().substring(directoryName.length() + 1);
        if (daemon) {
            // Try to use daemon
            Socket socket = null;
            
            try {
                StringBuilder command = new StringBuilder();
                command.append("get ");
                command.append(rev);
                command.append(" ");
                command.append(filename);
                command.append(" ");
                command.append(directoryName);
                command.append("\n");
                
                socket = new Socket("localhost", 4242);
                socket.getOutputStream().write(command.toString().getBytes());
                socket.getOutputStream().flush();
                
                InputStream in = socket.getInputStream();
                byte[] buffer = new byte[8192];
                if (in.read() == '+' && in.read() != -1) {
                    int size;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    while ((size = in.read(buffer)) != -1) {
                        out.write(buffer, 0, size);                        
                    }
                    
                    ret = new MercurialGet(new ByteArrayInputStream(out.toByteArray()));
                }
                
            } catch (Exception ex) {
                System.err.println("Failed to use Mercurial daemon: ");
                ex.printStackTrace(System.err);
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception e) {
                    ;
                }
            }
        }
        
        if (ret == null) {
            // Use external process!!!
            try {
                String argv[] = { command, "cat", "-r", rev, filename };
                Process process = Runtime.getRuntime().exec(argv, null, directory);
                
                ret = new MercurialGet(process.getInputStream());
                process.waitFor();
            } catch (Exception exp) {
                System.err.print("Failed to get history: " + exp.getClass().toString());
                exp.printStackTrace();
            }
        }
        
        return ret;
    }
    
    public Class<? extends HistoryParser> getHistoryParser() {
        return MercurialHistoryParser.class;
    }
}
