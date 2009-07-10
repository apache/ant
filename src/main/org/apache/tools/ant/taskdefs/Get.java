/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;

/**
 * Gets a particular file from a URL source.
 * Options include verbose reporting, timestamp based fetches and controlling
 * actions on failures. NB: access through a firewall only works if the whole
 * Java runtime is correctly configured.
 *
 * @since Ant 1.1
 *
 * @ant.task category="network"
 */
public class Get extends Task {
    private static final int NUMBER_RETRIES = 3;
    private static final int DOTS_PER_LINE = 50;
    private static final int BIG_BUFFER_SIZE = 100 * 1024;
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final int REDIRECT_LIMIT = 25;
    
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    private URL source; // required
    private File dest; // required
    private boolean verbose = false;
    private boolean useTimestamp = false; //off by default
    private boolean ignoreErrors = false;
    private String uname = null;
    private String pword = null;
    private long maxTime = 0;

    /**
     * Does the work.
     *
     * @exception BuildException Thrown in unrecoverable error.
     */
    public void execute() throws BuildException {

        //set up logging
        int logLevel = Project.MSG_INFO;
        DownloadProgress progress = null;
        if (verbose) {
            progress = new VerboseProgress(System.out);
        }

        //execute the get
        try {
            doGet(logLevel, progress);
        } catch (IOException ioe) {
            log("Error getting " + source + " to " + dest);
            if (!ignoreErrors) {
                throw new BuildException(ioe, getLocation());
            }
        }
    }

    /**
     * make a get request, with the supplied progress and logging info.
     * All the other config parameters are set at the task level,
     * source, dest, ignoreErrors, etc.
     * @param logLevel level to log at, see {@link Project#log(String, int)}
     * @param progress progress callback; null for no-callbacks
     * @return true for a successful download, false otherwise.
     * The return value is only relevant when {@link #ignoreErrors} is true, as
     * when false all failures raise BuildExceptions.
     * @throws IOException for network trouble
     * @throws BuildException for argument errors, or other trouble when ignoreErrors
     * is false.
     */
    public boolean doGet(int logLevel, DownloadProgress progress)
            throws IOException {
        checkAttributes();

        //dont do any progress, unless asked
        if (progress == null) {
            progress = new NullProgress();
        }
        log("Getting: " + source, logLevel);
        log("To: " + dest.getAbsolutePath(), logLevel);

        //set the timestamp to the file date.
        long timestamp = 0;

        boolean hasTimestamp = false;
        if (useTimestamp && dest.exists()) {
            timestamp = dest.lastModified();
            if (verbose) {
                Date t = new Date(timestamp);
                log("local file date : " + t.toString(), logLevel);
            }
            hasTimestamp = true;
        }

        GetThread getThread = new GetThread(hasTimestamp, timestamp, progress,
                                            logLevel);
        getThread.setDaemon(true);
        getProject().registerThreadTask(getThread, this);
        getThread.start();
        try {
            getThread.join(maxTime * 1000);
        } catch (InterruptedException ie) {
            log("interrupted waiting for GET to finish",
                Project.MSG_VERBOSE);
        }

        if (getThread.isAlive()) {
            String msg = "The GET operation took longer than " + maxTime
                + " seconds, stopping it.";
            if (ignoreErrors) {
                log(msg);
            }
            getThread.closeStreams();
            if (!ignoreErrors) {
                throw new BuildException(msg);
            }
            return false;
        }

        return getThread.wasSuccessful();
    }

    /**
     * Check the attributes.
     */
    private void checkAttributes() {
        if (source == null) {
            throw new BuildException("src attribute is required", getLocation());
        }

        if (dest == null) {
            throw new BuildException("dest attribute is required", getLocation());
        }

        if (dest.exists() && dest.isDirectory()) {
            throw new BuildException("The specified destination is a directory",
                    getLocation());
        }

        if (dest.exists() && !dest.canWrite()) {
            throw new BuildException("Can't write to " + dest.getAbsolutePath(),
                    getLocation());
        }
    }

    /**
     * Set the URL to get.
     *
     * @param u URL for the file.
     */
    public void setSrc(URL u) {
        this.source = u;
    }

    /**
     * Where to copy the source file.
     *
     * @param dest Path to file.
     */
    public void setDest(File dest) {
        this.dest = dest;
    }

    /**
     * If true, show verbose progress information.
     *
     * @param v if "true" then be verbose
     */
    public void setVerbose(boolean v) {
        verbose = v;
    }

    /**
     * If true, log errors but do not treat as fatal.
     *
     * @param v if "true" then don't report download errors up to ant
     */
    public void setIgnoreErrors(boolean v) {
        ignoreErrors = v;
    }

    /**
     * If true, conditionally download a file based on the timestamp
     * of the local copy.
     *
     * <p>In this situation, the if-modified-since header is set so
     * that the file is only fetched if it is newer than the local
     * file (or there is no local file) This flag is only valid on
     * HTTP connections, it is ignored in other cases.  When the flag
     * is set, the local copy of the downloaded file will also have
     * its timestamp set to the remote file time.</p>
     *
     * <p>Note that remote files of date 1/1/1970 (GMT) are treated as
     * 'no timestamp', and web servers often serve files with a
     * timestamp in the future by replacing their timestamp with that
     * of the current time. Also, inter-computer clock differences can
     * cause no end of grief.</p>
     * @param v "true" to enable file time fetching
     */
    public void setUseTimestamp(boolean v) {
        useTimestamp = v;
    }


    /**
     * Username for basic auth.
     *
     * @param u username for authentication
     */
    public void setUsername(String u) {
        this.uname = u;
    }

    /**
     * password for the basic authentication.
     *
     * @param p password for authentication
     */
    public void setPassword(String p) {
        this.pword = p;
    }

    /**
     * Provide this for Backward Compatibility.
     */
    protected static class Base64Converter
        extends org.apache.tools.ant.util.Base64Converter {
    }

    /**
     * The time in seconds the download is allowed to take before
     * being terminated.
     *
     * @since ant 1.8.0
     */
    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    /**
     * Interface implemented for reporting
     * progess of downloading.
     */
    public interface DownloadProgress {
        /**
         * begin a download
         */
        void beginDownload();

        /**
         * tick handler
         *
         */
        void onTick();

        /**
         * end a download
         */
        void endDownload();
    }

    /**
     * do nothing with progress info
     */
    public static class NullProgress implements DownloadProgress {

        /**
         * begin a download
         */
        public void beginDownload() {

        }

        /**
         * tick handler
         *
         */
        public void onTick() {
        }

        /**
         * end a download
         */
        public void endDownload() {

        }
    }

    /**
     * verbose progress system prints to some output stream
     */
    public static class VerboseProgress implements DownloadProgress  {
        private int dots = 0;
        // CheckStyle:VisibilityModifier OFF - bc
        PrintStream out;
        // CheckStyle:VisibilityModifier ON

        /**
         * Construct a verbose progress reporter.
         * @param out the output stream.
         */
        public VerboseProgress(PrintStream out) {
            this.out = out;
        }

        /**
         * begin a download
         */
        public void beginDownload() {
            dots = 0;
        }

        /**
         * tick handler
         *
         */
        public void onTick() {
            out.print(".");
            if (dots++ > DOTS_PER_LINE) {
                out.flush();
                dots = 0;
            }
        }

        /**
         * end a download
         */
        public void endDownload() {
            out.println();
            out.flush();
        }
    }

    private class GetThread extends Thread {

        private final boolean hasTimestamp;
        private final long timestamp;
        private final DownloadProgress progress;
        private final int logLevel;

        private boolean success = false;
        private IOException ioexception = null;
        private BuildException exception = null;
        private InputStream is = null;
        private OutputStream os = null;
        private URLConnection connection;
        private int redirections = 0;

        GetThread(boolean h, long t, DownloadProgress p, int l) {
            hasTimestamp = h;
            timestamp = t;
            progress = p;
            logLevel = l;
        }

        public void run() {
            try {
                success = get();
            } catch (IOException ioex) {
                ioexception = ioex;
            } catch (BuildException bex) {
                exception = bex;
            }
        }

        private boolean get() throws IOException, BuildException {
            
            connection = openConnection(source);

            if (connection == null)
            {
                return false;
            }

            boolean downloadSucceeded = downloadFile();

            //if (and only if) the use file time option is set, then
            //the saved file now has its timestamp set to that of the
            //downloaded file
            if (downloadSucceeded && useTimestamp)  {
                updateTimeStamp();
            }
            
            return downloadSucceeded;
        }


        private boolean redirectionAllowed(URL aSource, URL aDest) {
            if (!(aSource.getProtocol().equals(aDest.getProtocol()) || (HTTP
                    .equals(aSource.getProtocol()) && HTTPS.equals(aDest
                    .getProtocol())))) {
                String message = "Redirection detected from "
                        + aSource.getProtocol() + " to " + aDest.getProtocol()
                        + ". Protocol switch unsafe, not allowed.";
                if (ignoreErrors) {
                    log(message, logLevel);
                    return false;
                } else {
                    throw new BuildException(message);
                }
            }

            redirections++;
            if (redirections > REDIRECT_LIMIT) {
                String message = "More than " + REDIRECT_LIMIT
                        + " times redirected, giving up";
                if (ignoreErrors) {
                    log(message, logLevel);
                    return false;
                } else {
                    throw new BuildException(message);
                }
            }

            
            return true;
        }

        private URLConnection openConnection(URL aSource) throws IOException {

            // set up the URL connection
            URLConnection connection = aSource.openConnection();
            // modify the headers
            // NB: things like user authentication could go in here too.
            if (hasTimestamp) {
                connection.setIfModifiedSince(timestamp);
            }
            // prepare Java 1.1 style credentials
            if (uname != null || pword != null) {
                String up = uname + ":" + pword;
                String encoding;
                // we do not use the sun impl for portability,
                // and always use our own implementation for consistent
                // testing
                Base64Converter encoder = new Base64Converter();
                encoding = encoder.encode(up.getBytes());
                connection.setRequestProperty("Authorization", "Basic "
                        + encoding);
            }

            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection)
                        .setInstanceFollowRedirects(false);
            }
            // connect to the remote site (may take some time)
            connection.connect();

            // First check on a 301 / 302 (moved) response (HTTP only)
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
              //  httpConnection.setInstanceFollowRedirects(false);
              //  httpConnection.setUseCaches(false);
                int responseCode = httpConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER)
                {
                    String newLocation = httpConnection.getHeaderField("Location");
                    String message = aSource
                            + (responseCode == HttpURLConnection.HTTP_MOVED_PERM ? " permanently"
                                    : "") + " moved to " + newLocation;
                    log(message, logLevel);
                    URL newURL = new URL(newLocation);
                    if (!redirectionAllowed(aSource, newURL))
                    {
                        return null;
                    }
                    return openConnection(newURL);
                }
                // next test for a 304 result (HTTP only)
                long lastModified = httpConnection.getLastModified();
                if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED
                        || (lastModified != 0 && hasTimestamp && timestamp >= lastModified)) {
                    // not modified so no file download. just return
                    // instead and trace out something so the user
                    // doesn't think that the download happened when it
                    // didn't
                    log("Not modified - so not downloaded", logLevel);
                    return null;
                }
                // test for 401 result (HTTP only)
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    String message = "HTTP Authorization failure";
                    if (ignoreErrors) {
                        log(message, logLevel);
                        return null;
                    } else {
                        throw new BuildException(message);
                    }
                }
            }

            //REVISIT: at this point even non HTTP connections may
            //support the if-modified-since behaviour -we just check
            //the date of the content and skip the write if it is not
            //newer. Some protocols (FTP) don't include dates, of
            //course.
            return connection;
        }

        private boolean downloadFile()
                throws FileNotFoundException, IOException {
            for (int i = 0; i < NUMBER_RETRIES; i++) {
                // this three attempt trick is to get round quirks in different
                // Java implementations. Some of them take a few goes to bind
                // property; we ignore the first couple of such failures.
                try {
                    is = connection.getInputStream();
                    break;
                } catch (IOException ex) {
                    log("Error opening connection " + ex, logLevel);
                }
            }
            if (is == null) {
                log("Can't get " + source + " to " + dest, logLevel);
                if (ignoreErrors) {
                    return false;
                }
                throw new BuildException("Can't get " + source + " to " + dest,
                        getLocation());
            }

            os = new FileOutputStream(dest);
            progress.beginDownload();
            boolean finished = false;
            try {
                byte[] buffer = new byte[BIG_BUFFER_SIZE];
                int length;
                while (!isInterrupted() && (length = is.read(buffer)) >= 0) {
                    os.write(buffer, 0, length);
                    progress.onTick();
                }
                finished = !isInterrupted();
            } finally {
                FileUtils.close(os);
                FileUtils.close(is);

                // we have started to (over)write dest, but failed.
                // Try to delete the garbage we'd otherwise leave
                // behind.
                if (!finished) {
                    dest.delete();
                }
            }
            progress.endDownload();
            return true;
        }

        private void updateTimeStamp() {
            long remoteTimestamp = connection.getLastModified();
            if (verbose)  {
                Date t = new Date(remoteTimestamp);
                log("last modified = " + t.toString()
                    + ((remoteTimestamp == 0)
                       ? " - using current time instead"
                       : ""), logLevel);
            }
            if (remoteTimestamp != 0) {
                FILE_UTILS.setFileLastModified(dest, remoteTimestamp);
            }
        }
        
        /**
         * Has the download completed successfully?
         *
         * <p>Re-throws any exception caught during executaion.</p>
         */
        boolean wasSuccessful() throws IOException, BuildException {
            if (ioexception != null) {
                throw ioexception;
            }
            if (exception != null) {
                throw exception;
            }
            return success;
        }

        /**
         * Closes streams, interrupts the download, may delete the
         * output file.
         */
        void closeStreams() {
            interrupt();
            FileUtils.close(os);
            FileUtils.close(is);
            if (!success && dest.exists()) {
                dest.delete();
            }
        }
    }
}
