/*
 * Copyright  2003-2004 The Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSchException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Iterator;

public class ScpToMessage extends AbstractSshMessage {

    private final int BUFFER_SIZE = 1024;

    private File localFile;
    private String remotePath;
    private List directoryList;

    /**
     * @since Ant 1.6.2
     */
    public ScpToMessage(boolean verbose,
                        Session session,
                        File aLocalFile,
                        String aRemotePath) {
        this(verbose, session, aRemotePath);

        this.localFile = aLocalFile;
    }

    /**
     * @since Ant 1.6.2
     */
    public ScpToMessage(boolean verbose,
                        Session session,
                        List aDirectoryList,
                        String aRemotePath) {
        this(verbose, session, aRemotePath);

        this.directoryList = aDirectoryList;
    }

    /**
     * @since Ant 1.6.2
     */
    private ScpToMessage(boolean verbose,
                         Session session,
                         String aRemotePath) {
        super(verbose, session);
        this.remotePath = aRemotePath;
    }

    public ScpToMessage(Session session,
                        File aLocalFile,
                        String aRemotePath) {
        this(false, session, aLocalFile, aRemotePath);
    }

    public ScpToMessage(Session session,
                         List aDirectoryList,
                         String aRemotePath) {
        this(false, session, aDirectoryList, aRemotePath);
    }

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
        String cmd = "scp -t " + remotePath;
        Channel channel = openExecChannel(cmd);
        try {

            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

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
        Channel channel = openExecChannel("scp -d -t " + remotePath);
        try {
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            waitForAck(in);
            for (Iterator i = directoryList.iterator(); i.hasNext();) {
                Directory current = (Directory) i.next();
                sendDirectory(current, in, out);
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void sendDirectory(Directory current,
                               InputStream in,
                               OutputStream out) throws IOException {
        for (Iterator fileIt = current.filesIterator(); fileIt.hasNext();) {
            sendFileToRemote((File) fileIt.next(), in, out);
        }
        for (Iterator dirIt = current.directoryIterator(); dirIt.hasNext();) {
            Directory dir = (Directory) dirIt.next();
            sendDirectoryToRemote(dir, in, out);
        }
    }

    private void sendDirectoryToRemote(Directory directory,
                                        InputStream in,
                                        OutputStream out) throws IOException {
        String command = "D0755 0 ";
        command += directory.getDirectory().getName();
        command += "\n";

        out.write(command.getBytes());
        out.flush();

        waitForAck(in);
        sendDirectory(directory, in, out);
        out.write("E\n".getBytes());
        waitForAck(in);
    }

    private void sendFileToRemote(File localFile,
                                   InputStream in,
                                   OutputStream out) throws IOException {
        // send "C0644 filesize filename", where filename should not include '/'
        int filesize = (int) localFile.length();
        String command = "C0644 " + filesize + " ";
        command += localFile.getName();
        command += "\n";

        out.write(command.getBytes());
        out.flush();

        waitForAck(in);

        // send a content of lfile
        FileInputStream fis = new FileInputStream(localFile);
        byte[] buf = new byte[BUFFER_SIZE];
        long startTime = System.currentTimeMillis();
        int totalLength = 0;

        // only track progress for files larger than 100kb in verbose mode
        boolean trackProgress = getVerbose() && filesize > 102400;
        // since filesize keeps on decreasing we have to store the
        // initial filesize
        int initFilesize = filesize;
        int percentTransmitted = 0;

        try {
            log("Sending: " + localFile.getName() + " : " + localFile.length());
            while (true) {
                int len = fis.read(buf, 0, buf.length);
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
            long endTime = System.currentTimeMillis();
            logStats(startTime, endTime, totalLength);
            fis.close();
        }
    }

    public File getLocalFile() {
        return localFile;
    }

    public String getRemotePath() {
        return remotePath;
    }
}
