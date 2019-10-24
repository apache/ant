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

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.KeepAliveInputStream;
import org.apache.tools.ant.util.KeepAliveOutputStream;
import org.apache.tools.ant.util.TeeOutputStream;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Executes a command on a remote machine via ssh.
 * @since     Ant 1.6 (created February 2, 2003)
 */
public class SSHExec extends SSHBase {

    private static final int BUFFER_SIZE = 8192;
    private static final int RETRY_INTERVAL = 500;

    /** the command to execute via ssh */
    private String command = null;

    /** units are milliseconds, default is 0=infinite */
    private long maxwait = 0;

    /** for waiting for the command to finish */
    private Thread thread = null;

    private String outputProperty = null;   // like <exec>
    private String errorProperty = null;
    private String resultProperty = null;
    private File outputFile = null;   // like <exec>
    private File errorFile = null;
    private String inputProperty = null;
    private String inputString = null;   // like <exec>
    private File inputFile = null;   // like <exec>
    private boolean append = false;   // like <exec>
    private boolean appenderr = false;
    private boolean usePty = false;
    private boolean useSystemIn = false;

    private Resource commandResource = null;

    private static final String TIMEOUT_MESSAGE =
        "Timeout period exceeded, connection dropped.";

    /**
     * To suppress writing logs to System.out
     */
    private boolean suppressSystemOut = false;

    /**
     * To suppress writing logs to System.err
     */
    private boolean suppressSystemErr = false;

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
    public void setCommand(final String command) {
        this.command = command;
    }

    /**
     * Sets a commandResource from a file
     * @param f the value to use.
     * @since Ant 1.7.1
     */
    public void setCommandResource(final String f) {
        this.commandResource = new FileResource(new File(f));
    }

    /**
     * The connection can be dropped after a specified number of
     * milliseconds. This is sometimes useful when a connection may be
     * flaky. Default is 0, which means &quot;wait forever&quot;.
     *
     * @param timeout  The new timeout value in seconds
     */
    public void setTimeout(final long timeout) {
        maxwait = timeout;
    }

    /**
     * If used, stores the output of the command to the given file.
     *
     * @param output  The file to write to.
     */
    public void setOutput(final File output) {
        outputFile = output;
    }

    /**
     * If used, stores the erroutput of the command to the given file.
     *
     * @param output  The file to write to.
     * @since Apache Ant 1.9.4
     */
    public void setErrorOutput(final File output) {
        errorFile = output;
    }

    /**
     * If used, the content of the file is piped to the remote command
     *
     * @param input  The file which provides the input data for the remote command
     *
     * @since Ant 1.8.0
     */
    public void setInput(final File input) {
        inputFile = input;
    }

    /**
     * If used, the content of the property is piped to the remote command
     *
     * @param inputProperty The property which contains the input data
     * for the remote command.
     *
     * @since Ant 1.8.0
     */
    public void setInputProperty(final String inputProperty) {
        this.inputProperty = inputProperty;
    }

    /**
     * If used, the string is piped to the remote command.
     *
     * @param inputString the input data for the remote command.
     *
     * @since Ant 1.8.3
     */
    public void setInputString(final String inputString) {
        this.inputString = inputString;
    }

    /**
     * Determines if the output is appended to the file given in
     * <code>setOutput</code>. Default is false, that is, overwrite
     * the file.
     *
     * @param append  True to append to an existing file, false to overwrite.
     */
    public void setAppend(final boolean append) {
        this.append = append;
    }

    /**
     * Determines if the output is appended to the file given in
     * <code>setErrorOutput</code>. Default is false, that is, overwrite
     * the file.
     *
     * @param appenderr  True to append to an existing file, false to overwrite.
     * @since Apache Ant 1.9.4
     */
    public void setErrAppend(final boolean appenderr) {
        this.appenderr = appenderr;
    }

    /**
     * If set, the output of the command will be stored in the given property.
     *
     * @param property  The name of the property in which the command output
     *      will be stored.
     */
    public void setOutputproperty(final String property) {
        outputProperty = property;
    }

    /**
     * If set, the erroroutput of the command will be stored in the given property.
     *
     * @param property  The name of the property in which the command erroroutput
     *      will be stored.
     * @since Apache Ant 1.9.4
     */
    public void setErrorproperty(final String property) {
        errorProperty = property;
    }

    /**
     * If set, the exitcode of the command will be stored in the given property.
     *
     * @param property  The name of the property in which the exitcode
     *      will be stored.
     * @since Apache Ant 1.9.4
     */
    public void setResultproperty(final String property) {
        resultProperty = property;
    }

    /**
     * Whether a pseudo-tty should be allocated.
     * @param b boolean
     * @since Apache Ant 1.8.3
     */
    public void setUsePty(final boolean b) {
        usePty = b;
    }

    /**
     * If set, input will be taken from System.in
     *
     * @param useSystemIn True to use System.in as InputStream, false otherwise
     * @since Apache Ant 1.9.4
     */
    public void setUseSystemIn(final boolean useSystemIn) {
        this.useSystemIn = useSystemIn;
    }

    /**
     * If suppressSystemOut is <code>true</code>, output will not be sent to System.out,
     * if suppressSystemOut is <code>false</code>, normal behavior
     * @param suppressSystemOut boolean
     * @since Ant 1.9.0
     */
    public void setSuppressSystemOut(final boolean suppressSystemOut) {
        this.suppressSystemOut = suppressSystemOut;
    }

    /**
     * If suppressSystemErr is <code>true</code>, output will not be sent to System.err,
     * if suppressSystemErr is <code>false</code>, normal behavior
     * @param suppressSystemErr boolean
     * @since Ant 1.9.4
     */
    public void setSuppressSystemErr(final boolean suppressSystemErr) {
        this.suppressSystemErr = suppressSystemErr;
    }

    /**
     * Execute the command on the remote host.
     *
     * @exception BuildException  Most likely a network error or bad parameter.
     */
    @Override
    public void execute() throws BuildException {

        if (getHost() == null) {
            throw new BuildException("Host is required.");
        }
        
        loadSshConfig();
        
        if (getUserInfo().getName() == null) {
            throw new BuildException("Username is required.");
        }
        if (getUserInfo().getKeyfile() == null
            && getUserInfo().getPassword() == null) {
            throw new BuildException("Password or Keyfile is required.");
        }
        if (command == null && commandResource == null) {
            throw new BuildException("Command or commandResource is required.");
        }

        final int numberOfInputs = (inputFile != null ? 1 : 0)
            + (inputProperty != null ? 1 : 0)
            + (inputString != null ? 1 : 0);
        if (numberOfInputs > 1) {
            throw new BuildException(
                "You can't specify more than one of inputFile, inputProperty and inputString.");
        }
        if (inputFile != null && !inputFile.exists()) {
            throw new BuildException("The input file %s does not exist.",
                inputFile.getAbsolutePath());
        }

        Session session = null;
        final StringBuilder output = new StringBuilder();
        try {
            session = openSession();
            /* called once */
            if (command != null) {
                log("cmd : " + command, Project.MSG_INFO);
                executeCommand(session, command, output);
            } else { // read command resource and execute for each command
                try (final BufferedReader br = new BufferedReader(
                    new InputStreamReader(commandResource.getInputStream()))) {
                    final Session s = session;
                    br.lines().forEach(cmd -> {
                        log("cmd : " + cmd, Project.MSG_INFO);
                        output.append(cmd).append(" : ");
                        executeCommand(s, cmd, output);
                        output.append("\n");
                    });
                } catch (final IOException e) {
                    if (getFailonerror()) {
                        throw new BuildException(e);
                    }
                    log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
                }
            }
        } catch (final JSchException e) {
            if (getFailonerror()) {
                throw new BuildException(e);
            }
            log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
        } finally {
            if (outputProperty != null) {
                getProject().setNewProperty(outputProperty, output.toString());
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void executeCommand(final Session session, final String cmd, final StringBuilder sb)
        throws BuildException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream errout = new ByteArrayOutputStream();
        final OutputStream teeErr = suppressSystemErr ? errout : new TeeOutputStream(errout, KeepAliveOutputStream.wrapSystemErr());
        final OutputStream tee = suppressSystemOut ? out : new TeeOutputStream(out, KeepAliveOutputStream.wrapSystemOut());

        InputStream istream = null;
        if (inputFile != null) {
            try {
                istream = Files.newInputStream(inputFile.toPath());
            } catch (final IOException e) {
                // because we checked the existence before, this one
                // shouldn't happen What if the file exists, but there
                // are no read permissions?
                log("Failed to read " + inputFile + " because of: "
                    + e.getMessage(), Project.MSG_WARN);
            }
        }
        if (inputProperty != null) {
            final String inputData = getProject().getProperty(inputProperty);
            if (inputData != null) {
                istream = new ByteArrayInputStream(inputData.getBytes());
            }
        }
        if (inputString != null) {
            istream = new ByteArrayInputStream(inputString.getBytes());
        }

        if (useSystemIn) {
            istream = KeepAliveInputStream.wrapSystemIn();
        }

        try {
            final ChannelExec channel;
            session.setTimeout((int) maxwait);
            /* execute the command */
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            channel.setOutputStream(tee);
            channel.setExtOutputStream(tee);
            channel.setErrStream(teeErr);
            if (istream != null) {
                channel.setInputStream(istream);
            }
            channel.setPty(usePty);
            channel.connect();
            // wait for it to finish
            thread =
                    new Thread(() -> {
                        while (!channel.isClosed()) {
                            if (thread == null) {
                                return;
                            }
                            try {
                                Thread.sleep(RETRY_INTERVAL);
                            } catch (final Exception e) {
                                // ignored
                            }
                        }
                    });

            thread.start();
            thread.join(maxwait);

            if (thread.isAlive()) {
                // ran out of time
                thread = null;
                if (getFailonerror()) {
                    throw new BuildException(TIMEOUT_MESSAGE);
                }
                log(TIMEOUT_MESSAGE, Project.MSG_ERR);
            } else {
                // stdout to outputFile
                if (outputFile != null) {
                    writeToFile(out.toString(), append, outputFile);
                }
                // set errorProperty
                if (errorProperty != null) {
                    getProject().setNewProperty(errorProperty, errout.toString());
                }
                // stderr to errorFile
                if (errorFile != null) {
                    writeToFile(errout.toString(), appenderr, errorFile);
                }
                // this is the wrong test if the remote OS is OpenVMS,
                // but there doesn't seem to be a way to detect it.
                final int ec = channel.getExitStatus();
                // set resultproperty
                if (resultProperty != null) {
                    getProject().setNewProperty(resultProperty, Integer.toString(ec));
                }
                if (ec != 0) {
                    final String msg = "Remote command failed with exit status " + ec;
                    if (getFailonerror()) {
                        throw new BuildException(msg);
                    }
                    log(msg, Project.MSG_ERR);
                }
            }
        } catch (final BuildException e) {
            throw e;
        } catch (final JSchException e) {
            if (e.getMessage().contains("session is down")) {
                if (getFailonerror()) {
                    throw new BuildException(TIMEOUT_MESSAGE, e);
                }
                log(TIMEOUT_MESSAGE, Project.MSG_ERR);
            } else {
                if (getFailonerror()) {
                    throw new BuildException(e);
                }
                log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
            }
        } catch (final Exception e) {
            if (getFailonerror()) {
                throw new BuildException(e);
            }
            log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
        } finally {
            sb.append(out.toString());
            FileUtils.close(istream);
        }
    }

    /**
     * Writes a string to a file. If destination file exists, it may be
     * overwritten depending on the "append" value.
     *
     * @param from           string to write
     * @param to             file to write to
     * @param append         if true, append to existing file, else overwrite
     */
    private void writeToFile(final String from, final boolean append, final File to)
        throws IOException {
        final StandardOpenOption appendOrTruncate = append ? StandardOpenOption.APPEND
            : StandardOpenOption.TRUNCATE_EXISTING;
        try (BufferedWriter out = Files.newBufferedWriter(to.getAbsoluteFile().toPath(),
            appendOrTruncate, StandardOpenOption.CREATE)) {
            final StringReader in = new StringReader(from);
            final char[] buffer = new char[BUFFER_SIZE];
            while (true) {
                int bytesRead = in.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }

}
