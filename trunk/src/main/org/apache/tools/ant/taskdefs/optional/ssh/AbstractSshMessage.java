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

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.text.NumberFormat;

import org.apache.tools.ant.BuildException;

/**
 * Abstract class for ssh upload and download
 */
public abstract class AbstractSshMessage {
    private static final double ONE_SECOND = 1000.0;

    private Session session;
    private boolean verbose;
    private LogListener listener = new LogListener() {
        public void log(String message) {
            // do nothing;
        }
    };

    /**
     * Constructor for AbstractSshMessage
     * @param session the ssh session to use
     */
    public AbstractSshMessage(Session session) {
        this(false, session);
    }

    /**
     * Constructor for AbstractSshMessage
     * @param verbose if true do verbose logging
     * @param session the ssh session to use
     * @since Ant 1.6.2
     */
    public AbstractSshMessage(boolean verbose, Session session) {
        this.verbose = verbose;
        this.session = session;
    }

    /**
     * Open an ssh channel.
     * @param command the command to use
     * @return the channel
     * @throws JSchException on error
     */
    protected Channel openExecChannel(String command) throws JSchException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        return channel;
    }

    /**
     * Open an ssh sftp channel.
     * @return the channel
     * @throws JSchException on error
     */
    protected ChannelSftp openSftpChannel() throws JSchException {
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");

        return channel;
    }

    /**
     * Send an ack.
     * @param out the output stream to use
     * @throws IOException on error
     */
    protected void sendAck(OutputStream out) throws IOException {
        byte[] buf = new byte[1];
        buf[0] = 0;
        out.write(buf);
        out.flush();
    }

    /**
     * Reads the response, throws a BuildException if the response
     * indicates an error.
     * @param in the input stream to use
     * @throws IOException on I/O error
     * @throws BuildException on other errors
     */
    protected void waitForAck(InputStream in)
        throws IOException, BuildException {
        int b = in.read();

        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,

        if (b == -1) {
            // didn't receive any response
            throw new BuildException("No response from server");
        } else if (b != 0) {
            StringBuffer sb = new StringBuffer();

            int c = in.read();
            while (c > 0 && c != '\n') {
                sb.append((char) c);
                c = in.read();
            }

            if (b == 1) {
                throw new BuildException("server indicated an error: "
                                         + sb.toString());
            } else if (b == 2) {
                throw new BuildException("server indicated a fatal error: "
                                         + sb.toString());
            } else {
                throw new BuildException("unknown response, code " + b
                                         + " message: " + sb.toString());
            }
        }
    }

    /**
     * Carry out the transfer.
     * @throws IOException on I/O errors
     * @throws JSchException on ssh errors
     */
    public abstract void execute() throws IOException, JSchException;

    /**
     * Set a log listener.
     * @param aListener the log listener
     */
    public void setLogListener(LogListener aListener) {
        listener = aListener;
    }

    /**
     * Log a message to the log listener.
     * @param message the message to log
     */
    protected void log(String message) {
        listener.log(message);
    }

    /**
     * Log transfer stats to the log listener.
     * @param timeStarted the time started
     * @param timeEnded   the finishing time
     * @param totalLength the total length
     */
    protected void logStats(long timeStarted,
                             long timeEnded,
                             long totalLength) {
        double duration = (timeEnded - timeStarted) / ONE_SECOND;
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(1);
        listener.log("File transfer time: " + format.format(duration)
            + " Average Rate: " + format.format(totalLength / duration)
            + " B/s");
    }

    /**
     * Is the verbose attribute set.
     * @return true if the verbose attribute is set
     * @since Ant 1.6.2
     */
    protected final boolean getVerbose() {
        return verbose;
    }

    /**
     * Track progress every 10% if 100kb < filesize < 1mb. For larger
     * files track progress for every percent transmitted.
     * @param filesize the size of the file been transmitted
     * @param totalLength the total transmission size
     * @param percentTransmitted the current percent transmitted
     * @return the percent that the file is of the total
     */
    protected final int trackProgress(long filesize, long totalLength,
                                      int percentTransmitted) {

        // CheckStyle:MagicNumber OFF
        int percent = (int) Math.round(Math.floor((totalLength
                                                   / (double) filesize) * 100));

        if (percent > percentTransmitted) {
            if (filesize < 1048576) {
                if (percent % 10 == 0) {
                    if (percent == 100) {
                        System.out.println(" 100%");
                    } else {
                        System.out.print("*");
                    }
                }
            } else {
                if (percent == 50) {
                    System.out.println(" 50%");
                } else if (percent == 100) {
                    System.out.println(" 100%");
                } else {
                    System.out.print(".");
                }
            }
        }
        // CheckStyle:MagicNumber ON

        return percent;
    }

    private ProgressMonitor monitor = null;

    /**
     * Get the progress monitor.
     * @return the progress monitor.
     */
    protected SftpProgressMonitor getProgressMonitor() {
        if (monitor == null) {
            monitor = new ProgressMonitor();
        }
        return monitor;
    }

    private class ProgressMonitor implements SftpProgressMonitor {
        private long initFileSize = 0;
        private long totalLength = 0;
        private int percentTransmitted = 0;

        public void init(int op, String src, String dest, long max) {
            initFileSize = max;
            totalLength = 0;
            percentTransmitted = 0;
        }

        public boolean count(long len) {
            totalLength += len;
            percentTransmitted = trackProgress(initFileSize,
                                               totalLength,
                                               percentTransmitted);
            return true;
        }

        public void end() {
        }

        public long getTotalLength() {
            return totalLength;
        }
    }
}
