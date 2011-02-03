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

import java.io.File;
import java.io.IOException;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpProgressMonitor;

import org.apache.tools.ant.util.FileUtils;

/**
 * A helper object representing an scp download.
 */
public class ScpFromMessageBySftp extends ScpFromMessage {

    private static final int HUNDRED_KILOBYTES = 102400;

    private String remoteFile;
    private File localFile;
    private boolean isRecursive = false;
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
    public ScpFromMessageBySftp(boolean verbose,
                                Session session,
                                String aRemoteFile,
                                File aLocalFile,
                                boolean recursive) {
        this(verbose, session, aRemoteFile, aLocalFile, recursive, false);
    }

    /**
     * Constructor for ScpFromMessageBySftp.
     * @param session the Scp session to use
     * @param aRemoteFile the remote file name
     * @param aLocalFile  the local file
     * @param recursive   if true use recursion
     */
    public ScpFromMessageBySftp(Session session,
                                String aRemoteFile,
                                File aLocalFile,
                                boolean recursive) {
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
    public ScpFromMessageBySftp(boolean verbose,
                                Session session,
                                String aRemoteFile,
                                File aLocalFile,
                                boolean recursive,
                                boolean preserveLastModified) {
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
    public void execute() throws IOException, JSchException {
        ChannelSftp channel = openSftpChannel();
        try {
            channel.connect();
            try {
                SftpATTRS attrs = channel.stat(remoteFile);
                if (attrs.isDir() && !remoteFile.endsWith("/")) {
                    remoteFile = remoteFile + "/";
                }
            } catch (SftpException ee) {
                // Ignored
            }
            getDir(channel, remoteFile, localFile);
        } catch (SftpException e) {
            JSchException schException = new JSchException("Could not get '"+ remoteFile
                    +"' to '"+localFile+"' - "
                    +e.toString());
            schException.initCause(e);
            throw schException;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
        log("done\n");
    }

    private void getDir(ChannelSftp channel,
                        String remoteFile,
                        File localFile) throws IOException, SftpException {
        String pwd = remoteFile;
        if (remoteFile.lastIndexOf('/') != -1) {
            if (remoteFile.length() > 1) {
                pwd = remoteFile.substring(0, remoteFile.lastIndexOf('/'));
            }
        }
        channel.cd(pwd);
        if (!localFile.exists()) {
            localFile.mkdirs();
        }
        java.util.Vector files = channel.ls(remoteFile);
        final int size = files.size();
        for (int i = 0; i < size; i++) {
            ChannelSftp.LsEntry le = (ChannelSftp.LsEntry) files.elementAt(i);
            String name = le.getFilename();
            if (le.getAttrs().isDir()) {
                if (name.equals(".") || name.equals("..")) {
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

    private void getFile(ChannelSftp channel,
                         ChannelSftp.LsEntry le,
                         File localFile) throws IOException, SftpException {
        String remoteFile = le.getFilename();
        if (!localFile.exists()) {
            String path = localFile.getAbsolutePath();
            int i = path.lastIndexOf(File.pathSeparator);
            if (i != -1) {
                if (path.length() > File.pathSeparator.length()) {
                    new File(path.substring(0, i)).mkdirs();
                }
            }
        }

        if (localFile.isDirectory()) {
            localFile = new File(localFile, remoteFile);
        }

        long startTime = System.currentTimeMillis();
        long totalLength = le.getAttrs().getSize();

        SftpProgressMonitor monitor = null;
        boolean trackProgress = getVerbose() && totalLength > HUNDRED_KILOBYTES;
        if (trackProgress) {
            monitor = getProgressMonitor();
        }
        try {
            log("Receiving: " + remoteFile + " : " + le.getAttrs().getSize());
            channel.get(remoteFile, localFile.getAbsolutePath(), monitor);
        } finally {
            long endTime = System.currentTimeMillis();
            logStats(startTime, endTime, (int) totalLength);
        }
        if (getPreserveLastModified()) {
            FileUtils.getFileUtils().setFileLastModified(localFile,
                                                         ((long) le.getAttrs()
                                                          .getMTime())
                                                         * 1000);
        }
    }
}
