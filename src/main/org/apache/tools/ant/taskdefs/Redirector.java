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
package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.LazyFileOutputStream;
import org.apache.tools.ant.util.StringUtils;
import org.apache.tools.ant.util.TeeOutputStream;

/**
 * The Redirector class manages the setup and connection of
 * input and output redirection for an Ant task.
 *
 * @author Conor MacNeill
 * @since Ant 1.6
 */
public class Redirector {
    /**
     * The file receiving standard output. Will also receive standard error
     * unless standard error is redirected or logError is true.
     */
    private File out;

    /**
     * The file to which standard error is being redirected
     */
    private File error;

    /**
     * The file from which standard input is being taken.
     */
    private File input;

    /**
      * Indicates if standard error should be logged to Ant's log system
      * rather than the output. This has no effect if standard error is
      * redirected to a file or property.
      */
    private boolean logError = false;

    /**
     * Buffer used to capture output for storage into a property
     */
    private ByteArrayOutputStream baos = null;

    /**
     * Buffer used to capture error output for storage into a property
     */
    private ByteArrayOutputStream errorBaos = null;

    /** The name of the property into which output is to be stored */
    private String outputProperty;

    /** The name of the property into which error output is to be stored */
    private String errorProperty;

    /** String from which input is taken */
    private String inputString;

    /** Flag which indicates if error and output files are to be appended. */
    private boolean append = false;

    /** The task for which this redirector is working */
    private Task managingTask;

    /** The stream for output data */
    private OutputStream outputStream = null;

    /** The stream for error output */
    private OutputStream errorStream = null;

    /** The stream for input */
    private InputStream inputStream = null;

    /** Stream which are used for line oriented output */
    private PrintStream outPrintStream = null;

    /** Stream which is used for line oriented error output */
    private PrintStream errorPrintStream = null;

    /**
     * Create a redirector instance for the given task
     *
     * @param managingTask the task for which the redirector is to work
     */
    public Redirector(Task managingTask) {
        this.managingTask = managingTask;
    }

    /**
     * Set the input to use for the task
     *
     * @param input the file from which input is read.
     */
    public void setInput(File input) {
        this.input = input;
    }

    /**
     * Set the string to use as input
     *
     * @param inputString the string which is used as the input source
     */
    public void setInputString(String inputString) {
        this.inputString = inputString;
    }


    /**
     * File the output of the process is redirected to. If error is not
     * redirected, it too will appear in the output
     *
     * @param out the file to which output stream is written
     */
    public void setOutput(File out) {
        this.out = out;
    }

    /**
     * Controls whether error output of exec is logged. This is only useful
     * when output is being redirected and error output is desired in the
     * Ant log
     *
     * @param logError if true the standard error is sent to the Ant log system
     *        and not sent to output.
     */
    public void setLogError(boolean logError) {
        this.logError = logError;
    }

    /**
     * Set the file to which standard error is to be redirected.
     *
     * @param error the file to which error is to be written
     */
    public void setError(File error) {
        this.error = error;
    }

    /**
     * Property name whose value should be set to the output of
     * the process.
     *
     * @param outputProperty the name of the property to be set with the
     *        task's output.
     */
    public void setOutputProperty(String outputProperty) {
        this.outputProperty = outputProperty;
    }

    /**
     * Whether output should be appended to or overwrite an existing file.
     * Defaults to false.
     *
     * @param append if true output and error streams are appended to their
     *        respective files, if specified.
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * Property name whose value should be set to the error of
     * the process.
     *
     * @param errorProperty the name of the property to be set
     *        with the error output.
     */
    public void setErrorProperty(String errorProperty) {
        this.errorProperty = errorProperty;
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

        BufferedReader in =
            new BufferedReader(new StringReader(Execute.toString(baos)));
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
    public void createStreams() {
        if (out == null && outputProperty == null) {
            outputStream = new LogOutputStream(managingTask, Project.MSG_INFO);
            errorStream = new LogOutputStream(managingTask, Project.MSG_WARN);
        } else {
            if (out != null)  {
                outputStream = new LazyFileOutputStream(out, append, true);
                managingTask.log("Output redirected to " + out,
                                 Project.MSG_VERBOSE);
            }

            if (outputProperty != null) {
                baos = new ByteArrayOutputStream();
                managingTask.log("Output redirected to property: "
                    + outputProperty, Project.MSG_VERBOSE);
                if (out == null) {
                    outputStream = baos;
                } else {
                    outputStream = new TeeOutputStream(outputStream, baos);
                }
            } else {
                baos = null;
            }

            errorStream = outputStream;
        }

        if (logError) {
            errorStream = new LogOutputStream(managingTask, Project.MSG_WARN);
        }

        if (error != null)  {
            errorStream = new LazyFileOutputStream(error, append, true);
            managingTask.log("Error redirected to " + error,
                             Project.MSG_VERBOSE);
        }

        if (errorProperty != null) {
            errorBaos = new ByteArrayOutputStream();
            managingTask.log("Error redirected to property: " + errorProperty,
                Project.MSG_VERBOSE);
            if (error == null) {
                errorStream = errorBaos;
            } else {
                errorStream = new TeeOutputStream(errorStream, errorBaos);
            }
        } else {
            errorBaos = null;
        }

        if (input != null && inputString != null) {
            throw new BuildException("The \"input\" and \"inputstring\" "
                + "attributes cannot both be specified");
        }
        if (input != null) {
            try {
                inputStream = new FileInputStream(input);
            } catch (FileNotFoundException fne) {
                throw new BuildException("Cannot read from " + input, fne);
            }
        } else if (inputString != null) {
            inputStream = new ByteArrayInputStream(inputString.getBytes());
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
    public ExecuteStreamHandler createHandler() throws BuildException {
        createStreams();
        return new PumpStreamHandler(outputStream, errorStream, inputStream);
    }

    /**
     * Pass output sent to System.out to specified output.
     *
     * @param output the data to be output
     */
    protected void handleOutput(String output) {
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
    protected int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
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
    protected void handleFlush(String output) {
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
    protected void handleErrorOutput(String output) {
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
    protected void handleErrorFlush(String output) {
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
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Get the error stream for the redirector
     *
     * @return the redirector's error stream or null if no output
     *         has been configured
     */
    public OutputStream getErrorStream() {
        return errorStream;
    }

    /**
     * Get the input stream for the redirector
     *
     * @return the redirector's input stream or null if no output
     *         has been configured
     */
    public InputStream getInputStream() {
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
    public void complete() throws IOException {
        System.out.flush();
        System.err.flush();

        if (inputStream != null) {
            inputStream.close();
        }

        outputStream.close();

        if (errorStream != outputStream) {
            errorStream.close();
        }

        if (baos != null) {
            setPropertyFromBAOS(baos, outputProperty);
        }
        if (errorBaos != null) {
            setPropertyFromBAOS(errorBaos, errorProperty);
        }
    }
}
