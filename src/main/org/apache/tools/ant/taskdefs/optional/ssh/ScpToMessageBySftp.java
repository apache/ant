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

import com.jcraft.jsch.Session;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;

/**
 * Utility class to carry out an upload by sftp.
 */
public class ScpToMessageBySftp extends ScpToMessage/*AbstractSshMessage*/ {

    private static final int HUNDRED_KILOBYTES = 102400;

    private File localFile;
    private String remotePath;
    private List directoryList;

    /**
     * Constructor for a local file to remote.
     * @param verbose if true do verbose logging
     * @param session the scp session to use
     * @param aLocalFile the local file
     * @param aRemotePath the remote path
     * @since Ant 1.7
     */
    public ScpToMessageBySftp(boolean verbose,
                              Session session,
                              File aLocalFile,
                              String aRemotePath) {
        this(verbose, session, aRemotePath);

        this.localFile = aLocalFile;
    }

    /**
     * Constructor for a local directories to remote.
     * @param verbose if true do verbose logging
     * @param session the scp session to use
     * @param aDirectoryList a list of directories
     * @param aRemotePath the remote path
     * @since Ant 1.7
     */
    public ScpToMessageBySftp(boolean verbose,
                              Session session,
                              List aDirectoryList,
                              String aRemotePath) {
        this(verbose, session, aRemotePath);

        this.directoryList = aDirectoryList;
    }

    /**
     * Constructor for ScpToMessage.
     * @param verbose if true do verbose logging
     * @param session the scp session to use
     * @param aRemotePath the remote path
     * @since Ant 1.6.2
     */
    private ScpToMessageBySftp(boolean verbose,
                               Session session,
                               String aRemotePath) {
        super(verbose, session);
        this.remotePath = aRemotePath;
    }

    /**
     * Constructor for ScpToMessage.
     * @param session the scp session to use
     * @param aLocalFile the local file
     * @param aRemotePath the remote path
     */
    public ScpToMessageBySftp(Session session,
                              File aLocalFile,
                              String aRemotePath) {
        this(false, session, aLocalFile, aRemotePath);
    }

    /**
     * Constructor for ScpToMessage.
     * @param session the scp session to use
     * @param aDirectoryList a list of directories
     * @param aRemotePath the remote path
     */
    public ScpToMessageBySftp(Session session,
                              List aDirectoryList,
                              String aRemotePath) {
        this(false, session, aDirectoryList, aRemotePath);
    }

    /**
     * Carry out the transfer.
     * @throws IOException on i/o errors
     * @throws JSchException on errors detected by scp
     */
    public void execute() throws IOException, JSchException {
        if (directoryList != null) {
            doMultipleTransfer();
        }
        if (localFile != null) {
            doSingleTransfer();
        }
        log("done.\n");
    }

    private void doSingleTransfer() throws IOException, JSchException {
        ChannelSftp channel = openSftpChannel();
        try {
            channel.connect();
            try {
                sendFileToRemote(channel, localFile, remotePath);
            } catch (SftpException e) {
                JSchException schException = new JSchException("Could not send '" + localFile
                        + "' to '" + remotePath + "' - "
                        + e.toString());
                schException.initCause(e);
                throw schException;
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void doMultipleTransfer() throws IOException, JSchException {
        ChannelSftp channel = openSftpChannel();
        try {
            channel.connect();

            try {
                try {
                    channel.stat(remotePath);
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        // dir does not exist.
                        channel.mkdir(remotePath);
                    } else {
                        throw new JSchException("failed to access remote dir '"
                                                + remotePath + "'", e);
                    }
                }
                channel.cd(remotePath);
            } catch (SftpException e) {
                throw new JSchException("Could not CD to '" + remotePath
                                        + "' - " + e.toString(), e);
            }
            Directory current = null;
            try {
                for (Iterator i = directoryList.iterator(); i.hasNext();) {
                    current = (Directory) i.next();
                    if (getVerbose()) {
                        log("Sending directory " + current);
                    }
                    sendDirectory(channel, current);
                }
            } catch (SftpException e) {
                String msg = "Error sending directory";
                if (current != null && current.getDirectory() != null) {
                    msg += " '" + current.getDirectory().getName() + "'";
                }
                throw new JSchException(msg, e);
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void sendDirectory(ChannelSftp channel,
                               Directory current)
        throws IOException, SftpException {
        for (Iterator fileIt = current.filesIterator(); fileIt.hasNext();) {
            sendFileToRemote(channel, (File) fileIt.next(), null);
        }
        for (Iterator dirIt = current.directoryIterator(); dirIt.hasNext();) {
            Directory dir = (Directory) dirIt.next();
            sendDirectoryToRemote(channel, dir);
        }
    }

    private void sendDirectoryToRemote(ChannelSftp channel,
                                       Directory directory)
        throws IOException, SftpException {
        String dir = directory.getDirectory().getName();
        try {
            channel.stat(dir);
        } catch (SftpException e) {
            // dir does not exist.
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                channel.mkdir(dir);
            }
        }
        channel.cd(dir);
        sendDirectory(channel, directory);
        channel.cd("..");
    }

    private void sendFileToRemote(ChannelSftp channel,
                                  File localFile,
                                  String remotePath)
        throws IOException, SftpException {
        long filesize = localFile.length();

        if (remotePath == null) {
            remotePath = localFile.getName();
        }

        long startTime = System.currentTimeMillis();
        long totalLength = filesize;

        // only track progress for files larger than 100kb in verbose mode
        boolean trackProgress = getVerbose() && filesize > HUNDRED_KILOBYTES;

        SftpProgressMonitor monitor = null;
        if (trackProgress) {
            monitor = getProgressMonitor();
        }

        try {
            if (this.getVerbose()) {
                log("Sending: " + localFile.getName() + " : " + filesize);
            }
            channel.put(localFile.getAbsolutePath(), remotePath, monitor);
        } finally {
            if (this.getVerbose()) {
                long endTime = System.currentTimeMillis();
                logStats(startTime, endTime, (int) totalLength);
            }
        }
    }

    /**
     * Get the local file.
     * @return the local file.
     */
    public File getLocalFile() {
        return localFile;
    }

    /**
     * Get the remote path.
     * @return the remote path.
     */
    public String getRemotePath() {
        return remotePath;
    }
}
