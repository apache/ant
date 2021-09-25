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
package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.util.ConcatFileInputStream;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.KeepAliveOutputStream;
import org.apache.tools.ant.util.LazyFileOutputStream;
import org.apache.tools.ant.util.LeadPipeInputStream;
import org.apache.tools.ant.util.LineOrientedOutputStreamRedirector;
import org.apache.tools.ant.util.NullOutputStream;
import org.apache.tools.ant.util.OutputStreamFunneler;
import org.apache.tools.ant.util.ReaderInputStream;
import org.apache.tools.ant.util.TeeOutputStream;

/**
 * The Redirector class manages the setup and connection of input and output
 * redirection for an Ant project component.
 *
 * @since Ant 1.6
 */
public class Redirector {
    private static final int STREAMPUMPER_WAIT_INTERVAL = 1000;

    private static final String DEFAULT_ENCODING = System
            .getProperty("file.encoding");

    private class PropertyOutputStream extends ByteArrayOutputStream {
        private final String property;

        private boolean closed = false;

        PropertyOutputStream(final String property) {
            super();
            this.property = property;
        }

        @Override
        public void close() throws IOException {
            synchronized (outMutex) {
                if (!closed && !(appendOut && appendProperties)) {
                    setPropertyFromBAOS(this, property);
                    closed = true;
                }
            }
        }
    }

    /**
     * The file(s) from which standard input is being taken. If &gt; 1, files'
     * content will be concatenated in the order received.
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
     * Indicates if standard error should be logged to Ant's log system rather
     * than the output. This has no effect if standard error is redirected to a
     * file or property.
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
    private boolean appendOut = false;

    private boolean appendErr = false;

    /** Flag which indicates that output should be always sent to the log */
    private boolean alwaysLogOut = false;

    private boolean alwaysLogErr = false;

    /** Flag which indicates whether files should be created even when empty. */
    private boolean createEmptyFilesOut = true;

    private boolean createEmptyFilesErr = true;

    /** The task for which this redirector is working */
    private final ProjectComponent managingTask;

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
    private Vector<FilterChain> outputFilterChains;

    /** The error filter chains */
    private Vector<FilterChain> errorFilterChains;

    /** The input filter chains */
    private Vector<FilterChain> inputFilterChains;

    /** The output encoding */
    private String outputEncoding = DEFAULT_ENCODING;

    /** The error encoding */
    private String errorEncoding = DEFAULT_ENCODING;

    /** The input encoding */
    private String inputEncoding = DEFAULT_ENCODING;

    /** Whether to complete properties settings **/
    private boolean appendProperties = true;

    /** The thread group used for starting <code>StreamPumper</code> threads */
    private final ThreadGroup threadGroup = new ThreadGroup("redirector");

    /** whether to log the inputstring */
    private boolean logInputString = true;

    /** Mutex for in */
    private final Object inMutex = new Object();

    /** Mutex for out */
    private final Object outMutex = new Object();

    /** Mutex for err */
    private final Object errMutex = new Object();

    /** Is the output binary or can we safely split it into lines? */
    private boolean outputIsBinary = false;

    /** Flag which indicates if error and output files are to be discarded. */
    private boolean discardOut = false;

    private boolean discardErr = false;

    /**
     * Create a redirector instance for the given task
     *
     * @param managingTask
     *            the task for which the redirector is to work
     */
    public Redirector(final Task managingTask) {
        this((ProjectComponent) managingTask);
    }

    /**
     * Create a redirector instance for the given task
     *
     * @param managingTask
     *            the project component for which the redirector is to work
     * @since Ant 1.6.3
     */
    public Redirector(final ProjectComponent managingTask) {
        this.managingTask = managingTask;
    }

    /**
     * Set the input to use for the task
     *
     * @param input
     *            the file from which input is read.
     */
    public void setInput(final File input) {
        setInput((input == null) ? null : new File[] {input});
    }

    /**
     * Set the input to use for the task
     *
     * @param input
     *            the files from which input is read.
     */
    public void setInput(final File[] input) {
        synchronized (inMutex) {
            if (input == null) {
                this.input = null;
            } else {
                this.input = input.clone();
            }
        }
    }

    /**
     * Set the string to use as input
     *
     * @param inputString
     *            the string which is used as the input source
     */
    public void setInputString(final String inputString) {
        synchronized (inMutex) {
            this.inputString = inputString;
        }
    }

    /**
     * Set whether to include the value of the input string in log messages.
     * Defaults to true.
     *
     * @param logInputString
     *            true or false.
     * @since Ant 1.7
     */
    public void setLogInputString(final boolean logInputString) {
        this.logInputString = logInputString;
    }

    /**
     * Set a stream to use as input.
     *
     * @param inputStream
     *            the stream from which input will be read
     * @since Ant 1.6.3
     */
    /* public */void setInputStream(final InputStream inputStream) {
        synchronized (inMutex) {
            this.inputStream = inputStream;
        }
    }

    /**
     * File the output of the process is redirected to. If error is not
     * redirected, it too will appear in the output
     *
     * @param out
     *            the file to which output stream is written
     */
    public void setOutput(final File out) {
        setOutput((out == null) ? null : new File[] {out});
    }

    /**
     * Files the output of the process is redirected to. If error is not
     * redirected, it too will appear in the output
     *
     * @param out
     *            the files to which output stream is written
     */
    public void setOutput(final File[] out) {
        synchronized (outMutex) {
            if (out == null) {
                this.out = null;
            } else {
                this.out = out.clone();
            }
        }
    }

    /**
     * Set the output encoding.
     *
     * @param outputEncoding
     *            <code>String</code>.
     */
    public void setOutputEncoding(final String outputEncoding) {
        if (outputEncoding == null) {
            throw new IllegalArgumentException(
                    "outputEncoding must not be null");
        }
        synchronized (outMutex) {
            this.outputEncoding = outputEncoding;
        }
    }

    /**
     * Set the error encoding.
     *
     * @param errorEncoding
     *            <code>String</code>.
     */
    public void setErrorEncoding(final String errorEncoding) {
        if (errorEncoding == null) {
            throw new IllegalArgumentException("errorEncoding must not be null");
        }
        synchronized (errMutex) {
            this.errorEncoding = errorEncoding;
        }
    }

    /**
     * Set the input encoding.
     *
     * @param inputEncoding
     *            <code>String</code>.
     */
    public void setInputEncoding(final String inputEncoding) {
        if (inputEncoding == null) {
            throw new IllegalArgumentException("inputEncoding must not be null");
        }
        synchronized (inMutex) {
            this.inputEncoding = inputEncoding;
        }
    }

    /**
     * Controls whether error output of exec is logged. This is only useful when
     * output is being redirected and error output is desired in the Ant log
     *
     * @param logError
     *            if true the standard error is sent to the Ant log system and
     *            not sent to output.
     */
    public void setLogError(final boolean logError) {
        synchronized (errMutex) {
            this.logError = logError;
        }
    }

    /**
     * This <code>Redirector</code>'s subordinate
     * <code>PropertyOutputStream</code>s will not set their respective
     * properties <code>while (appendProperties &amp;&amp; append)</code>.
     *
     * @param appendProperties
     *            whether to append properties.
     */
    public void setAppendProperties(final boolean appendProperties) {
        synchronized (outMutex) {
            this.appendProperties = appendProperties;
        }
    }

    /**
     * Set the file to which standard error is to be redirected.
     *
     * @param error
     *            the file to which error is to be written
     */
    public void setError(final File error) {
        setError((error == null) ? null : new File[] {error});
    }

    /**
     * Set the files to which standard error is to be redirected.
     *
     * @param error
     *            the file to which error is to be written
     */
    public void setError(final File[] error) {
        synchronized (errMutex) {
            if (error == null) {
                this.error = null;
            } else {
                this.error = error.clone();
            }
        }
    }

    /**
     * Property name whose value should be set to the output of the process.
     *
     * @param outputProperty
     *            the name of the property to be set with the task's output.
     */
    public void setOutputProperty(final String outputProperty) {
        if (outputProperty == null
                || !(outputProperty.equals(this.outputProperty))) {
            synchronized (outMutex) {
                this.outputProperty = outputProperty;
                baos = null;
            }
        }
    }

    /**
     * Whether output should be appended to or overwrite an existing file.
     * Defaults to false.
     *
     * @param append
     *            if true output and error streams are appended to their
     *            respective files, if specified.
     */
    public void setAppend(final boolean append) {
        synchronized (outMutex) {
            appendOut = append;
        }
        synchronized (errMutex) {
            appendErr = append;
        }
    }

    /**
     * Whether output should be discarded.
     *
     * <p>Defaults to false.</p>
     *
     * @param discard
     *            if true output streams are discarded.
     *
     * @since Ant 1.10.10
     * @see #setDiscardError
     */
    public void setDiscardOutput(final boolean discard) {
        synchronized (outMutex) {
            discardOut = discard;
        }
    }

    /**
     * Whether error output should be discarded.
     *
     * <p>Defaults to false.</p>
     *
     * @param discard
     *            if true error streams are discarded.
     *
     * @since Ant 1.10.10
     * @see #setDiscardOutput
     */
    public void setDiscardError(final boolean discard) {
        synchronized (errMutex) {
            discardErr = discard;
        }
    }

    /**
     * If true, (error and non-error) output will be "teed", redirected as
     * specified while being sent to Ant's logging mechanism as if no
     * redirection had taken place. Defaults to false.
     *
     * @param alwaysLog
     *            <code>boolean</code>
     * @since Ant 1.6.3
     */
    public void setAlwaysLog(final boolean alwaysLog) {
        synchronized (outMutex) {
            alwaysLogOut = alwaysLog;
        }
        synchronized (errMutex) {
            alwaysLogErr = alwaysLog;
        }
    }

    /**
     * Whether output and error files should be created even when empty.
     * Defaults to true.
     *
     * @param createEmptyFiles
     *            <code>boolean</code>.
     */
    public void setCreateEmptyFiles(final boolean createEmptyFiles) {
        synchronized (outMutex) {
            createEmptyFilesOut = createEmptyFiles;
        }
        synchronized (outMutex) {
            createEmptyFilesErr = createEmptyFiles;
        }
    }

    /**
     * Property name whose value should be set to the error of the process.
     *
     * @param errorProperty
     *            the name of the property to be set with the error output.
     */
    public void setErrorProperty(final String errorProperty) {
        synchronized (errMutex) {
            if (errorProperty == null
                    || !(errorProperty.equals(this.errorProperty))) {
                this.errorProperty = errorProperty;
                errorBaos = null;
            }
        }
    }

    /**
     * Set the input <code>FilterChain</code>s.
     *
     * @param inputFilterChains
     *            <code>Vector</code> containing <code>FilterChain</code>.
     */
    public void setInputFilterChains(final Vector<FilterChain> inputFilterChains) {
        synchronized (inMutex) {
            this.inputFilterChains = inputFilterChains;
        }
    }

    /**
     * Set the output <code>FilterChain</code>s.
     *
     * @param outputFilterChains
     *            <code>Vector</code> containing <code>FilterChain</code>.
     */
    public void setOutputFilterChains(final Vector<FilterChain> outputFilterChains) {
        synchronized (outMutex) {
            this.outputFilterChains = outputFilterChains;
        }
    }

    /**
     * Set the error <code>FilterChain</code>s.
     *
     * @param errorFilterChains
     *            <code>Vector</code> containing <code>FilterChain</code>.
     */
    public void setErrorFilterChains(final Vector<FilterChain> errorFilterChains) {
        synchronized (errMutex) {
            this.errorFilterChains = errorFilterChains;
        }
    }

    /**
     * Whether to consider the output created by the process binary.
     *
     * <p>Binary output will not be split into lines which may make
     * error and normal output look mixed up when they get written to
     * the same stream.</p>
     *
     * @param b boolean
     * @since 1.9.4
     */
    public void setBinaryOutput(final boolean b) {
        outputIsBinary = b;
    }

    /**
     * Set a property from a ByteArrayOutputStream
     *
     * @param baos
     *            contains the property value.
     * @param propertyName
     *            the property name.
     */
    private void setPropertyFromBAOS(final ByteArrayOutputStream baos,
                                     final String propertyName) {
        final BufferedReader in = new BufferedReader(new StringReader(Execute.toString(baos)));
        managingTask.getProject().setNewProperty(propertyName,
                in.lines().collect(Collectors.joining(System.lineSeparator())));
    }

    /**
     * Create the input, error and output streams based on the configuration
     * options.
     */
    public void createStreams() {

        synchronized (outMutex) {
            outStreams();
            if (alwaysLogOut || outputStream == null) {
                final OutputStream outputLog = new LogOutputStream(managingTask,
                        Project.MSG_INFO);
                outputStream = (outputStream == null) ? outputLog
                        : new TeeOutputStream(outputLog, outputStream);
            }

            if ((outputFilterChains != null && outputFilterChains.size() > 0)
                    || !outputEncoding.equalsIgnoreCase(inputEncoding)) {
                try {
                    final LeadPipeInputStream snk = new LeadPipeInputStream();
                    snk.setManagingComponent(managingTask);

                    InputStream outPumpIn = snk;

                    Reader reader = new InputStreamReader(outPumpIn,
                            inputEncoding);

                    if (outputFilterChains != null
                            && outputFilterChains.size() > 0) {
                        final ChainReaderHelper helper = new ChainReaderHelper();
                        helper.setProject(managingTask.getProject());
                        helper.setPrimaryReader(reader);
                        helper.setFilterChains(outputFilterChains);
                        reader = helper.getAssembledReader();
                    }
                    outPumpIn = new ReaderInputStream(reader, outputEncoding);

                    final Thread t = new Thread(threadGroup, new StreamPumper(
                            outPumpIn, outputStream, true), "output pumper");
                    t.setPriority(Thread.MAX_PRIORITY);
                    outputStream = new PipedOutputStream(snk);
                    t.start();
                } catch (final IOException eyeOhEx) {
                    throw new BuildException("error setting up output stream",
                            eyeOhEx);
                }
            }
        }

        synchronized (errMutex) {
            errorStreams();
            if (alwaysLogErr || errorStream == null) {
                final OutputStream errorLog = new LogOutputStream(managingTask,
                        Project.MSG_WARN);
                errorStream = (errorStream == null) ? errorLog
                        : new TeeOutputStream(errorLog, errorStream);
            }

            if ((errorFilterChains != null && errorFilterChains.size() > 0)
                    || !errorEncoding.equalsIgnoreCase(inputEncoding)) {
                try {
                    final LeadPipeInputStream snk = new LeadPipeInputStream();
                    snk.setManagingComponent(managingTask);

                    InputStream errPumpIn = snk;

                    Reader reader = new InputStreamReader(errPumpIn,
                            inputEncoding);

                    if (errorFilterChains != null
                            && errorFilterChains.size() > 0) {
                        final ChainReaderHelper helper = new ChainReaderHelper();
                        helper.setProject(managingTask.getProject());
                        helper.setPrimaryReader(reader);
                        helper.setFilterChains(errorFilterChains);
                        reader = helper.getAssembledReader();
                    }
                    errPumpIn = new ReaderInputStream(reader, errorEncoding);

                    final Thread t = new Thread(threadGroup, new StreamPumper(
                            errPumpIn, errorStream, true), "error pumper");
                    t.setPriority(Thread.MAX_PRIORITY);
                    errorStream = new PipedOutputStream(snk);
                    t.start();
                } catch (final IOException eyeOhEx) {
                    throw new BuildException("error setting up error stream",
                            eyeOhEx);
                }
            }
        }

        synchronized (inMutex) {
            // if input files are specified, inputString and inputStream are
            // ignored;
            // classes that work with redirector attributes can enforce
            // whatever warnings are needed
            if (input != null && input.length > 0) {
                managingTask
                        .log("Redirecting input from file"
                                + ((input.length == 1) ? "" : "s"),
                                Project.MSG_VERBOSE);
                try {
                    inputStream = new ConcatFileInputStream(input);
                } catch (final IOException eyeOhEx) {
                    throw new BuildException(eyeOhEx);
                }
                ((ConcatFileInputStream) inputStream).setManagingComponent(managingTask);
            } else if (inputString != null) {
                final StringBuilder buf = new StringBuilder("Using input ");
                if (logInputString) {
                    buf.append('"').append(inputString).append('"');
                } else {
                    buf.append("string");
                }
                managingTask.log(buf.toString(), Project.MSG_VERBOSE);
                inputStream = new ByteArrayInputStream(inputString.getBytes());
            }

            if (inputStream != null && inputFilterChains != null
                    && inputFilterChains.size() > 0) {
                final ChainReaderHelper helper = new ChainReaderHelper();
                helper.setProject(managingTask.getProject());
                try {
                    helper.setPrimaryReader(new InputStreamReader(inputStream,
                            inputEncoding));
                } catch (final IOException eyeOhEx) {
                    throw new BuildException("error setting up input stream",
                            eyeOhEx);
                }
                helper.setFilterChains(inputFilterChains);
                inputStream = new ReaderInputStream(
                        helper.getAssembledReader(), inputEncoding);
            }
        }
    }

    /** outStreams */
    private void outStreams() {
        final boolean haveOutputFiles = out != null && out.length > 0;
        if (discardOut) {
            if (haveOutputFiles || outputProperty != null) {
                throw new BuildException("Cant discard output when output or outputProperty"
                        + " are set");
            }
            managingTask.log("Discarding output", Project.MSG_VERBOSE);
            outputStream = NullOutputStream.INSTANCE;
            return;
        }
        if (haveOutputFiles) {
            final String logHead = "Output "
                    + ((appendOut) ? "appended" : "redirected") + " to ";
            outputStream = foldFiles(out, logHead, Project.MSG_VERBOSE,
                    appendOut, createEmptyFilesOut);
        }
        if (outputProperty != null) {
            if (baos == null) {
                baos = new PropertyOutputStream(outputProperty);
                managingTask.log("Output redirected to property: "
                        + outputProperty, Project.MSG_VERBOSE);
            }
            // shield it from being closed by a filtering StreamPumper
            final OutputStream keepAliveOutput = new KeepAliveOutputStream(baos);
            outputStream = (outputStream == null) ? keepAliveOutput
                    : new TeeOutputStream(outputStream, keepAliveOutput);
        } else {
            baos = null;
        }
    }

    private void errorStreams() {
        final boolean haveErrorFiles = error != null && error.length > 0;
        if (discardErr) {
            if (haveErrorFiles || errorProperty != null || logError) {
                throw new BuildException("Cant discard error output when error, errorProperty"
                        + " or logError are set");
            }
            managingTask.log("Discarding error output", Project.MSG_VERBOSE);
            errorStream = NullOutputStream.INSTANCE;
            return;
        }
        if (haveErrorFiles) {
            final String logHead = "Error "
                    + ((appendErr) ? "appended" : "redirected") + " to ";
            errorStream = foldFiles(error, logHead, Project.MSG_VERBOSE,
                    appendErr, createEmptyFilesErr);
        } else if (!(logError || outputStream == null) && errorProperty == null) {
            final long funnelTimeout = 0L;
            final OutputStreamFunneler funneler = new OutputStreamFunneler(
                    outputStream, funnelTimeout);
            try {
                outputStream = funneler.getFunnelInstance();
                errorStream = funneler.getFunnelInstance();
                if (!outputIsBinary) {
                    outputStream = new LineOrientedOutputStreamRedirector(outputStream);
                    errorStream = new LineOrientedOutputStreamRedirector(errorStream);
                }
            } catch (final IOException eyeOhEx) {
                throw new BuildException(
                        "error splitting output/error streams", eyeOhEx);
            }
        }
        if (errorProperty != null) {
            if (errorBaos == null) {
                errorBaos = new PropertyOutputStream(errorProperty);
                managingTask.log("Error redirected to property: "
                        + errorProperty, Project.MSG_VERBOSE);
            }
            // shield it from being closed by a filtering StreamPumper
            final OutputStream keepAliveError = new KeepAliveOutputStream(errorBaos);
            errorStream = (error == null || error.length == 0) ? keepAliveError
                    : new TeeOutputStream(errorStream, keepAliveError);
        } else {
            errorBaos = null;
        }
    }

    /**
     * Create the StreamHandler to use with our Execute instance.
     *
     * @return the execute stream handler to manage the input, output and error
     *         streams.
     *
     * @throws BuildException
     *             if the execute stream handler cannot be created.
     */
    public ExecuteStreamHandler createHandler() throws BuildException {
        createStreams();
        final boolean nonBlockingRead = input == null && inputString == null;
        return new PumpStreamHandler(getOutputStream(), getErrorStream(),
                getInputStream(), nonBlockingRead);
    }

    /**
     * Pass output sent to System.out to specified output.
     *
     * @param output
     *            the data to be output
     */
    protected void handleOutput(final String output) {
        synchronized (outMutex) {
            if (outPrintStream == null) {
                outPrintStream = new PrintStream(outputStream);
            }
            outPrintStream.print(output);
        }
    }

    /**
     * Handle an input request
     *
     * @param buffer
     *            the buffer into which data is to be read.
     * @param offset
     *            the offset into the buffer at which data is stored.
     * @param length
     *            the amount of data to read
     *
     * @return the number of bytes read
     *
     * @exception IOException
     *                if the data cannot be read
     */
    protected int handleInput(final byte[] buffer, final int offset, final int length)
            throws IOException {
        synchronized (inMutex) {
            if (inputStream == null) {
                return managingTask.getProject().defaultInput(buffer, offset,
                        length);
            }
            return inputStream.read(buffer, offset, length);

        }
    }

    /**
     * Process data due to a flush operation.
     *
     * @param output
     *            the data being flushed.
     */
    protected void handleFlush(final String output) {
        synchronized (outMutex) {
            if (outPrintStream == null) {
                outPrintStream = new PrintStream(outputStream);
            }
            outPrintStream.print(output);
            outPrintStream.flush();
        }
    }

    /**
     * Process error output
     *
     * @param output
     *            the error output data.
     */
    protected void handleErrorOutput(final String output) {
        synchronized (errMutex) {
            if (errorPrintStream == null) {
                errorPrintStream = new PrintStream(errorStream);
            }
            errorPrintStream.print(output);
        }
    }

    /**
     * Handle a flush operation on the error stream
     *
     * @param output
     *            the error information being flushed.
     */
    protected void handleErrorFlush(final String output) {
        synchronized (errMutex) {
            if (errorPrintStream == null) {
                errorPrintStream = new PrintStream(errorStream);
            }
            errorPrintStream.print(output);
            errorPrintStream.flush();
        }
    }

    /**
     * Get the output stream for the redirector
     *
     * @return the redirector's output stream or null if no output has been
     *         configured
     */
    public OutputStream getOutputStream() {
        synchronized (outMutex) {
            return outputStream;
        }
    }

    /**
     * Get the error stream for the redirector
     *
     * @return the redirector's error stream or null if no output has been
     *         configured
     */
    public OutputStream getErrorStream() {
        synchronized (errMutex) {
            return errorStream;
        }
    }

    /**
     * Get the input stream for the redirector
     *
     * @return the redirector's input stream or null if no output has been
     *         configured
     */
    public InputStream getInputStream() {
        synchronized (inMutex) {
            return inputStream;
        }
    }

    /**
     * Complete redirection.
     *
     * This operation will close any streams and create any specified property
     * values.
     *
     * @throws IOException
     *             if the output properties cannot be read from their output
     *             streams.
     */
    public void complete() throws IOException {
        System.out.flush();
        System.err.flush();

        synchronized (inMutex) {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        synchronized (outMutex) {
            outputStream.flush();
            outputStream.close();
        }

        synchronized (errMutex) {
            errorStream.flush();
            errorStream.close();
        }

        // wait for the StreamPumpers to finish
        synchronized (this) {
            while (threadGroup.activeCount() > 0) {
                try {
                    managingTask.log("waiting for " + threadGroup.activeCount()
                            + " Threads:", Project.MSG_DEBUG);
                    final Thread[] thread = new Thread[threadGroup.activeCount()];
                    threadGroup.enumerate(thread);
                    for (int i = 0; i < thread.length && thread[i] != null; i++) {
                        try {
                            managingTask.log(thread[i].toString(),
                                    Project.MSG_DEBUG);
                        } catch (final NullPointerException enPeaEx) {
                            // Ignore exception
                        }
                    }
                    wait(STREAMPUMPER_WAIT_INTERVAL);
                } catch (final InterruptedException eyeEx) {
                    final Thread[] thread = new Thread[threadGroup.activeCount()];
                    threadGroup.enumerate(thread);
                    for (int i = 0; i < thread.length && thread[i] != null; i++) {
                        thread[i].interrupt();
                    }
                }
            }
        }

        setProperties();

        synchronized (inMutex) {
            inputStream = null;
        }
        synchronized (outMutex) {
            outputStream = null;
            outPrintStream = null;
        }
        synchronized (errMutex) {
            errorStream = null;
            errorPrintStream = null;
        }
    }

    /**
     * Notify the <code>Redirector</code> that it is now okay to set any output
     * and/or error properties.
     */
    public void setProperties() {
        synchronized (outMutex) {
            FileUtils.close(baos);
        }
        synchronized (errMutex) {
            FileUtils.close(errorBaos);
        }
    }

    private OutputStream foldFiles(final File[] file, final String logHead, final int loglevel,
            final boolean append, final boolean createEmptyFiles) {
        final OutputStream result = new LazyFileOutputStream(file[0], append,
                createEmptyFiles);

        managingTask.log(logHead + file[0], loglevel);
        final char[] c = new char[logHead.length()];
        Arrays.fill(c, ' ');
        final String indent = new String(c);

        for (int i = 1; i < file.length; i++) {
            outputStream = new TeeOutputStream(outputStream,
                    new LazyFileOutputStream(file[i], append, createEmptyFiles));
            managingTask.log(indent + file[i], loglevel);
        }
        return result;
    }
}
