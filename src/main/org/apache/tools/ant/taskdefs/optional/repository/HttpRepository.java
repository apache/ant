/*
 * Copyright  2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.optional.repository;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Get;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This is a base class for repositories that are built on URLs. Although you
 * can share this datatype, it is *not* thread safe; you can only use it in one
 * thread at at time
 *
 * @since Ant1.7
 */
public abstract class HttpRepository extends Repository {
    /**
     * repositoryURL of repository
     */
    private String url;

    /**
     * username
     */
    private String username;

    /**
     * password
     */
    private String password;

    /**
     * auth realm; can be null
     */
//    private String realm;

    /**
     * no repository URL
     */
    public static final String ERROR_NO_REPOSITORY_URL = "No repository URL";

    /**
     * owner class
     */
    private GetLibraries owner;

    /**
     * retry logic
     */
    public static final String ERROR_REENTRANT_USE = "Repository is already in use";
    private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    private static final int BLOCKSIZE = 8192;

    /**
     * get the base URL of the repository
     *
     * @return
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the base URL of the repository
     *
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    /**
     * set the username for the remote repository
     *
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * set the password for the remote repository
     *
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }
/*

    public String getRealm() {
        return realm;
    }
*/

    /**
     * set the realm for authentication; empty string is equivalent to "any
     * realm" (the default)
     *
     * @param realm
     */
/*    public void setRealm(String realm) {
        if (realm != null) {
            this.realm = realm;
        } else {
            this.realm = null;
        }
    }*/

    public GetLibraries getOwner() {
        return owner;
    }

    /**
     * validate yourself
     *
     * @throws org.apache.tools.ant.BuildException
     *          if unhappy
     */
    public void validate() {
        super.validate();
        checkChildrenAllowed();
        checkAttributesAllowed();
        if (url == null || url.length() == 0) {
            throw new BuildException(ERROR_NO_REPOSITORY_URL);
        }
    }

    /**
     * override point: connection is called at the start of the retrieval
     * process
     *
     * @param newOwner
     *
     * @throws org.apache.tools.ant.BuildException
     *
     */
    public void connect(GetLibraries newOwner) {
        this.owner = newOwner;
        if (!url.endsWith("/")) {
            url = url + '/';
        }

        //validate the URL
        URL repository;
        try {
            repository = new URL(url);
        } catch (MalformedURLException e) {
            throw new BuildException(e);
        }
    }

    /**
     * override point: connection is called at the start of the retrieval
     * process
     *
     * @throws org.apache.tools.ant.BuildException
     *
     */

    public void disconnect() {
    }

    /**
     * Test for a repository being reachable. This method is called after {@link
     * #connect(org.apache.tools.ant.taskdefs.optional.repository.GetLibraries)}
     * is called, before any files are to be retrieved.
     * <p/>
     * If it returns false the repository considers itself offline. Similarly,
     * any ioexception is interpreted as being offline.
     * <p/>
     * The Http implementation does nothing
     * @return true if the repository is online.
     *
     * @throws java.io.IOException
     */
    public boolean checkRepositoryReachable() throws IOException {
        return true;
    }



    /**
     * fetch a library from the repository
     *
     * @param library
     *
     * @return true if we retrieved
     *
     * @throws org.apache.tools.ant.BuildException
     *
     */
    public boolean fetch(Library library) throws IOException {

        String path = getRemoteLibraryURL(library);
        logVerbose("Library URL=" + path);
        URL remoteURL=new URL(path);
        logVerbose("destination =" + library.getAbsolutePath());
        long start, finish;
        start = System.currentTimeMillis();
        finish = System.currentTimeMillis();
        boolean useTimestamps = !getOwner().isForceDownload() &&
                !library.exists();
        boolean success=get(remoteURL, library.getLibraryFile(),useTimestamps,
                username, password);
        long diff = finish - start;
        logVerbose("downloaded in " + diff / 1000 + " seconds");

        return success;
    }

    /**
     * get the
     * @param url
     * @param destFile
     * @param useTimestamp
     * @return
     */
    public boolean get(URL url,File destFile,boolean useTimestamp,String user,String passwd)
            throws IOException {
        Get getTask = new Get();
        getTask.setProject(getProject());
        getTask.setTaskName("dependencies");
        getTask.setDest(destFile);
        getTask.setUsername(user);
        getTask.setPassword(passwd);
        getTask.setUseTimestamp(useTimestamp);
        getTask.setSrc(url);
        getTask.setIgnoreErrors(true);
        return getTask.doGet(Project.MSG_VERBOSE,null);
    }

    /**
     * log something at the verbose level
     *
     * @param message text to log
     */
    protected void logVerbose(String message) {
        getOwner().log(message,
                Project.MSG_VERBOSE);
    }

    /**
     * log at debug level
     * @param message
     */
    protected void logDebug(String message) {
        getOwner().log(message,
                Project.MSG_DEBUG);
    }

    /**
     * Get the path to a remote library. This is the full URL
     *
     * @param library
     *
     * @return URL to library
     */
    protected abstract String getRemoteLibraryURL(Library library);

    /**
     * save a stream from a connection to a library. prerequisite: connection
     * open and response=200.
     *
     * @param get
     * @param library
     *
     * @throws java.io.IOException on any trouble.
     */
    /*
    protected void saveStreamToLibrary(GetMethod get, Library library)
            throws IOException {
        //we only get here if we are happy
        //so save it to a temp file
        File tempDest = File.createTempFile("download", ".bin", getOwner()
                .getDestDir());
        logDebug("Saving file to " + tempDest);
        FileOutputStream fos = new FileOutputStream(tempDest);
        InputStream is = get.getResponseBodyAsStream();
        boolean finished = false;
        try {
            byte[] buffer = new byte[BLOCKSIZE];
            int length;

            while ((length = is.read(buffer)) >= 0) {
                fos.write(buffer, 0, length);
            }
            finished = true;
        } finally {
            FileUtils.close(fos);
            FileUtils.close(is);
            // we have started to (over)write dest, but failed.
            // Try to delete the garbage we'd otherwise leave
            // behind.
            if (!finished) {
                logVerbose("Deleting temporary file after failed download");
                tempDest.delete();
            }
        }
        logDebug("download complete; renaming destination file");
        //then copy over the file
        File libraryFile = library.getLibraryFile();
        if (libraryFile.exists()) {
            libraryFile.delete();
        }
        // set the dest file
        if (!tempDest.renameTo(libraryFile)) {
            tempDest.delete();
            throw new IOException(
                    "Could not rename temp file to destination file");
        }
    }
    */

    /**
     * Returns a string representation of the repository
     *
     * @return the base URL
     */
    public String toString() {
        return "Repository at " + getUrl();
    }
}
