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
import java.util.List;
import org.apache.tools.ant.util.FileUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

/**
 * A helper object representing an scp download.
 */
public class ScpFromMessageBySftp extends ScpFromMessage {

    private static final int HUNDRED_KILOBYTES = 102400;

    private String remoteFile;
    private final File localFile;
    @SuppressWarnings("unused")
    private boolean isRecursive = false;
    @SuppressWarnings("unused")
    private boolean verbose = false;

    /**
     * Constructor for ScpFromMessageBySftp.
     * @param verbose if true log extra information
     * @param session the Scp session to use
     * @param aRemoteFile the remote file name
     * @param aLocalFile  the local file
     * @param recursive   if true use recursion
     * @since Ant 1.7
     */
    public ScpFromMessageBySftp(final boolean verbose,
                                final Session session,
                                final String aRemoteFile,
                                final File aLocalFile,
                                final boolean recursive) {
        this(verbose, session, aRemoteFile, aLocalFile, recursive, false);
    }

    /**
     * Constructor for ScpFromMessageBySftp.
     * @param session the Scp session to use
     * @param aRemoteFile the remote file name
     * @param aLocalFile  the local file
     * @param recursive   if true use recursion
     */
    public ScpFromMessageBySftp(final Session session,
                                final String aRemoteFile,
                                final File aLocalFile,
                                final boolean recursive) {
        this(false, session, aRemoteFile, aLocalFile, recursive);
    }

    /**
     * Constructor for ScpFromMessageBySftp.
     * @param verbose if true log extra information
     * @param session the Scp session to use
     * @param aRemoteFile the remote file name
     * @param aLocalFile  the local file
     * @param recursive   if true use recursion
     * @param preserveLastModified whether to preserve file
     * modification times
     * @since Ant 1.8.0
     */
    public ScpFromMessageBySftp(final boolean verbose,
                                final Session session,
                                final String aRemoteFile,
                                final File aLocalFile,
                                final boolean recursive,
                                final boolean preserveLastModified) {
        super(verbose, session, aRemoteFile, aLocalFile, recursive,
              preserveLastModified);
        this.verbose = verbose;
        this.remoteFile = aRemoteFile;
        this.localFile = aLocalFile;
        this.isRecursive = recursive;
    }

    /**
     * Carry out the transfer.
     * @throws IOException on i/o errors
     * @throws JSchException on errors detected by scp
     */
    @Override
    public void execute() throws IOException, JSchException {
        final ChannelSftp channel = openSftpChannel();
        try {
            channel.connect();
            try {
                final SftpATTRS attrs = channel.stat(remoteFile);
                if (attrs.isDir() && !remoteFile.endsWith("/")) {
                    remoteFile += "/";
                }
            } catch (final SftpException ee) {
                // Ignored
            }
            getDir(channel, remoteFile, localFile);
        } catch (final SftpException e) {
            throw new JSchException("Could not get '" + remoteFile + "' to '"
                + localFile + "' - " + e.toString(), e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
        log("done\n");
    }

    private void getDir(final ChannelSftp channel,
                        final String remoteFile,
                        final File localFile) throws SftpException {
        String pwd = remoteFile;
        final int lastIndexOfFileSeparator = remoteFile.lastIndexOf('/');
        if (lastIndexOfFileSeparator != -1) {
            if (remoteFile.length() > 1) {
                if (lastIndexOfFileSeparator == 0) {
                    // the file path is of the form "/foo....." i.e. the file separator
                    // occurs at the start (and only there).
                    pwd = "/";
                } else {
                    pwd = remoteFile.substring(0, lastIndexOfFileSeparator);
                }
            }
        }
        channel.cd(pwd);
        if (!localFile.exists()) {
            localFile.mkdirs();
        }
        @SuppressWarnings("unchecked")
        final List<ChannelSftp.LsEntry> files = channel.ls(remoteFile);
        for (ChannelSftp.LsEntry le : files) {
            final String name = le.getFilename();
            if (le.getAttrs().isDir()) {
                if (".".equals(name) || "..".equals(name)) {
                    continue;
                }
                getDir(channel,
                       channel.pwd() + "/" + name + "/",
                       new File(localFile, le.getFilename()));
            } else {
                getFile(channel, le, localFile);
            }
        }
        channel.cd("..");
    }

    private void getFile(final ChannelSftp channel,
                         final ChannelSftp.LsEntry le,
                         File localFile) throws SftpException {
        final String remoteFile = le.getFilename();
        if (!localFile.exists()) {
            final String path = localFile.getAbsolutePath();
            final int i = path.lastIndexOf(File.pathSeparator);
            if (i != -1) {
                if (path.length() > File.pathSeparator.length()) {
                    new File(path.substring(0, i)).mkdirs();
                }
            }
        }

        if (localFile.isDirectory()) {
            localFile = new File(localFile, remoteFile);
        }

        final long startTime = System.currentTimeMillis();
        final long totalLength = le.getAttrs().getSize();

        SftpProgressMonitor monitor = null;
        final boolean trackProgress = getVerbose() && totalLength > HUNDRED_KILOBYTES;
        if (trackProgress) {
            monitor = getProgressMonitor();
        }
        try {
            log("Receiving: " + remoteFile + " : " + le.getAttrs().getSize());
            channel.get(remoteFile, localFile.getAbsolutePath(), monitor);
        } finally {
            final long endTime = System.currentTimeMillis();
            logStats(startTime, endTime, (int) totalLength);
        }
        if (getPreserveLastModified()) {
            FileUtils.getFileUtils().setFileLastModified(localFile,
                    ((long) le.getAttrs().getMTime()) * 1000);
        }
    }
}
