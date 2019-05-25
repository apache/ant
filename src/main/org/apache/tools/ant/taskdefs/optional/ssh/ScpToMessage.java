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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Utility class to carry out an upload scp transfer.
 */
public class ScpToMessage extends AbstractSshMessage {

    private static final int HUNDRED_KILOBYTES = 102400;
    private static final int BUFFER_SIZE = 100 * 1024;
    private static final int DEFAULT_DIR_MODE = 0755;
    private static final int DEFAULT_FILE_MODE = 0644;

    private File localFile;
    private String remotePath;
    private List<Directory> directoryList;
    private Integer fileMode, dirMode;
    private boolean preserveLastModified;

    /**
     * Constructor for ScpToMessage
     * @param session the ssh session to use
     */
    public ScpToMessage(final Session session) {
        super(session);
    }

    /**
     * Constructor for ScpToMessage
     * @param verbose if true do verbose logging
     * @param session the ssh session to use
     * @since Ant 1.7
     */
    public ScpToMessage(final boolean verbose, final Session session) {
        this(verbose, false, session);
    }

    /**
     * Constructor for ScpToMessage
     * @param verbose if true do verbose logging
     * @param compressed if true use compression
     * @param session the ssh session to use
     * @since Ant 1.9.8
     */
    public ScpToMessage(final boolean verbose, boolean compressed, final Session session) {
        super(verbose, compressed, session);
    }

    /**
     * Constructor for a local file to remote.
     * @param verbose if true do verbose logging
     * @param session the scp session to use
     * @param aLocalFile the local file
     * @param aRemotePath the remote path
     * @param preserveLastModified whether to preserve the last modified timestamps
     * @since Ant 1.9.7
     */
    public ScpToMessage(final boolean verbose,
                        final Session session,
                        final File aLocalFile,
                        final String aRemotePath,
                        final boolean preserveLastModified) {
        this(verbose, false, session, aLocalFile, aRemotePath, preserveLastModified);
    }

    /**
     * Constructor for a local file to remote.
     * @param verbose if true do verbose logging
     * @param compressed if true use compression
     * @param session the scp session to use
     * @param aLocalFile the local file
     * @param aRemotePath the remote path
     * @param preserveLastModified whether to preserve the last modified timestamps
     * @since Ant 1.9.8
     */
    public ScpToMessage(final boolean verbose,
                        final boolean compressed,
                        final Session session,
                        final File aLocalFile,
                        final String aRemotePath,
                        final boolean preserveLastModified) {
        this(verbose, compressed, session, aRemotePath);
        this.localFile = aLocalFile;
        this.preserveLastModified = preserveLastModified;
    }

    /**
     * Constructor for a local directories to remote.
     * @param verbose if true do verbose logging
     * @param session the scp session to use
     * @param aDirectoryList a list of directories
     * @param aRemotePath the remote path
     * @param preserveLastModified whether to preserve the last modified timestamps
     * @since Ant 1.9.7
     */
    public ScpToMessage(final boolean verbose,
                        final Session session,
                        final List<Directory> aDirectoryList,
                        final String aRemotePath,
                        final boolean preserveLastModified) {
        this(verbose, false, session, aDirectoryList, aRemotePath, preserveLastModified);
    }

    /**
     * Constructor for a local directories to remote.
     * @param verbose if true do verbose logging
     * @param compressed whether to use compression
     * @param session the scp session to use
     * @param aDirectoryList a list of directories
     * @param aRemotePath the remote path
     * @param preserveLastModified whether to preserve the last modified timestamps
     * @since Ant 1.9.8
     */
    public ScpToMessage(final boolean verbose,
                        final boolean compressed,
                        final Session session,
                        final List<Directory> aDirectoryList,
                        final String aRemotePath,
                        final boolean preserveLastModified) {
        this(verbose, compressed, session, aRemotePath);
        this.directoryList = aDirectoryList;
        this.preserveLastModified = preserveLastModified;
    }

    /**
     * Constructor for a local file to remote.
     * @param verbose if true do verbose logging
     * @param session the scp session to use
     * @param aLocalFile the local file
     * @param aRemotePath the remote path
     * @since Ant 1.6.2
     */
    public ScpToMessage(final boolean verbose,
                        final Session session,
                        final File aLocalFile,
                        final String aRemotePath) {
        this(verbose, session, aLocalFile, aRemotePath, false);
    }

    /**
     * Constructor for a local directories to remote.
     * @param verbose if true do verbose logging
     * @param session the scp session to use
     * @param aDirectoryList a list of directories
     * @param aRemotePath the remote path
     * @since Ant 1.6.2
     */
    public ScpToMessage(final boolean verbose,
                        final Session session,
                        final List<Directory> aDirectoryList,
                        final String aRemotePath) {
        this(verbose, session, aDirectoryList, aRemotePath, false);
    }

    /**
     * Constructor for ScpToMessage.
     * @param verbose if true do verbose logging
     * @param compressed if true use compression
     * @param session the scp session to use
     * @param aRemotePath the remote path
     * @since Ant 1.9.8
     */
    private ScpToMessage(final boolean verbose,
                         final boolean compressed,
                         final Session session,
                         final String aRemotePath) {
        super(verbose, compressed, session);
        this.remotePath = aRemotePath;
    }

    /**
     * Constructor for ScpToMessage.
     * @param session the scp session to use
     * @param aLocalFile the local file
     * @param aRemotePath the remote path
     */
    public ScpToMessage(final Session session,
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
    public ScpToMessage(final Session session,
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

    private void doSingleTransfer() throws IOException, JSchException {
        StringBuilder sb = new StringBuilder("scp -t ");
        if (getPreserveLastModified()) {
            sb.append("-p ");
        }
        if (getCompressed()) {
            sb.append("-C ");
        }
        sb.append(remotePath);
        final String cmd = sb.toString();
        final Channel channel = openExecChannel(cmd);
        try {
            final OutputStream out = channel.getOutputStream();
            final InputStream in = channel.getInputStream();

            channel.connect();

            waitForAck(in);
            sendFileToRemote(localFile, in, out);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void doMultipleTransfer() throws IOException, JSchException {
        StringBuilder sb = new StringBuilder("scp -r -d -t ");
        if (getPreserveLastModified()) {
            sb.append("-p ");
        }
        if (getCompressed()) {
            sb.append("-C ");
        }
        sb.append(remotePath);
        final Channel channel = openExecChannel(sb.toString());
        try {
            final OutputStream out = channel.getOutputStream();
            final InputStream in = channel.getInputStream();

            channel.connect();

            waitForAck(in);
            for (Directory current : directoryList) {
                sendDirectory(current, in, out);
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void sendDirectory(final Directory current,
                               final InputStream in,
                               final OutputStream out) throws IOException {
        for (final Iterator<File> fileIt = current.filesIterator(); fileIt.hasNext();) {
            sendFileToRemote(fileIt.next(), in, out);
        }
        for (final Iterator<Directory> dirIt = current.directoryIterator(); dirIt.hasNext();) {
            sendDirectoryToRemote(dirIt.next(), in, out);
        }
    }

    private void sendDirectoryToRemote(final Directory directory,
                                        final InputStream in,
                                        final OutputStream out) throws IOException {
        String command = "D0";
        command += Integer.toOctalString(getDirMode());
        command += " 0 ";
        command += directory.getDirectory().getName();
        command += "\n";

        out.write(command.getBytes());
        out.flush();

        waitForAck(in);
        sendDirectory(directory, in, out);
        out.write("E\n".getBytes());
        out.flush();
        waitForAck(in);
    }

    private void sendFileToRemote(final File localFile,
                                   final InputStream in,
                                   final OutputStream out) throws IOException {
        // send "C0644 filesize filename", where filename should not include '/'
        final long filesize = localFile.length();

        if (getPreserveLastModified()) {
            String command = "T" + (localFile.lastModified() / 1000) + " 0";
            command += " " + (localFile.lastModified() / 1000) + " 0\n";
            out.write(command.getBytes());
            out.flush();

            waitForAck(in);
        }

        String command = "C0";
        command += Integer.toOctalString(getFileMode());
        command += " " + filesize + " ";
        command += localFile.getName();
        command += "\n";

        out.write(command.getBytes());
        out.flush();

        waitForAck(in);

        // send a content of lfile
        final byte[] buf = new byte[BUFFER_SIZE];
        final long startTime = System.currentTimeMillis();
        long totalLength = 0;

        // only track progress for files larger than 100kb in verbose mode
        final boolean trackProgress = getVerbose() && filesize > HUNDRED_KILOBYTES;
        // since filesize keeps on decreasing we have to store the
        // initial filesize
        final long initFilesize = filesize;
        int percentTransmitted = 0;

        try (InputStream fis = Files.newInputStream(localFile.toPath())) {
            if (this.getVerbose()) {
                log("Sending: " + localFile.getName() + " : " + localFile.length());
            }
            while (true) {
                final int len = fis.read(buf, 0, buf.length);
                if (len <= 0) {
                    break;
                }
                out.write(buf, 0, len);
                totalLength += len;

                if (trackProgress) {
                    percentTransmitted = trackProgress(initFilesize,
                                                       totalLength,
                                                       percentTransmitted);
                }
            }
            out.flush();
            sendAck(out);
            waitForAck(in);
        } finally {
            if (this.getVerbose()) {
                final long endTime = System.currentTimeMillis();
                logStats(startTime, endTime, totalLength);
            }
        }
    }

    /**
     * Get the local file
     * @return the local file
     */
    public File getLocalFile() {
        return localFile;
    }

    /**
     * Get the remote path
     * @return the remote path
     */
    public String getRemotePath() {
        return remotePath;
    }

    /**
     * Set the file mode, defaults to 0644.
     * @param fileMode int
     * @since Ant 1.9.5
     */
    public void setFileMode(int fileMode) {
        this.fileMode = fileMode;
    }

    /**
     * Get the file mode.
     * @return int
     * @since Ant 1.9.5
     */
    public int getFileMode() {
        return fileMode != null ? fileMode : DEFAULT_FILE_MODE;
    }

    /**
     * Set the dir mode, defaults to 0755.
     * @param dirMode int
     * @since Ant 1.9.5
     */
    public void setDirMode(int dirMode) {
        this.dirMode = dirMode;
    }

    /**
     * Get the dir mode.
     * @return int
     * @since Ant 1.9.5
     */
    public int getDirMode() {
        return dirMode != null ? dirMode : DEFAULT_DIR_MODE;
    }

    /**
     * Whether to preserve the last modified time.
     * @return boolean
     * @since Ant 1.9.7
     */
    public boolean getPreserveLastModified() {
        return preserveLastModified;
    }

}
