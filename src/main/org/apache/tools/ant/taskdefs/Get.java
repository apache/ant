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
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.URLProvider;
import org.apache.tools.ant.types.resources.URLResource;
import org.apache.tools.ant.util.FileNameMapper;
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

    private Resources sources = new Resources();
    private File destination; // required
    private boolean verbose = false;
    private boolean useTimestamp = false; //off by default
    private boolean ignoreErrors = false;
    private String uname = null;
    private String pword = null;
    private long maxTime = 0;
    private int numberRetries = NUMBER_RETRIES;
    private boolean skipExisting = false;
    private boolean httpUseCaches = true; // on by default
    private Mapper mapperElement = null;

    /**
     * Does the work.
     *
     * @exception BuildException Thrown in unrecoverable error.
     */
    public void execute() throws BuildException {
        checkAttributes();

        for (Iterator iter = sources.iterator(); iter.hasNext(); ) {
            Resource r = (Resource) iter.next();
            URLProvider up = (URLProvider) r.as(URLProvider.class);
            URL source = up.getURL();

            File dest = destination;
            if (destination.isDirectory()) {
                if (mapperElement == null) {
                    String path = source.getPath();
                    if (path.endsWith("/")) {
                        path = path.substring(0, path.length() - 1);
                    }
                    int slash = path.lastIndexOf("/");
                    if (slash > -1) {
                        path = path.substring(slash + 1);
                    }
                    dest = new File(destination, path);
                } else {
                    FileNameMapper mapper = mapperElement.getImplementation();
                    String[] d = mapper.mapFileName(source.toString());
                    if (d == null) {
                        log("skipping " + r + " - mapper can't handle it",
                            Project.MSG_WARN);
                        continue;
                    } else if (d.length == 0) {
                        log("skipping " + r + " - mapper returns no file name",
                            Project.MSG_WARN);
                        continue;
                    } else if (d.length > 1) {
                        log("skipping " + r + " - mapper returns multiple file"
                            + " names", Project.MSG_WARN);
                        continue;
                    }
                    dest = new File(destination, d[0]);
                }
            }

        //set up logging
        int logLevel = Project.MSG_INFO;
        DownloadProgress progress = null;
        if (verbose) {
            progress = new VerboseProgress(System.out);
        }

        //execute the get
        try {
            doGet(source, dest, logLevel, progress);
        } catch (IOException ioe) {
            log("Error getting " + source + " to " + dest);
            if (!ignoreErrors) {
                throw new BuildException(ioe, getLocation());
            }
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
     * @deprecated only gets the first configured resource
     */
    public boolean doGet(int logLevel, DownloadProgress progress)
            throws IOException {
        checkAttributes();
        for (Iterator iter = sources.iterator(); iter.hasNext(); ) {
            Resource r = (Resource) iter.next();
            URLProvider up = (URLProvider) r.as(URLProvider.class);
            URL source = up.getURL();
            return doGet(source, destination, logLevel, progress);
        }
        /*NOTREACHED*/
        return false;
    }

    /**
     * make a get request, with the supplied progress and logging info.
     *
     * All the other config parameters like ignoreErrors are set at
     * the task level.
     * @param source the URL to get
     * @param dest the target file
     * @param logLevel level to log at, see {@link Project#log(String, int)}
     * @param progress progress callback; null for no-callbacks
     * @return true for a successful download, false otherwise.
     * The return value is only relevant when {@link #ignoreErrors} is true, as
     * when false all failures raise BuildExceptions.
     * @throws IOException for network trouble
     * @throws BuildException for argument errors, or other trouble when ignoreErrors
     * is false.
     * @since Ant 1.8.0
     */
    public boolean doGet(URL source, File dest, int logLevel,
                         DownloadProgress progress)
        throws IOException {

        if (dest.exists() && skipExisting) {
            log("Destination already exists (skipping): "
                + dest.getAbsolutePath(), logLevel);
            return true;
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

        GetThread getThread = new GetThread(source, dest,
                                            hasTimestamp, timestamp, progress,
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
        if (sources.size() == 0) {
            throw new BuildException("at least one source is required",
                                     getLocation());
        }
        for (Iterator iter = sources.iterator(); iter.hasNext(); ) {
            Object up = ((Resource) iter.next()).as(URLProvider.class);
            if (up == null) {
                throw new BuildException("Only URLProvider resources are"
                                         + " supported", getLocation());
            }
        }

        if (destination == null) {
            throw new BuildException("dest attribute is required", getLocation());
        }

        if (destination.exists() && sources.size() > 1
            && !destination.isDirectory()) {
            throw new BuildException("The specified destination is not a"
                                     + " directory",
                                     getLocation());
        }

        if (destination.exists() && !destination.canWrite()) {
            throw new BuildException("Can't write to "
                                     + destination.getAbsolutePath(),
                                     getLocation());
        }

        if (sources.size() > 1 && !destination.exists()) {
            destination.mkdirs();
        }
    }

    /**
     * Set an URL to get.
     *
     * @param u URL for the file.
     */
    public void setSrc(URL u) {
        add(new URLResource(u));
    }

    /**
     * Adds URLs to get.
     * @since Ant 1.8.0
     */
    public void add(ResourceCollection rc) {
        sources.add(rc);
    }

    /**
     * Where to copy the source file.
     *
     * @param dest Path to file.
     */
    public void setDest(File dest) {
        this.destination = dest;
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
     * The time in seconds the download is allowed to take before
     * being terminated.
     *
     * @since Ant 1.8.0
     */
    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    /**
     * The number of retries to attempt upon error, defaults to 3.
     *
     * @param r retry count
     *
     * @since Ant 1.8.0
     */
    public void setRetries(int r) {
        this.numberRetries = r;
    }

    /**
     * Skip files that already exist locally.
     *
     * @param s "true" to skip existing destination files
     *
     * @since Ant 1.8.0
     */
    public void setSkipExisting(boolean s) {
        this.skipExisting = s;
    }

    /**
     * HTTP connections only - control caching on the
     * HttpUrlConnection: httpConnection.setUseCaches(); if false, do
     * not allow caching on the HttpUrlConnection.
     *
     * <p>Defaults to true (allow caching, which is also the
     * HttpUrlConnection default value.</p>
     *
     * @since Ant 1.8.0
     */
    public void setHttpUseCaches(boolean httpUseCache) {
        this.httpUseCaches = httpUseCache;
    }

    /**
     * Define the mapper to map source to destination files.
     * @return a mapper to be configured.
     * @exception BuildException if more than one mapper is defined.
     * @since Ant 1.8.0
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper",
                                     getLocation());
        }
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }

    /**
     * Add a nested filenamemapper.
     * @param fileNameMapper the mapper to add.
     * @since Ant 1.8.0
     */
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
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

        private final URL source;
        private final File dest;
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

        GetThread(URL source, File dest,
                  boolean h, long t, DownloadProgress p, int l) {
            this.source = source;
            this.dest = dest;
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
                ((HttpURLConnection) connection)
                        .setUseCaches(httpUseCaches);
            }
            // connect to the remote site (may take some time)
            try {
                connection.connect();
            } catch (NullPointerException e) {
                //bad URLs can trigger NPEs in some JVMs
                throw new BuildException("Failed to parse " + source.toString(), e);
            }

            // First check on a 301 / 302 (moved) response (HTTP only)
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
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
                    URL newURL = new URL(aSource, newLocation);
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
            for (int i = 0; i < numberRetries; i++) {
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
