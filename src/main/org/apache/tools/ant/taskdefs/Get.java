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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

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

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private URL source; // required
    private File dest; // required
    private boolean verbose = false;
    private boolean useTimestamp = false; //off by default
    private boolean ignoreErrors = false;
    private String uname = null;
    private String pword = null;



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

        //set up the URL connection
        URLConnection connection = source.openConnection();
        //modify the headers
        //NB: things like user authentication could go in here too.
        if (hasTimestamp) {
            connection.setIfModifiedSince(timestamp);
        }
        // prepare Java 1.1 style credentials
        if (uname != null || pword != null) {
            String up = uname + ":" + pword;
            String encoding;
            //we do not use the sun impl for portability,
            //and always use our own implementation for consistent
            //testing
            Base64Converter encoder = new Base64Converter();
            encoding = encoder.encode(up.getBytes());
            connection.setRequestProperty ("Authorization",
                    "Basic " + encoding);
        }

        //connect to the remote site (may take some time)
        connection.connect();
        //next test for a 304 result (HTTP only)
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection
                    = (HttpURLConnection) connection;
            long lastModified = httpConnection.getLastModified();
            if (httpConnection.getResponseCode()
                    == HttpURLConnection.HTTP_NOT_MODIFIED
                || (lastModified != 0 && hasTimestamp
                && timestamp >= lastModified)) {
                //not modified so no file download. just return
                //instead and trace out something so the user
                //doesn't think that the download happened when it
                //didn't
                log("Not modified - so not downloaded", logLevel);
                return false;
            }
            // test for 401 result (HTTP only)
            if (httpConnection.getResponseCode()
                    == HttpURLConnection.HTTP_UNAUTHORIZED)  {
                String message = "HTTP Authorization failure";
                if (ignoreErrors) {
                    log(message, logLevel);
                    return false;
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

        InputStream is = null;
        for (int i = 0; i < 3; i++) {
            //this three attempt trick is to get round quirks in different
            //Java implementations. Some of them take a few goes to bind
            //property; we ignore the first couple of such failures.
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

        FileOutputStream fos = new FileOutputStream(dest);
        progress.beginDownload();
        boolean finished = false;
        try {
            byte[] buffer = new byte[100 * 1024];
            int length;
            while ((length = is.read(buffer)) >= 0) {
                fos.write(buffer, 0, length);
                progress.onTick();
            }
            finished = true;
        } finally {
            FileUtils.close(fos);
            FileUtils.close(is);

            // we have started to (over)write dest, but failed.
            // Try to delete the garbage we'd otherwise leave
            // behind.
            if (!finished) {
                dest.delete();
            }
        }
        progress.endDownload();

        //if (and only if) the use file time option is set, then
        //the saved file now has its timestamp set to that of the
        //downloaded file
        if (useTimestamp)  {
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

        //successful download
        return true;
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
            if (dots++ > 50) {
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

}
