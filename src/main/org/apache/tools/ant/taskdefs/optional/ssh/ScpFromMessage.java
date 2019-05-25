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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.tools.ant.util.FileUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * A helper object representing an scp download.
 */
public class ScpFromMessage extends AbstractSshMessage {

    private static final int HUNDRED_KILOBYTES = 102400;
    private static final byte LINE_FEED = 0x0a;
    private static final int BUFFER_SIZE = 100 * 1024;

    private String remoteFile;
    private File localFile;
    private boolean isRecursive = false;
    private boolean preserveLastModified = false;

    /**
     * Constructor for ScpFromMessage
     * @param session the ssh session to use
     */
    public ScpFromMessage(final Session session) {
        super(session);
    }

    /**
     * Constructor for ScpFromMessage
     * @param verbose if true do verbose logging
     * @param session the ssh session to use
     * @since Ant 1.7
     */
    public ScpFromMessage(final boolean verbose, final Session session) {
        super(verbose, session);
    }

    /**
     * Constructor for ScpFromMessage.
     * @param verbose if true log extra information
     * @param session the Scp session to use
     * @param aRemoteFile the remote file name
     * @param aLocalFile  the local file
     * @param recursive   if true use recursion (-r option to scp)
     * @since Ant 1.6.2
     */
    public ScpFromMessage(final boolean verbose,
                          final Session session,
                          final String aRemoteFile,
                          final File aLocalFile,
                          final boolean recursive) {
        this(false, session, aRemoteFile, aLocalFile, recursive, false);
    }

    /**
     * Constructor for ScpFromMessage.
     * @param session the Scp session to use
     * @param aRemoteFile the remote file name
     * @param aLocalFile  the local file
     * @param recursive   if true use recursion (-r option to scp)
     */
    public ScpFromMessage(final Session session,
                           final String aRemoteFile,
                           final File aLocalFile,
                           final boolean recursive) {
        this(false, session, aRemoteFile, aLocalFile, recursive);
    }

    /**
     * Constructor for ScpFromMessage.
     * @param verbose if true log extra information
     * @param session the Scp session to use
     * @param aRemoteFile the remote file name
     * @param aLocalFile  the local file
     * @param recursive   if true use recursion (-r option to scp)
     * @param preserveLastModified whether to preserve file
     * modification times
     * @since Ant 1.8.0
     */
    public ScpFromMessage(final boolean verbose,
                          final Session session,
                          final String aRemoteFile,
                          final File aLocalFile,
                          final boolean recursive,
                          final boolean preserveLastModified) {
        this(verbose, session, aRemoteFile, aLocalFile, recursive, preserveLastModified, false);
    }

    /**
     * Constructor for ScpFromMessage.
     * @param verbose if true log extra information
     * @param session the Scp session to use
     * @param aRemoteFile the remote file name
     * @param aLocalFile  the local file
     * @param recursive   if true use recursion (-r option to scp)
     * @param preserveLastModified whether to preserve file
     * @param compressed  if true use compression (-C option to scp)
     * modification times
     * @since Ant 1.9.8
     */
    public ScpFromMessage(boolean verbose,
                          Session session,
                          String aRemoteFile,
                          File aLocalFile,
                          boolean recursive,
                          boolean preserveLastModified,
                          boolean compressed) {
        super(verbose, compressed, session);
        this.remoteFile = aRemoteFile;
        this.localFile = aLocalFile;
        this.isRecursive = recursive;
        this.preserveLastModified = preserveLastModified;
    }

    /**
     * Carry out the transfer.
     * @throws IOException on i/o errors
     * @throws JSchException on errors detected by scp
     */
    @Override
    public void execute() throws IOException, JSchException {
        String command = "scp -f ";
        if (isRecursive) {
            command += "-r ";
        }
        if (getCompressed()) {
            command += "-C ";
        }
        command += remoteFile;
        final Channel channel = openExecChannel(command);
        try {
            // get I/O streams for remote scp
            final OutputStream out = channel.getOutputStream();
            final InputStream in = channel.getInputStream();

            channel.connect();

            sendAck(out);
            startRemoteCpProtocol(in, out, localFile);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
        log("done\n");
    }

    protected boolean getPreserveLastModified() {
        return preserveLastModified;
    }

    private void startRemoteCpProtocol(final InputStream in,
                                       final OutputStream out,
                                       final File localFile)
        throws IOException, JSchException {
        File startFile = localFile;
        while (true) {
            // C0644 filesize filename - header for a regular file
            // T time 0 time 0\n - present if perserve time.
            // D directory - this is the header for a directory.
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            while (true) {
                final int read = in.read();
                if (read < 0) {
                    return;
                }
                if ((byte) read == LINE_FEED) {
                    break;
                }
                stream.write(read);
            }
            final String serverResponse = stream.toString("UTF-8");
            if (serverResponse.charAt(0) == 'C') {
                parseAndFetchFile(serverResponse, startFile, out, in);
            } else if (serverResponse.charAt(0) == 'D') {
                startFile = parseAndCreateDirectory(serverResponse,
                                                    startFile);
                sendAck(out);
            } else if (serverResponse.charAt(0) == 'E') {
                startFile = startFile.getParentFile();
                sendAck(out);
            } else if (serverResponse.charAt(0) == '\01'
                    || serverResponse.charAt(0) == '\02') {
                // this indicates an error.
                throw new IOException(serverResponse.substring(1));
            }
        }
    }

    private File parseAndCreateDirectory(final String serverResponse,
                                         final File localFile) {
        int start = serverResponse.indexOf(' ');
        // appears that the next token is not used and it's zero.
        start = serverResponse.indexOf(' ', start + 1);
        final String directoryName = serverResponse.substring(start + 1);
        if (localFile.isDirectory()) {
            final File dir = new File(localFile, directoryName);
            dir.mkdir();
            log("Creating: " + dir);
            return dir;
        }
        return null;
    }

    private void parseAndFetchFile(final String serverResponse,
                                   final File localFile,
                                   final OutputStream out,
                                   final InputStream in)
        throws IOException, JSchException  {
        int start = 0;
        int end = serverResponse.indexOf(' ', start + 1);
        start = end + 1;
        end = serverResponse.indexOf(' ', start + 1);
        final long filesize = Long.parseLong(serverResponse.substring(start, end));
        final String filename = serverResponse.substring(end + 1);
        log("Receiving: " + filename + " : " + filesize);
        final File transferFile = localFile.isDirectory()
                ? new File(localFile, filename)
                : localFile;
        fetchFile(transferFile, filesize, out, in);
        waitForAck(in);
        sendAck(out);
    }

    private void fetchFile(final File localFile,
                           long filesize,
                           final OutputStream out,
                           final InputStream in)
        throws IOException, JSchException {
        final byte[] buf = new byte[BUFFER_SIZE];
        sendAck(out);

        // read a content of lfile
        final OutputStream fos = Files.newOutputStream(localFile.toPath());
        int length;
        long totalLength = 0;
        final long startTime = System.currentTimeMillis();

        // only track progress for files larger than 100kb in verbose mode
        final boolean trackProgress = getVerbose() && filesize > HUNDRED_KILOBYTES;
        // since filesize keeps on decreasing we have to store the
        // initial filesize
        final long initFilesize = filesize;
        int percentTransmitted = 0;

        try {
            while (true) {
                length = in.read(buf, 0,
                                 BUFFER_SIZE < filesize ? BUFFER_SIZE
                                                          : (int) filesize);
                if (length < 0) {
                    throw new EOFException("Unexpected end of stream.");
                }
                fos.write(buf, 0, length);
                filesize -= length;
                totalLength += length;
                if (trackProgress) {
                    percentTransmitted = trackProgress(initFilesize,
                                                       totalLength,
                                                       percentTransmitted);
                }
                if (filesize == 0) {
                    break;
                }
            }
        } finally {
            final long endTime = System.currentTimeMillis();
            logStats(startTime, endTime, totalLength);
            fos.flush();
            fos.close();
        }

        if (getPreserveLastModified()) {
            setLastModified(localFile);
        }
    }

    private void setLastModified(final File localFile) throws JSchException {
        SftpATTRS fileAttributes = null;
        final ChannelSftp channel = openSftpChannel();
        channel.connect();
        try {
            fileAttributes = channel.lstat(remoteDir(remoteFile)
                                           + localFile.getName());
        } catch (final SftpException e) {
            throw new JSchException("failed to stat remote file", e);
        }
        FileUtils.getFileUtils().setFileLastModified(localFile,
                ((long) fileAttributes.getMTime()) * 1000);
    }

    /**
     * returns the directory part of the remote file, if any.
     */
    private static String remoteDir(final String remoteFile) {
        int index = remoteFile.lastIndexOf('/');
        if (index < 0) {
            index = remoteFile.lastIndexOf('\\');
        }
        return index < 0 ? "" : remoteFile.substring(0, index + 1);
    }
}
