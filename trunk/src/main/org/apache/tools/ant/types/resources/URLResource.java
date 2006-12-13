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

package org.apache.tools.ant.types.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.JarURLConnection;
import java.util.jar.JarFile;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileUtils;

/**
 * Exposes a URL as a Resource.
 * @since Ant 1.7
 */
public class URLResource extends Resource {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final int NULL_URL
        = Resource.getMagicNumber("null URL".getBytes());

    private URL url;
    private URLConnection conn;

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
     * Get the URL used by this URLResource.
     * @return a URL object.
     */
    public synchronized URL getURL() {
        if (isReference()) {
            return ((URLResource) getCheckedRef()).getURL();
        }
        return url;
     }

    /**
     * Overrides the super version.
     * @param r the Reference to set.
     */
    public synchronized void setRefid(Reference r) {
        //not using the accessor in this case to avoid side effects
        if (url != null) {
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
        return isReference() ? ((Resource) getCheckedRef()).getName()
            : getURL().getFile().substring(1);
    }

    /**
     * Return this URLResource formatted as a String.
     * @return a String representation of this URLResource.
     */
    public synchronized String toString() {
        return isReference()
            ? getCheckedRef().toString() : String.valueOf(getURL());
    }

    /**
     * Find out whether the URL exists .
     * @return true if this resource exists.
     */
    public synchronized boolean isExists() {
        if (isReference()) {
            return ((Resource) getCheckedRef()).isExists();
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
            connect();
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
            return ((Resource) getCheckedRef()).getLastModified();
        }
        if (!isExists(false)) {
            return 0L;
        }
        return conn.getLastModified();
    }

    /**
     * Tells if the resource is a directory.
     * @return boolean whether the resource is a directory.
     */
    public synchronized boolean isDirectory() {
        return isReference()
            ? ((Resource) getCheckedRef()).isDirectory()
            : getName().endsWith("/");
    }

    /**
     * Get the size of this Resource.
     * @return the size, as a long, 0 if the Resource does not exist (for
     *         compatibility with java.io.File), or UNKNOWN_SIZE if not known.
     */
    public synchronized long getSize() {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getSize();
        }
        if (!isExists(false)) {
            return 0L;
        }
        try {
            connect();
            long contentlength = conn.getContentLength();
            close();
            return contentlength;
        } catch (IOException e) {
            return UNKNOWN_SIZE;
        }
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
            return getCheckedRef().equals(another);
        }
        if (!(another.getClass().equals(getClass()))) {
            return false;
        }
        URLResource otheru = (URLResource) another;
        return getURL() == null
            ? otheru.getURL() == null
            : getURL().equals(otheru.getURL());
    }

    /**
     * Get the hash code for this Resource.
     * @return hash code as int.
     */
    public synchronized int hashCode() {
        if (isReference()) {
            return getCheckedRef().hashCode();
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
            return ((Resource) getCheckedRef()).getInputStream();
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
            return ((Resource) getCheckedRef()).getOutputStream();
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
    protected synchronized void connect() throws IOException {
        URL u = getURL();
        if (u == null) {
            throw new BuildException("URL not set");
        }
        if (conn == null) {
            try {
                conn = u.openConnection();
                conn.connect();
            } catch (IOException e) {
                log(e.toString(), Project.MSG_ERR);
                conn = null;
                throw e;
            }
        }
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
        if (conn != null) {
            try {
                if (conn instanceof JarURLConnection) {
                    JarURLConnection juc = (JarURLConnection) conn;
                    JarFile jf = juc.getJarFile();
                    jf.close();
                    jf = null;
                } else if (conn instanceof HttpURLConnection) {
                    ((HttpURLConnection) conn).disconnect();
                }
            } catch (IOException exc) {
                //ignore
            } finally {
                conn = null;
            }
        }
    }

    private static URL newURL(String u) {
        try {
            return new URL(u);
        } catch (MalformedURLException e) {
            throw new BuildException(e);
        }
    }

}
