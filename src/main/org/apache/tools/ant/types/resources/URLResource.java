/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.types.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.util.FileUtils;

/**
 * Exposes a URL as a Resource.
 * @since Ant 1.7
 */
public class URLResource extends Resource implements URLProvider {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final int NULL_URL
        = Resource.getMagicNumber("null URL".getBytes());

    private URL url;
    private URLConnection conn;
    private URL baseURL;
    private String relPath;

    /**
     * Default constructor.
     */
    public URLResource() {
    }

    /**
     * Convenience constructor.
     * @param u the URL to expose.
     */
    public URLResource(URL u) {
        setURL(u);
    }

    /**
     * Convenience constructor.
     * @param u holds the URL to expose.
     */
    public URLResource(URLProvider u) {
        setURL(u.getURL());
    }

    /**
     * Convenience constructor.
     * @param f the File to set as a URL.
     */
    public URLResource(File f) {
        setFile(f);
    }

    /**
     * String constructor for Ant attribute introspection.
     * @param u String representation of this URL.
     * @see org.apache.tools.ant.IntrospectionHelper
     */
    public URLResource(String u) {
        this(newURL(u));
    }

    /**
     * Set the URL for this URLResource.
     * @param u the URL to expose.
     */
    public synchronized void setURL(URL u) {
        checkAttributesAllowed();
        url = u;
    }

    /**
     * Set the URL from a File.
     * @param f the File to set as a URL.
     */
    public synchronized void setFile(File f) {
        try {
            setURL(FILE_UTILS.getFileURL(f));
        } catch (MalformedURLException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Base URL which combined with the relativePath attribute defines
     * the URL.
     * @param base URL
     * @since Ant 1.8.0
     */
    public synchronized void setBaseURL(URL base) {
        checkAttributesAllowed();
        if (url != null) {
            throw new BuildException("can't define URL and baseURL attribute");
        }
        baseURL = base;
    }

    /**
     * Relative path which combined with the baseURL attribute defines
     * the URL.
     * @param r String
     * @since Ant 1.8.0
     */
    public synchronized void setRelativePath(String r) {
        checkAttributesAllowed();
        if (url != null) {
            throw new BuildException("can't define URL and relativePath"
                                     + " attribute");
        }
        relPath = r;
    }


    /**
     * Get the URL used by this URLResource.
     * @return a URL object.
     */
    public synchronized URL getURL() {
        if (isReference()) {
            return getRef().getURL();
        }
        if (url == null) {
            if (baseURL != null) {
                if (relPath == null) {
                    throw new BuildException("must provide relativePath"
                                             + " attribute when using baseURL.");
                }
                try {
                    url = new URL(baseURL, relPath);
                } catch (MalformedURLException e) {
                    throw new BuildException(e);
                }
            }
        }
        return url;
     }

    /**
     * Overrides the super version.
     * @param r the Reference to set.
     */
    public synchronized void setRefid(Reference r) {
        //not using the accessor in this case to avoid side effects
        if (url != null || baseURL != null || relPath != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Get the name of this URLResource
     * (its file component minus the leading separator).
     * @return the name of this resource.
     */
    public synchronized String getName() {
        if (isReference()) {
            return getRef().getName();
        }
        String name = getURL().getFile();
        return name.isEmpty() ? name : name.substring(1);
    }

    /**
     * Return this URLResource formatted as a String.
     * @return a String representation of this URLResource.
     */
    public synchronized String toString() {
        return isReference()
            ? getRef().toString() : String.valueOf(getURL());
    }

    /**
     * Find out whether the URL exists .
     * @return true if this resource exists.
     */
    public synchronized boolean isExists() {
        if (isReference()) {
            return getRef().isExists();
        }
        return isExists(false);
    }

    /**
     * Find out whether the URL exists, and close the connection
     * opened to the URL if closeConnection is true.
     *
     * Note that this method does ensure that if:
     * - the resource exists (if it returns true)
     * - and if the current object is not a reference
     * (isReference() returns false)
     * - and if it was called with closeConnection to false,
     *
     * then the connection to the URL (stored in the conn
     * private field) will be opened, and require to be closed
     * by the caller.
     *
     * @param closeConnection true if the connection should be closed
     * after the call, false if it should stay open.
     * @return true if this resource exists.
     */
    private synchronized boolean isExists(boolean closeConnection) {
        if (getURL() == null) {
            return false;
        }
        try {
            connect(Project.MSG_VERBOSE);
            if (conn instanceof HttpURLConnection) {
                int sc = ((HttpURLConnection) conn).getResponseCode();
                // treating inaccessible resources as non-existent
                return sc < 400;
            } else if (url.getProtocol().startsWith("ftp")) {
                closeConnection = true;
                InputStream in = null;
                try {
                    in = conn.getInputStream();
                } finally {
                    FileUtils.close(in);
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (closeConnection) {
                close();
            }
        }
    }


    /**
     * Tells the modification time in milliseconds since 01.01.1970 .
     *
     * @return 0 if the resource does not exist to mirror the behavior
     * of {@link java.io.File File}.
     */
    public synchronized long getLastModified() {
        if (isReference()) {
            return getRef().getLastModified();
        }
        if (!isExists(false)) {
            return UNKNOWN_DATETIME;
        }
        return withConnection(c -> conn.getLastModified(), UNKNOWN_DATETIME);
    }

    /**
     * Tells if the resource is a directory.
     * @return boolean whether the resource is a directory.
     */
    public synchronized boolean isDirectory() {
        return isReference()
            ? getRef().isDirectory()
            : getName().endsWith("/");
    }

    /**
     * Get the size of this Resource.
     * @return the size, as a long, 0 if the Resource does not exist (for
     *         compatibility with java.io.File), or UNKNOWN_SIZE if not known.
     */
    public synchronized long getSize() {
        if (isReference()) {
            return getRef().getSize();
        }
        if (!isExists(false)) {
            return 0L;
        }
        return withConnection(c -> conn.getContentLength(), UNKNOWN_SIZE);
    }

    /**
     * Test whether an Object equals this URLResource.
     * @param another the other Object to compare.
     * @return true if the specified Object is equal to this Resource.
     */
    public synchronized boolean equals(Object another) {
        if (this == another) {
            return true;
        }
        if (isReference()) {
            return getRef().equals(another);
        }
        if (another == null || another.getClass() != getClass()) {
            return false;
        }
        URLResource other = (URLResource) another;
        return getURL() == null
            ? other.getURL() == null
            : getURL().equals(other.getURL());
    }

    /**
     * Get the hash code for this Resource.
     * @return hash code as int.
     */
    public synchronized int hashCode() {
        if (isReference()) {
            return getRef().hashCode();
        }
        return MAGIC * ((getURL() == null) ? NULL_URL : getURL().hashCode());
    }

    /**
     * Get an InputStream for the Resource.
     * @return an InputStream containing this Resource's content.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if InputStreams are not
     *         supported for this Resource type.
     */
    public synchronized InputStream getInputStream() throws IOException {
        if (isReference()) {
            return getRef().getInputStream();
        }
        connect();
        try {
            return conn.getInputStream();
        } finally {
            conn = null;
        }
    }

    /**
     * Get an OutputStream for the Resource.
     * @return an OutputStream to which content can be written.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if OutputStreams are not
     *         supported for this Resource type.
     * @throws IOException if the URL cannot be opened.
     */
    public synchronized OutputStream getOutputStream() throws IOException {
        if (isReference()) {
            return getRef().getOutputStream();
        }
        connect();
        try {
            return conn.getOutputStream();
        } finally {
            conn = null;
        }
    }

    /**
     * Ensure that we have a connection.
     * @throws IOException if the connection cannot be established.
     */
    protected void connect() throws IOException {
        connect(Project.MSG_ERR);
    }

    /**
     * Ensure that we have a connection.
     * @param logLevel severity to use when logging connection errors.
     * Should be one of the <code>MSG_</code> constants in {@link
     * Project Project}.
     * @throws IOException if the connection cannot be established.
     * @since Ant 1.8.2
     */
    protected synchronized void connect(int logLevel) throws IOException {
        URL u = getURL();
        if (u == null) {
            throw new BuildException("URL not set");
        }
        if (conn == null) {
            try {
                conn = u.openConnection();
                conn.connect();
            } catch (IOException e) {
                log(e.toString(), logLevel);
                conn = null;
                throw e;
            }
        }
    }

    @Override
    protected URLResource getRef() {
        return getCheckedRef(URLResource.class);
    }

    /**
     * Closes the URL connection if:
     * - it is opened (i.e. the field conn is not null)
     * - this type of URLConnection supports some sort of close mechanism
     *
     * This method ensures the field conn will be null after the call.
     *
     */
    private synchronized void close() {
        try {
            FileUtils.close(conn);
        } finally {
            conn = null;
        }
    }

    private static URL newURL(String u) {
        try {
            return new URL(u);
        } catch (MalformedURLException e) {
            throw new BuildException(e);
        }
    }

    private interface ConnectionUser {
        long useConnection(URLConnection c);
    }

    private long withConnection(ConnectionUser u, long defaultValue) {
        try {
            if (conn != null) {
                return u.useConnection(conn);
            } else {
                try {
                    connect();
                    return u.useConnection(conn);
                } finally {
                    close();
                }
            }
        } catch (IOException ex) {
            return defaultValue;
        }
    }
}
