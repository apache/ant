/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs.optional.ssh;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

/**
 * Executes a command on a remote machine via ssh.
 *
 * @author    Robert Anderson, riznob@hotmail.com
 * @author    Dale Anson, danson@germane-software.com
 * @version   $Revision$
 * @created   February 2, 2003
 * @since     Ant 1.6
 */
public class SSHExec extends SSHBase {

    private String command = null;   // the command to execute via ssh
    private int maxwait = 0;   // units are milliseconds, default is 0=infinite
    private Thread thread = null;   // for waiting for the command to finish

    private String output_property = null;   // like <exec>
    private File output_file = null;   // like <exec>
    private boolean append = false;   // like <exec>

    /**
     * Constructor for SSHExecTask.
     */
    public SSHExec() {
        super();
    }

    /**
     * Sets the command to execute on the remote host.
     *
     * @param command  The new command value
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * The connection can be dropped after a specified number of
     * milliseconds. This is sometimes useful when a connection may be
     * flaky. Default is 0, which means &quot;wait forever&quot;.
     *
     * @param timeout  The new timeout value in seconds
     */
    public void setTimeout(int timeout) {
        maxwait = timeout * 1000;
    }

    /**
     * If used, stores the output of the command to the given file.
     *
     * @param maxwait  The new maxwait value
     */
    public void setOutput(File output) {
        output_file = output;
    }

    /**
     * Should the output be appended to the file given in
     * <code>setOutput</code> ? Default is false, that is, overwrite
     * the file.
     *
     * @param append  True to append to an existing file, false to overwrite.
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * If set, the output of the command will be stored in the given property.
     *
     * @param property  The name of the property in which the command output
     *      will be stored.
     */
    public void setOutputproperty(String property) {
        output_property = property;
    }

    /**
     * Execute the command on the remote host.
     *
     * @exception BuildException  Most likely a network error or bad parameter.
     */
    public void execute() throws BuildException {
        if (getHost() == null) {
            throw new BuildException("Host is required.");
        }
        if (getUserInfo().getName() == null) {
            throw new BuildException("Username is required.");
        }
        if (getUserInfo().getKeyfile() == null 
            && getUserInfo().getPassword() == null) {
            throw new BuildException("Password or Keyfile is required.");
        }
        if (command == null) {
            throw new BuildException("Command is required.");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Tee tee = new Tee(out, System.out);

        try {
            // execute the command
            Session session = openSession();
            session.setTimeout(maxwait);
            final ChannelExec channel=(ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setOutputStream(tee);
            channel.connect();

            // wait for it to finish
            thread =
                new Thread() {
                    public void run() {
                        while (!channel.isEOF()) {
                            if (thread == null) {
                                return;
                            }
                            try {
                                sleep(500);
                            } catch (Exception e) {
                                // ignored
                            }
                        }
                    }
                };
                    
            thread.start();
            thread.join(maxwait);
            
            if (thread.isAlive()) {
                // ran out of time
                thread = null;
                log("Timeout period exceeded, connection dropped.");
            } else {
                // completed successfully
                if (output_property != null) {
                    getProject().setProperty(output_property, out.toString());
                }
                if (output_file != null) {
                    writeToFile(out.toString(), append, output_file);
                }
            }

        } catch(Exception e){
            if (getFailonerror()) {
                throw new BuildException(e);
            } else {
                log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
            }
        }
    }


    /**
     * Writes a string to a file. If destination file exists, it may be
     * overwritten depending on the "append" value.
     *
     * @param from           string to write
     * @param to             file to write to
     * @param append         if true, append to existing file, else overwrite
     * @exception Exception  most likely an IOException
     */
    private void writeToFile(String from, boolean append, File to) 
        throws IOException {
        FileWriter out = null;
        try {
            out = new FileWriter(to.getAbsolutePath(), append);
            StringReader in = new StringReader(from);
            char[] buffer = new char[8192];
            int bytes_read;
            while (true) {
                bytes_read = in.read(buffer);
                if (bytes_read == -1) {
                    break;
                }
                out.write(buffer, 0, bytes_read);
            }
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Similar to standard unix "tee" utility, sends output to two streams.
     *
     * @author    Dale Anson, danson@germane-software.com
     * @version   $Revision$
     */
    public class Tee extends OutputStream {

        private OutputStream left = null;
        private OutputStream right = null;

        /**
         * Constructor for Tee, sends output to both of the given
         * streams, which are referred to as the "teed" streams.
         *
         * @param left   one stream to write to
         * @param right  the other stream to write to
         */
        public Tee(OutputStream left, OutputStream right) {
            if (left == null || right == null) {
                throw new IllegalArgumentException("Both streams are required.");
            }
            this.left = left;
            this.right = right;
        }

        /**
         * Writes the specified byte to both of the teed streams. Per java api,
         * the general contract for write is that one byte is written to the
         * output stream. The byte to be written is the eight low-order bits of
         * the argument b. The 24 high-order bits of b are ignored.
         *
         * @param b
         * @exception IOException  If an IO error occurs
         */
        public void write( int b ) throws IOException {
            left.write( b );
            right.write( b );
        }

        /**
         * Closes both of the teed streams.
         *
         * @exception IOException  If an IO error occurs
         */
        public void close() throws IOException {
            left.close();
            right.close();
        }

        /**
         * Flushes both of the teed streams.
         *
         * @exception IOException  If an IO error occurs
         */
        public void flush() throws IOException {
            left.flush();
            right.flush();
        }
    }

}

