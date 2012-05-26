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

package org.apache.tools.ant.taskdefs.optional.ssh;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.File;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/**
 * Ant task for sending files to remote machine over ssh/scp.
 *
 * @since Ant 1.6
 */
public class Scp extends SSHBase {

    private static final String[] FROM_ATTRS = {
        "file", "localfile", "remotefile" };

    private static final String[] TO_ATTRS = {
        "todir", "localtodir", "remotetodir", "localtofile", "remotetofile" };

    private String fromUri;
    private String toUri;
    private boolean preserveLastModified = false;
    private List fileSets = null;
    private boolean isFromRemote, isToRemote;
    private boolean isSftp = false;

    /**
     * Sets the file to be transferred.  This can either be a remote
     * file or a local file.  Remote files take the form:<br>
     * <i>user:password@host:/directory/path/file.example</i><br>
     * Files to transfer can also include a wildcard to include all
     * files in a remote directory.  For example:<br>
     * <i>user:password@host:/directory/path/*</i><br>
     * @param aFromUri a string representing the file to transfer.
     */
    public void setFile(String aFromUri) {
        setFromUri(aFromUri);
        this.isFromRemote = isRemoteUri(this.fromUri);
    }

    /**
     * Sets the location where files will be transferred to.
     * This can either be a remote directory or a local directory.
     * Remote directories take the form of:<br>
     * <i>user:password@host:/directory/path/</i><br>
     * This parameter is required.

     * @param aToUri a string representing the target of the copy.
     */
    public void setTodir(String aToUri) {
        setToUri(aToUri);
        this.isToRemote = isRemoteUri(this.toUri);
    }

    /**
     * Similiar to {@link #setFile setFile} but explicitly states that
     * the file is a local file.  This is the only way to specify a
     * local file with a @ character.
     * @param aFromUri a string representing the source of the copy.
     * @since Ant 1.6.2
     */
    public void setLocalFile(String aFromUri) {
        setFromUri(aFromUri);
        this.isFromRemote = false;
    }

    /**
     * Similiar to {@link #setFile setFile} but explicitly states that
     * the file is a remote file.
     * @param aFromUri a string representing the source of the copy.
     * @since Ant 1.6.2
     */
    public void setRemoteFile(String aFromUri) {
        validateRemoteUri("remoteFile", aFromUri);
        setFromUri(aFromUri);
        this.isFromRemote = true;
     }

    /**
     * Similiar to {@link #setTodir setTodir} but explicitly states
     * that the directory is a local.  This is the only way to specify
     * a local directory with a @ character.
     * @param aToUri a string representing the target of the copy.
     * @since Ant 1.6.2
     */
    public void setLocalTodir(String aToUri) {
        setToUri(aToUri);
        this.isToRemote = false;
    }

    /**
     * Sets flag to determine if file timestamp from
     * remote system is to be preserved during copy.
     * @since Ant 1.8.0
     */
    public void setPreservelastmodified(boolean yesOrNo) {
    	this.preserveLastModified = yesOrNo;
    }    

    /**
     * Similiar to {@link #setTodir setTodir} but explicitly states
     * that the directory is a remote.
     * @param aToUri a string representing the target of the copy.
     * @since Ant 1.6.2
     */
    public void setRemoteTodir(String aToUri) {
        validateRemoteUri("remoteToDir", aToUri);
        setToUri(aToUri);
        this.isToRemote = true;
    }

    private static void validateRemoteUri(String type, String aToUri) {
    	if (!isRemoteUri(aToUri)) {
            throw new BuildException(type + " '" + aToUri + "' is invalid. "
                                     + "The 'remoteToDir' attribute must "
                                     + "have syntax like the "
                                     + "following: user:password@host:/path"
                                     + " - the :password part is optional");
    	}
    } 

    /**
     * Changes the file name to the given name while receiving it,
     * only useful if receiving a single file.
     * @param aToUri a string representing the target of the copy.
     * @since Ant 1.6.2
     */
    public void setLocalTofile(String aToUri) {
        setToUri(aToUri);
        this.isToRemote = false;
    }

    /**
     * Changes the file name to the given name while sending it,
     * only useful if sending a single file.
     * @param aToUri a string representing the target of the copy.
     * @since Ant 1.6.2
     */
    public void setRemoteTofile(String aToUri) {
        validateRemoteUri("remoteToFile", aToUri);
        setToUri(aToUri);
        this.isToRemote = true;
    }

    /**
     * Setting this to true to use sftp protocol.
     *
     * @param yesOrNo if true sftp protocol will be used.
     */
    public void setSftp(boolean yesOrNo) {
        isSftp = yesOrNo;
    }

    /**
     * Adds a FileSet transfer to remote host.  NOTE: Either
     * addFileSet() or setFile() are required.  But, not both.
     *
     * @param set FileSet to send to remote host.
     */
    public void addFileset(FileSet set) {
        if (fileSets == null) {
            fileSets = new LinkedList();
        }
        fileSets.add(set);
    }

    /**
     * Initialize this task.
     * @throws BuildException on error
     */
    public void init() throws BuildException {
        super.init();
        this.toUri = null;
        this.fromUri = null;
        this.fileSets = null;
    }

    /**
     * Execute this task.
     * @throws BuildException on error
     */
    public void execute() throws BuildException {
        if (toUri == null) {
            throw exactlyOne(TO_ATTRS);
        }
        if (fromUri == null && fileSets == null) {
            throw exactlyOne(FROM_ATTRS, "one or more nested filesets");
        }
        try {
            if (isFromRemote && !isToRemote) {
                download(fromUri, toUri);
            } else if (!isFromRemote && isToRemote) {
                if (fileSets != null) {
                    upload(fileSets, toUri);
                } else {
                    upload(fromUri, toUri);
                }
            } else if (isFromRemote && isToRemote) {
                throw new BuildException(
                    "Copying from a remote server to a remote server is not supported.");
            } else {
                throw new BuildException("'todir' and 'file' attributes "
                    + "must have syntax like the following: "
                    + "user:password@host:/path");
            }
        } catch (Exception e) {
            if (getFailonerror()) {
                if(e instanceof BuildException) {
                    BuildException be = (BuildException) e;
                    if(be.getLocation() == null) {
                        be.setLocation(getLocation());
                    }
                    throw be;
                } else {
                    throw new BuildException(e);
                }
            } else {
                log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
            }
        }
    }

    private void download(String fromSshUri, String toPath)
        throws JSchException, IOException {
        String file = parseUri(fromSshUri);

        Session session = null;
        try {
            session = openSession();
            ScpFromMessage message = null;
            if (!isSftp) {
                message =
                    new ScpFromMessage(getVerbose(), session, file,
                                       getProject().resolveFile(toPath),
                                       fromSshUri.endsWith("*"),
                                       preserveLastModified);
            } else {
                message =
                    new ScpFromMessageBySftp(getVerbose(), session, file,
                                             getProject().resolveFile(toPath),
                                             fromSshUri.endsWith("*"),
                                             preserveLastModified);
            }
            log("Receiving file: " + file);
            message.setLogListener(this);
            message.execute();
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private void upload(List fileSet, String toSshUri)
        throws IOException, JSchException {
        String file = parseUri(toSshUri);

        Session session = null;
        try {
            List list = new ArrayList(fileSet.size());
            for (Iterator i = fileSet.iterator(); i.hasNext();) {
                FileSet set = (FileSet) i.next();
                Directory d = createDirectory(set);
                if (d != null) {
                    list.add(d);
                }
            }
            if (!list.isEmpty()) {
                session = openSession();
                ScpToMessage message = null;
                if (!isSftp) {
                    message = new ScpToMessage(getVerbose(), session,
                                               list, file);
                } else {
                    message = new ScpToMessageBySftp(getVerbose(), session,
                                                     list, file);
                }
                message.setLogListener(this);
                message.execute();
            }
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private void upload(String fromPath, String toSshUri)
        throws IOException, JSchException {
        String file = parseUri(toSshUri);

        Session session = null;
        try {
            session = openSession();
            ScpToMessage message = null;
            if (!isSftp) {
                message =
                    new ScpToMessage(getVerbose(), session,
                                     getProject().resolveFile(fromPath), file);
            } else {
                message =
                    new ScpToMessageBySftp(getVerbose(), session,
                                           getProject().resolveFile(fromPath),
                                           file);
            }
            message.setLogListener(this);
            message.execute();
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private String parseUri(String uri) {

        int indexOfAt = uri.indexOf('@');
        int indexOfColon = uri.indexOf(':');

        if (indexOfColon > -1 && indexOfColon < indexOfAt) {
            // user:password@host:/path notation
            // everything upto the last @ before the last : is considered
            // password. (so if the path contains an @ and a : it will not work)
            int indexOfCurrentAt = indexOfAt;
            int indexOfLastColon = uri.lastIndexOf(':');
            while (indexOfCurrentAt > -1 && indexOfCurrentAt < indexOfLastColon)
            {
                indexOfAt = indexOfCurrentAt;
                indexOfCurrentAt = uri.indexOf('@', indexOfCurrentAt + 1);
            }
            setUsername(uri.substring(0, indexOfColon));
            setPassword(uri.substring(indexOfColon + 1, indexOfAt));
        } else if (indexOfAt > -1) {
            // no password, will require keyfile
            setUsername(uri.substring(0, indexOfAt));
        } else {
            throw new BuildException("no username was given.  Can't authenticate."); 
        }

        if (getUserInfo().getPassword() == null
            && getUserInfo().getKeyfile() == null) {
            throw new BuildException("neither password nor keyfile for user "
                                     + getUserInfo().getName() + " has been "
                                     + "given.  Can't authenticate.");
        }

        int indexOfPath = uri.indexOf(':', indexOfAt + 1);
        if (indexOfPath == -1) {
            throw new BuildException("no remote path in " + uri);
        }

        setHost(uri.substring(indexOfAt + 1, indexOfPath));
        String remotePath = uri.substring(indexOfPath + 1);
        if (remotePath.equals("")) {
            remotePath = ".";
        }
        return remotePath;
    }

    private static boolean isRemoteUri(String uri) {
        boolean isRemote = true;
        int indexOfAt = uri.indexOf('@');
        if (indexOfAt < 0) {
            isRemote = false;
        }
        return isRemote;
    }

    private Directory createDirectory(FileSet set) {
        DirectoryScanner scanner = set.getDirectoryScanner(getProject());
        Directory root = new Directory(scanner.getBasedir());
        String[] files = scanner.getIncludedFiles();
        if (files.length != 0) {
            for (int j = 0; j < files.length; j++) {
                String[] path = Directory.getPath(files[j]);
                Directory current = root;
                File currentParent = scanner.getBasedir();
                for (int i = 0; i < path.length; i++) {
                    File file = new File(currentParent, path[i]);
                    if (file.isDirectory()) {
                        current.addDirectory(new Directory(file));
                        current = current.getChild(file);
                        currentParent = current.getDirectory();
                    } else if (file.isFile()) {
                        current.addFile(file);
                    }
                }
            }
        } else {
            // skip
            root = null;
        }
        return root;
    }

    private void setFromUri(String fromUri) {
        if (this.fromUri != null) {
            throw exactlyOne(FROM_ATTRS);
        }
        this.fromUri = fromUri;
    }

    private void setToUri(String toUri) {
        if (this.toUri != null) {
            throw exactlyOne(TO_ATTRS);
        }
        this.toUri = toUri;
    }

    private BuildException exactlyOne(String[] attrs) {
        return exactlyOne(attrs, null);
    }

    private BuildException exactlyOne(String[] attrs, String alt) {
        StringBuffer buf = new StringBuffer("Exactly one of ").append(
                '[').append(attrs[0]);
        for (int i = 1; i < attrs.length; i++) {
            buf.append('|').append(attrs[i]);
        }
        buf.append(']');
        if (alt != null) {
            buf.append(" or ").append(alt);
        }
        return new BuildException(buf.append(" is required.").toString());
    }
}
