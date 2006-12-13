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
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PipedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Vector;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.util.StringUtils;
import org.apache.tools.ant.util.TeeOutputStream;
import org.apache.tools.ant.util.ReaderInputStream;
import org.apache.tools.ant.util.LeadPipeInputStream;
import org.apache.tools.ant.util.LazyFileOutputStream;
import org.apache.tools.ant.util.OutputStreamFunneler;
import org.apache.tools.ant.util.ConcatFileInputStream;
import org.apache.tools.ant.util.KeepAliveOutputStream;

/**
 * The Redirector class manages the setup and connection of
 * input and output redirection for an Ant project component.
 *
 * @since Ant 1.6
 */
public class Redirector {

    private static final String DEFAULT_ENCODING
        = System.getProperty("file.encoding");

    private class PropertyOutputStream extends ByteArrayOutputStream {
        private String property;
        private boolean closed = false;

        PropertyOutputStream(String property) {
            super();
            this.property = property;
        }

        public void close() throws IOException {
            if (!closed && !(append && appendProperties)) {
                setPropertyFromBAOS(this, property);
                closed = true;
            }
        }
    }

    /**
     * The file(s) from which standard input is being taken.
     * If &gt; 1, files' content will be concatenated in the order received.
     */
    private File[] input;

    /**
     * The file(s) receiving standard output. Will also receive standard error
     * unless standard error is redirected or logError is true.
     */
    private File[] out;

    /**
     * The file(s) to which standard error is being redirected
     */
    private File[] error;

    /**
      * Indicates if standard error should be logged to Ant's log system
      * rather than the output. This has no effect if standard error is
      * redirected to a file or property.
      */
    private boolean logError = false;

    /**
     * Buffer used to capture output for storage into a property
     */
    private PropertyOutputStream baos = null;

    /**
     * Buffer used to capture error output for storage into a property
     */
    private PropertyOutputStream errorBaos = null;

    /** The name of the property into which output is to be stored */
    private String outputProperty;

    /** The name of the property into which error output is to be stored */
    private String errorProperty;

    /** String from which input is taken */
    private String inputString;

    /** Flag which indicates if error and output files are to be appended. */
    private boolean append = false;

    /** Flag which indicates that output should be always sent to the log */
    private boolean alwaysLog = false;

    /** Flag which indicates whether files should be created even when empty. */
    private boolean createEmptyFiles = true;

    /** The task for which this redirector is working */
    private ProjectComponent managingTask;

    /** The stream for output data */
    private OutputStream outputStream = null;

    /** The stream for error output */
    private OutputStream errorStream = null;

    /** The stream for input */
    private InputStream inputStream = null;

    /** Stream which is used for line oriented output */
    private PrintStream outPrintStream = null;

    /** Stream which is used for line oriented error output */
    private PrintStream errorPrintStream = null;

    /** The output filter chains */
    private Vector outputFilterChains;

    /** The error filter chains */
    private Vector errorFilterChains;

    /** The input filter chains */
    private Vector inputFilterChains;

    /** The output encoding */
    private String outputEncoding = DEFAULT_ENCODING;

    /** The error encoding */
    private String errorEncoding = DEFAULT_ENCODING;

    /** The input encoding */
    private String inputEncoding = DEFAULT_ENCODING;

    /** Whether to complete properties settings **/
    private boolean appendProperties = true;

    /** The thread group used for starting <code>StreamPumper</code> threads */
    private ThreadGroup threadGroup = new ThreadGroup("redirector");

    /** whether to log the inputstring */
    private boolean logInputString = true;

    /**
     * Create a redirector instance for the given task
     *
     * @param managingTask the task for which the redirector is to work
     */
    public Redirector(Task managingTask) {
        this((ProjectComponent) managingTask);
    }

    /**
     * Create a redirector instance for the given task
     *
     * @param managingTask the project component for which the
     * redirector is to work
     * @since Ant 1.6.3
     */
    public Redirector(ProjectComponent managingTask) {
        this.managingTask = managingTask;
    }

    /**
     * Set the input to use for the task
     *
     * @param input the file from which input is read.
     */
    public void setInput(File input) {
        setInput((input == null) ? null : new File[] {input});
    }

    /**
     * Set the input to use for the task
     *
     * @param input the files from which input is read.
     */
    public synchronized void setInput(File[] input) {
        this.input = input;
    }

    /**
     * Set the string to use as input
     *
     * @param inputString the string which is used as the input source
     */
    public synchronized void setInputString(String inputString) {
        this.inputString = inputString;
    }

    /**
     * Set whether to include the value of the input string in log messages.
     * Defaults to true.
     * @param logInputString true or false.
     * @since Ant 1.7
     */
    public void setLogInputString(boolean logInputString) {
        this.logInputString = logInputString;
    }

    /**
     * Set a stream to use as input.
     *
     * @param inputStream the stream from which input will be read
     * @since Ant 1.6.3
     */
    /*public*/ void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * File the output of the process is redirected to. If error is not
     * redirected, it too will appear in the output
     *
     * @param out the file to which output stream is written
     */
    public void setOutput(File out) {
        setOutput((out == null) ? null : new File[] {out});
    }

    /**
     * Files the output of the process is redirected to. If error is not
     * redirected, it too will appear in the output
     *
     * @param out the files to which output stream is written
     */
    public synchronized void setOutput(File[] out) {
        this.out = out;
    }

    /**
     * Set the output encoding.
     *
     * @param outputEncoding   <code>String</code>.
     */
    public synchronized void setOutputEncoding(String outputEncoding) {
        if (outputEncoding == null) {
            throw new IllegalArgumentException(
                "outputEncoding must not be null");
        } else {
            this.outputEncoding = outputEncoding;
        }
    }

    /**
     * Set the error encoding.
     *
     * @param errorEncoding   <code>String</code>.
     */
    public synchronized void setErrorEncoding(String errorEncoding) {
        if (errorEncoding == null) {
            throw new IllegalArgumentException(
                "errorEncoding must not be null");
        } else {
            this.errorEncoding = errorEncoding;
        }
    }

    /**
     * Set the input encoding.
     *
     * @param inputEncoding   <code>String</code>.
     */
    public synchronized void setInputEncoding(String inputEncoding) {
        if (inputEncoding == null) {
            throw new IllegalArgumentException(
                "inputEncoding must not be null");
        } else {
            this.inputEncoding = inputEncoding;
        }
    }

    /**
     * Controls whether error output of exec is logged. This is only useful
     * when output is being redirected and error output is desired in the
     * Ant log
     *
     * @param logError if true the standard error is sent to the Ant log system
     *        and not sent to output.
     */
    public synchronized void setLogError(boolean logError) {
        this.logError = logError;
    }

    /**
     * This <code>Redirector</code>'s subordinate
     * <code>PropertyOutputStream</code>s will not set their respective
     * properties <code>while (appendProperties && append)</code>.
     *
     * @param appendProperties whether to append properties.
     */
    public synchronized void setAppendProperties(boolean appendProperties) {
        this.appendProperties = appendProperties;
    }

    /**
     * Set the file to which standard error is to be redirected.
     *
     * @param error the file to which error is to be written
     */
    public void setError(File error) {
        setError((error == null) ? null : new File[] {error});
    }

    /**
     * Set the files to which standard error is to be redirected.
     *
     * @param error the file to which error is to be written
     */
    public synchronized void setError(File[] error) {
        this.error = error;
    }

    /**
     * Property name whose value should be set to the output of
     * the process.
     *
     * @param outputProperty the name of the property to be set with the
     *        task's output.
     */
    public synchronized void setOutputProperty(String outputProperty) {
        if (outputProperty == null
         || !(outputProperty.equals(this.outputProperty))) {
            this.outputProperty = outputProperty;
            baos = null;
        }
    }

    /**
     * Whether output should be appended to or overwrite an existing file.
     * Defaults to false.
     *
     * @param append if true output and error streams are appended to their
     *        respective files, if specified.
     */
    public synchronized void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * If true, (error and non-error) output will be "teed", redirected
     * as specified while being sent to Ant's logging mechanism as if no
     * redirection had taken place.  Defaults to false.
     * @param alwaysLog <code>boolean</code>
     * @since Ant 1.6.3
     */
    public synchronized void setAlwaysLog(boolean alwaysLog) {
        this.alwaysLog = alwaysLog;
    }

    /**
     * Whether output and error files should be created even when empty.
     * Defaults to true.
     * @param createEmptyFiles <code>boolean</code>.
     */
    public synchronized void setCreateEmptyFiles(boolean createEmptyFiles) {
        this.createEmptyFiles = createEmptyFiles;
    }

    /**
     * Property name whose value should be set to the error of
     * the process.
     *
     * @param errorProperty the name of the property to be set
     *        with the error output.
     */
    public synchronized void setErrorProperty(String errorProperty) {
        if (errorProperty == null
         || !(errorProperty.equals(this.errorProperty))) {
            this.errorProperty = errorProperty;
            errorBaos = null;
        }
    }

    /**
     * Set the input <code>FilterChain</code>s.
     *
     * @param inputFilterChains <code>Vector</code> containing <code>FilterChain</code>.
     */
    public synchronized void setInputFilterChains(Vector inputFilterChains) {
        this.inputFilterChains = inputFilterChains;
    }

    /**
     * Set the output <code>FilterChain</code>s.
     *
     * @param outputFilterChains <code>Vector</code> containing <code>FilterChain</code>.
     */
    public synchronized void setOutputFilterChains(Vector outputFilterChains) {
        this.outputFilterChains = outputFilterChains;
    }

    /**
     * Set the error <code>FilterChain</code>s.
     *
     * @param errorFilterChains <code>Vector</code> containing <code>FilterChain</code>.
     */
    public synchronized void setErrorFilterChains(Vector errorFilterChains) {
        this.errorFilterChains = errorFilterChains;
    }

    /**
     * Set a property from a ByteArrayOutputStream
     *
     * @param baos contains the property value.
     * @param propertyName the property name.
     *
     * @exception IOException if the value cannot be read form the stream.
     */
    private void setPropertyFromBAOS(ByteArrayOutputStream baos,
                                     String propertyName) throws IOException {

        BufferedReader in
            = new BufferedReader(new StringReader(Execute.toString(baos)));
        String line = null;
        StringBuffer val = new StringBuffer();
        while ((line = in.readLine()) != null) {
            if (val.length() != 0) {
                val.append(StringUtils.LINE_SEP);
            }
            val.append(line);
        }
        managingTask.getProject().setNewProperty(propertyName, val.toString());
    }

    /**
     * Create the input, error and output streams based on the
     * configuration options.
     */
    public synchronized void createStreams() {
        if (out != null && out.length > 0) {
            String logHead = new StringBuffer("Output ").append(
                ((append) ? "appended" : "redirected")).append(
                " to ").toString();
            outputStream = foldFiles(out, logHead, Project.MSG_VERBOSE);
        }
        if (outputProperty != null) {
            if (baos == null) {
                baos = new PropertyOutputStream(outputProperty);
                managingTask.log("Output redirected to property: "
                    + outputProperty, Project.MSG_VERBOSE);
            }
            //shield it from being closed by a filtering StreamPumper
            OutputStream keepAliveOutput = new KeepAliveOutputStream(baos);
            outputStream = (outputStream == null) ? keepAliveOutput
                : new TeeOutputStream(outputStream, keepAliveOutput);
        } else {
            baos = null;
        }

        if (error != null && error.length > 0) {
            String logHead = new StringBuffer("Error ").append(
                ((append) ? "appended" : "redirected")).append(
                " to ").toString();
            errorStream = foldFiles(error, logHead, Project.MSG_VERBOSE);
        } else if (!(logError || outputStream == null)) {
            long funnelTimeout = 0L;
            OutputStreamFunneler funneler
                = new OutputStreamFunneler(outputStream, funnelTimeout);
            try {
                outputStream = funneler.getFunnelInstance();
                errorStream = funneler.getFunnelInstance();
            } catch (IOException eyeOhEx) {
                throw new BuildException(
                    "error splitting output/error streams", eyeOhEx);
            }
        }
        if (errorProperty != null) {
            if (errorBaos == null) {
                errorBaos = new PropertyOutputStream(errorProperty);
                managingTask.log("Error redirected to property: " + errorProperty,
                    Project.MSG_VERBOSE);
            }
            //shield it from being closed by a filtering StreamPumper
            OutputStream keepAliveError = new KeepAliveOutputStream(errorBaos);
            errorStream = (error == null || error.length == 0) ? keepAliveError
                : new TeeOutputStream(errorStream, keepAliveError);
        } else {
            errorBaos = null;
        }
        if (alwaysLog || outputStream == null) {
            OutputStream outputLog
                = new LogOutputStream(managingTask, Project.MSG_INFO);
            outputStream = (outputStream == null)
                ? outputLog : new TeeOutputStream(outputLog, outputStream);
        }
        if (alwaysLog || errorStream == null) {
            OutputStream errorLog
                = new LogOutputStream(managingTask, Project.MSG_WARN);
            errorStream = (errorStream == null)
                ? errorLog : new TeeOutputStream(errorLog, errorStream);
        }
        if ((outputFilterChains != null && outputFilterChains.size() > 0)
            || !(outputEncoding.equalsIgnoreCase(inputEncoding))) {
            try {
                LeadPipeInputStream snk = new LeadPipeInputStream();
                snk.setManagingComponent(managingTask);

                InputStream outPumpIn = snk;

                Reader reader = new InputStreamReader(outPumpIn, inputEncoding);

                if (outputFilterChains != null && outputFilterChains.size() > 0) {
                    ChainReaderHelper helper = new ChainReaderHelper();
                    helper.setProject(managingTask.getProject());
                    helper.setPrimaryReader(reader);
                    helper.setFilterChains(outputFilterChains);
                    reader = helper.getAssembledReader();
                }
                outPumpIn = new ReaderInputStream(reader, outputEncoding);

                Thread t = new Thread(threadGroup, new StreamPumper(
                    outPumpIn, outputStream, true), "output pumper");
                t.setPriority(Thread.MAX_PRIORITY);
                outputStream = new PipedOutputStream(snk);
                t.start();
            } catch (IOException eyeOhEx) {
                throw new BuildException(
                    "error setting up output stream", eyeOhEx);
            }
        }

        if ((errorFilterChains != null && errorFilterChains.size() > 0)
            || !(errorEncoding.equalsIgnoreCase(inputEncoding))) {
            try {
                LeadPipeInputStream snk = new LeadPipeInputStream();
                snk.setManagingComponent(managingTask);

                InputStream errPumpIn = snk;

                Reader reader = new InputStreamReader(errPumpIn, inputEncoding);

                if (errorFilterChains != null && errorFilterChains.size() > 0) {
                    ChainReaderHelper helper = new ChainReaderHelper();
                    helper.setProject(managingTask.getProject());
                    helper.setPrimaryReader(reader);
                    helper.setFilterChains(errorFilterChains);
                    reader = helper.getAssembledReader();
                }
                errPumpIn = new ReaderInputStream(reader, errorEncoding);

                Thread t = new Thread(threadGroup, new StreamPumper(
                    errPumpIn, errorStream, true), "error pumper");
                t.setPriority(Thread.MAX_PRIORITY);
                errorStream = new PipedOutputStream(snk);
                t.start();
            } catch (IOException eyeOhEx) {
                throw new BuildException(
                    "error setting up error stream", eyeOhEx);
            }
        }

        // if input files are specified, inputString and inputStream are ignored;
        // classes that work with redirector attributes can enforce
        // whatever warnings are needed
        if (input != null && input.length > 0) {
            managingTask.log("Redirecting input from file"
                + ((input.length == 1) ? "" : "s"), Project.MSG_VERBOSE);
            try {
                inputStream = new ConcatFileInputStream(input);
            } catch (IOException eyeOhEx) {
                throw new BuildException(eyeOhEx);
            }
            ((ConcatFileInputStream) inputStream).setManagingComponent(managingTask);
        } else if (inputString != null) {
            StringBuffer buf = new StringBuffer("Using input ");
            if (logInputString) {
                buf.append('"').append(inputString).append('"');
            } else {
                buf.append("string");
            }
            managingTask.log(buf.toString(), Project.MSG_VERBOSE);
            inputStream = new ByteArrayInputStream(inputString.getBytes());
        }

        if (inputStream != null
            && inputFilterChains != null && inputFilterChains.size() > 0) {
            ChainReaderHelper helper = new ChainReaderHelper();
            helper.setProject(managingTask.getProject());
            try {
                helper.setPrimaryReader(
                    new InputStreamReader(inputStream, inputEncoding));
            } catch (IOException eyeOhEx) {
                throw new BuildException(
                    "error setting up input stream", eyeOhEx);
            }
            helper.setFilterChains(inputFilterChains);
            inputStream = new ReaderInputStream(
                helper.getAssembledReader(), inputEncoding);
        }
    }

    /**
     * Create the StreamHandler to use with our Execute instance.
     *
     * @return the execute stream handler to manage the input, output and
     * error streams.
     *
     * @throws BuildException if the execute stream handler cannot be created.
     */
    public synchronized ExecuteStreamHandler createHandler()
        throws BuildException {
        createStreams();
        return new PumpStreamHandler(outputStream, errorStream, inputStream);
    }

    /**
     * Pass output sent to System.out to specified output.
     *
     * @param output the data to be output
     */
    protected synchronized void handleOutput(String output) {
        if (outPrintStream == null) {
            outPrintStream = new PrintStream(outputStream);
        }
        outPrintStream.print(output);
    }

    /**
     * Handle an input request
     *
     * @param buffer the buffer into which data is to be read.
     * @param offset the offset into the buffer at which data is stored.
     * @param length the amount of data to read
     *
     * @return the number of bytes read
     *
     * @exception IOException if the data cannot be read
     */
    protected synchronized int handleInput(byte[] buffer, int offset,
                                           int length) throws IOException {
        if (inputStream == null) {
            return managingTask.getProject().defaultInput(buffer, offset,
                                                          length);
        } else {
            return inputStream.read(buffer, offset, length);
        }
    }

    /**
     * Process data due to a flush operation.
     *
     * @param output the data being flushed.
     */
    protected synchronized void handleFlush(String output) {
        if (outPrintStream == null) {
            outPrintStream = new PrintStream(outputStream);
        }
        outPrintStream.print(output);
        outPrintStream.flush();
    }

    /**
     * Process error output
     *
     * @param output the error output data.
     */
    protected synchronized void handleErrorOutput(String output) {
        if (errorPrintStream == null) {
            errorPrintStream = new PrintStream(errorStream);
        }
        errorPrintStream.print(output);
    }

    /**
     * Handle a flush operation on the error stream
     *
     * @param output the error information being flushed.
     */
    protected synchronized void handleErrorFlush(String output) {
        if (errorPrintStream == null) {
            errorPrintStream = new PrintStream(errorStream);
        }
        errorPrintStream.print(output);
    }

    /**
     * Get the output stream for the redirector
     *
     * @return the redirector's output stream or null if no output
     *         has been configured
     */
    public synchronized OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Get the error stream for the redirector
     *
     * @return the redirector's error stream or null if no output
     *         has been configured
     */
    public synchronized OutputStream getErrorStream() {
        return errorStream;
    }

    /**
     * Get the input stream for the redirector
     *
     * @return the redirector's input stream or null if no output
     *         has been configured
     */
    public synchronized InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Complete redirection.
     *
     * This operation will close any streams and create any specified
     * property values.
     *
     * @throws IOException if the output properties cannot be read from their
     * output streams.
     */
    public synchronized void complete() throws IOException {
        System.out.flush();
        System.err.flush();

        if (inputStream != null) {
            inputStream.close();
        }

        outputStream.flush();
        outputStream.close();

        errorStream.flush();
        errorStream.close();

        //wait for the StreamPumpers to finish
        while (threadGroup.activeCount() > 0) {
            try {
                managingTask.log("waiting for " + threadGroup.activeCount()
                    + " Threads:", Project.MSG_DEBUG);
                Thread[] thread = new Thread[threadGroup.activeCount()];
                threadGroup.enumerate(thread);
                for (int i = 0; i < thread.length && thread[i] != null; i++) {
                    try {
                        managingTask.log(thread[i].toString(), Project.MSG_DEBUG);
                    } catch (NullPointerException enPeaEx) {
                        // Ignore exception
                    }
                }
                wait(1000);
            } catch (InterruptedException eyeEx) {
                // Ignore exception
            }
        }

        setProperties();

        inputStream = null;
        outputStream = null;
        errorStream = null;
        outPrintStream = null;
        errorPrintStream = null;
   }

    /**
     * Notify the <code>Redirector</code> that it is now okay
     * to set any output and/or error properties.
     */
    public synchronized void setProperties() {
        if (baos != null) {
            try {
                baos.close();
            } catch (IOException eyeOhEx) {
                // Ignore exception
            }
        }
        if (errorBaos != null) {
            try {
                errorBaos.close();
            } catch (IOException eyeOhEx) {
                // Ignore exception
            }
        }
    }

    private OutputStream foldFiles(File[] file, String logHead, int loglevel) {
        OutputStream result
            = new LazyFileOutputStream(file[0], append, createEmptyFiles);

        managingTask.log(logHead + file[0], loglevel);
        char[] c = new char[logHead.length()];
        Arrays.fill(c, ' ');
        String indent = new String(c);

        for (int i = 1; i < file.length; i++) {
            outputStream = new TeeOutputStream(outputStream,
                new LazyFileOutputStream(file[i], append, createEmptyFiles));
            managingTask.log(indent + file[i], loglevel);
        }
        return result;
    }
}
