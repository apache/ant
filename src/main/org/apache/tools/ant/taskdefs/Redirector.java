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
package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
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
     * The file receiveing standard output. Will also receive standard error
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
                try {
                    outputStream 
                        = new FileOutputStream(out.getAbsolutePath(), append);
                    managingTask.log("Output redirected to " + out, 
                                     Project.MSG_VERBOSE);
                } catch (FileNotFoundException fne) {
                    throw new BuildException("Cannot write to " + out, fne);
                } catch (IOException ioe) {
                    throw new BuildException("Cannot write to " + out, ioe);
                }
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
            try {
                errorStream 
                    = new FileOutputStream(error.getAbsolutePath(), append);
                managingTask.log("Error redirected to " + error, 
                    Project.MSG_VERBOSE);
            } catch (FileNotFoundException fne) {
                throw new BuildException("Cannot write to " + error, fne);
            } catch (IOException ioe) {
                throw new BuildException("Cannot write to " + error, ioe);
            }
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
     * @param line the data to be output
     */
    protected void handleOutput(String line) {
        if (outPrintStream == null) {
            outPrintStream = new PrintStream(outputStream);
        }
        outPrintStream.println(line);
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
     * @param line the data being flushed.
     */
    protected void handleFlush(String line) {
        if (outPrintStream == null) {
            outPrintStream = new PrintStream(outputStream);
        }
        outPrintStream.print(line);
    }
    
    /**
     * Process error output
     *
     * @param line the error output data.
     */
    protected void handleErrorOutput(String line) {
        if (errorPrintStream == null) {
            errorPrintStream = new PrintStream(errorStream);
        }
        errorPrintStream.println(line);
    }
    
    /**
     * Handle a flush operation on the error stream
     *
     * @param line the error information being flushed.
     */
    protected void handleErrorFlush(String line) {
        if (errorPrintStream == null) {
            errorPrintStream = new PrintStream(errorStream);
        }
        errorPrintStream.print(line);
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
     * This opertaion will close any streams and create any specified
     * property values.
     *
     * @throws IOException if the outptu properties cannot be read from their
     * output streams.
     */
    public void complete() throws IOException {
        System.out.flush();
        System.err.flush();
        
        if (inputStream != null) {
            inputStream.close();
        }
        outputStream.close();
        errorStream.close();
        
        if (baos != null) {
            setPropertyFromBAOS(baos, outputProperty);
        }
        if (errorBaos != null) {
            setPropertyFromBAOS(errorBaos, errorProperty);
        }
    }
}
