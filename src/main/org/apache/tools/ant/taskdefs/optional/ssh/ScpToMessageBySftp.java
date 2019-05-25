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

package org.apache.tools.ant.taskdefs.optional.ssh;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

/**
 * Utility class to carry out an upload by sftp.
 */
public class ScpToMessageBySftp extends ScpToMessage/*AbstractSshMessage*/ {

    private static final int HUNDRED_KILOBYTES = 102400;

    private File localFile;
    private final String remotePath;
    private List<Directory> directoryList;
    private final boolean preserveLastModified;

    /**
     * Constructor for a local file to remote.
     * @param verbose if true do verbose logging
     * @param session the scp session to use
     * @param aLocalFile the local file
     * @param aRemotePath the remote path
     * @since Ant 1.7
     */
    public ScpToMessageBySftp(final boolean verbose,
                              final Session session,
                              final File aLocalFile,
                              final String aRemotePath) {
        this(verbose, session, aLocalFile, aRemotePath, false);

    }

    /**
     * Constructor for a local file to remote.
     * @param verbose if true do verbose logging
     * @param session the scp session to use
     * @param aLocalFile the local file
     * @param aRemotePath the remote path
     * @param preserveLastModified True if the last modified time needs to be preserved
     *                             on the transferred files. False otherwise.
     * @since Ant 1.9.10
     */
    public ScpToMessageBySftp(final boolean verbose,
                              final Session session,
                              final File aLocalFile,
                              final String aRemotePath,
                              final boolean preserveLastModified) {
        this(verbose, session, aRemotePath, preserveLastModified);

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
    public ScpToMessageBySftp(final boolean verbose,
                              final Session session,
                              final List<Directory> aDirectoryList,
                              final String aRemotePath) {
        this(verbose, session, aDirectoryList, aRemotePath, false);
    }

    /**
     * Constructor for a local directories to remote.
     * @param verbose if true do verbose logging
     * @param session the scp session to use
     * @param aDirectoryList a list of directories
     * @param aRemotePath the remote path
     * @param preserveLastModified  True if the last modified time needs to be preserved
     *                             on the transferred files. False otherwise.
     * @since Ant 1.9.10
     */
    public ScpToMessageBySftp(final boolean verbose,
                              final Session session,
                              final List<Directory> aDirectoryList,
                              final String aRemotePath,
                              final boolean preserveLastModified) {
        this(verbose, session, aRemotePath, preserveLastModified);

        this.directoryList = aDirectoryList;
    }


    /**
     * Constructor for ScpToMessage.
     * @param verbose if true do verbose logging
     * @param session the scp session to use
     * @param aRemotePath the remote path
     * @param preserveLastModified True if the last modified time needs to be preserved
     *                             on the transferred files. False otherwise.
     */
    private ScpToMessageBySftp(final boolean verbose,
                               final Session session,
                               final String aRemotePath,
                               final boolean preserveLastModified) {
        super(verbose, session);
        this.remotePath = aRemotePath;
        this.preserveLastModified = preserveLastModified;
    }

    /**
     * Constructor for ScpToMessage.
     * @param session the scp session to use
     * @param aLocalFile the local file
     * @param aRemotePath the remote path
     */
    public ScpToMessageBySftp(final Session session,
                              final File aLocalFile,
                              final String aRemotePath) {
        this(false, session, aLocalFile, aRemotePath);
    }

    /**
     * Constructor for ScpToMessage.
     * @param session the scp session to use
     * @param aDirectoryList a list of directories
     * @param aRemotePath the remote path
     */
    public ScpToMessageBySftp(final Session session,
                              final List<Directory> aDirectoryList,
                              final String aRemotePath) {
        this(false, session, aDirectoryList, aRemotePath);
    }

    /**
     * Carry out the transfer.
     * @throws IOException on i/o errors
     * @throws JSchException on errors detected by scp
     */
    @Override
    public void execute() throws IOException, JSchException {
        if (directoryList != null) {
            doMultipleTransfer();
        }
        if (localFile != null) {
            doSingleTransfer();
        }
        log("done.\n");
    }

    private void doSingleTransfer() throws JSchException {
        final ChannelSftp channel = openSftpChannel();
        try {
            channel.connect();
            try {
                sendFileToRemote(channel, localFile, remotePath);
            } catch (final SftpException e) {
                throw new JSchException("Could not send '" + localFile
                        + "' to '" + remotePath + "' - "
                        + e.toString(), e);
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void doMultipleTransfer() throws IOException, JSchException {
        final ChannelSftp channel = openSftpChannel();
        try {
            channel.connect();

            try {
                try {
                    channel.stat(remotePath);
                } catch (final SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        // dir does not exist.
                        channel.mkdir(remotePath);
                        channel.chmod(getDirMode(), remotePath);
                    } else {
                        throw new JSchException("failed to access remote dir '"
                                                + remotePath + "'", e);
                    }
                }
                channel.cd(remotePath);
            } catch (final SftpException e) {
                throw new JSchException("Could not CD to '" + remotePath
                                        + "' - " + e.toString(), e);
            }
            for (Directory current : directoryList) {
                try {
                    if (getVerbose()) {
                        log("Sending directory " + current);
                    }
                    sendDirectory(channel, current);
                } catch (final SftpException e) {
                    String msg = "Error sending directory";
                    if (current != null && current.getDirectory() != null) {
                        msg += " '" + current.getDirectory().getName() + "'";
                    }
                    throw new JSchException(msg, e);
                }
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void sendDirectory(final ChannelSftp channel,
                               final Directory current)
        throws IOException, SftpException {
        for (final Iterator<File> fileIt = current.filesIterator(); fileIt.hasNext();) {
            sendFileToRemote(channel, fileIt.next(), null);
        }
        for (final Iterator<Directory> dirIt = current.directoryIterator(); dirIt.hasNext();) {
            sendDirectoryToRemote(channel, dirIt.next());
        }
    }

    private void sendDirectoryToRemote(final ChannelSftp channel,
                                       final Directory directory)
        throws IOException, SftpException {
        final String dir = directory.getDirectory().getName();
        try {
            channel.stat(dir);
        } catch (final SftpException e) {
            // dir does not exist.
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                channel.mkdir(dir);
                channel.chmod(getDirMode(), dir);
            }
        }
        channel.cd(dir);
        sendDirectory(channel, directory);
        channel.cd("..");
    }

    private void sendFileToRemote(final ChannelSftp channel,
                                  final File localFile,
                                  String remotePath)
        throws SftpException {
        final long filesize = localFile.length();

        if (remotePath == null) {
            remotePath = localFile.getName();
        }

        final long startTime = System.currentTimeMillis();
        final long totalLength = filesize;

        // only track progress for files larger than 100kb in verbose mode
        final boolean trackProgress = getVerbose() && filesize > HUNDRED_KILOBYTES;

        SftpProgressMonitor monitor = null;
        if (trackProgress) {
            monitor = getProgressMonitor();
        }

        try {
            if (this.getVerbose()) {
                log("Sending: " + localFile.getName() + " : " + filesize);
            }
            channel.put(localFile.getAbsolutePath(), remotePath, monitor);
            // set the fileMode on the transferred file. The "remotePath" can potentially be a directory
            // into which the file got transferred, so we can't/shouldn't go ahead and try to change that directory's
            // permissions. Instead we determine the path of the transferred file on remote.
            final String transferredFileRemotePath;
            if (channel.stat(remotePath).isDir()) {
                // Note: It's correct to use "/" as the file separator without worrying about what the remote
                // server's file separator is, since the SFTP spec expects "/" to be considered as file path
                // separator. See section 6.2 "File Names" of the spec, which states:
                // "This protocol represents file names as strings.  File names are
                // assumed to use the slash ('/') character as a directory separator."
                transferredFileRemotePath = remotePath + "/" + localFile.getName();
            } else {
                transferredFileRemotePath = remotePath;
            }
            if (this.getVerbose()) {
                log("Setting file mode '" + Integer.toOctalString(getFileMode()) + "' on remote path " + transferredFileRemotePath);
            }
            channel.chmod(getFileMode(), transferredFileRemotePath);
            if (getPreserveLastModified()) {
                // set the last modified time (seconds since epoch) on the transferred file
                final int lastModifiedTime = (int) (localFile.lastModified() / 1000L);
                if (this.getVerbose()) {
                    log("Setting last modified time on remote path " + transferredFileRemotePath + " to " + lastModifiedTime);
                }
                channel.setMtime(transferredFileRemotePath, lastModifiedTime);
            }
        } finally {
            if (this.getVerbose()) {
                final long endTime = System.currentTimeMillis();
                logStats(startTime, endTime, (int) totalLength);
            }
        }
    }

    /**
     * Get the local file.
     * @return the local file.
     */
    @Override
    public File getLocalFile() {
        return localFile;
    }

    /**
     * Get the remote path.
     * @return the remote path.
     */
    @Override
    public String getRemotePath() {
        return remotePath;
    }

    /**
     * Returns true if the last modified time needs to be preserved on the
     * file(s) that get transferred. Returns false otherwise.
     *
     * @return boolean
     */
    public boolean getPreserveLastModified() {
        return this.preserveLastModified;
    }
}
