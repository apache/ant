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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
    private File out;
    private File error;
    private File input;

    private boolean logError = false;
    private ByteArrayOutputStream baos = null;
    private ByteArrayOutputStream errorBaos = null;
    private String outputProperty;
    private String errorProperty;
    private String inputString;
    private boolean append = false;
    
    private Task managingTask;

    private OutputStream outputStream = null;
    private OutputStream errorStream = null;
    private InputStream inputStream = null; 
    private PrintStream outPrintStream = null;
    private PrintStream errorPrintStream = null;
    
    public Redirector(Task managingTask) {
        this.managingTask = managingTask;
    }
    
    /**
     * Set the input to use for the task
     */
    public void setInput(File input) {
        this.input = input;
    }

    public void setInputString(String inputString) {
        this.inputString = inputString;
    }
        
    
    /**
     * File the output of the process is redirected to. If error is not 
     * redirected, it too will appear in the output
     */
    public void setOutput(File out) {
        this.out = out;
    }

    /**
     * Controls whether error output of exec is logged. This is only useful
     * when output is being redirected and error output is desired in the
     * Ant log
     */
    public void setLogError(boolean logError) {
        this.logError = logError;
    }
    
    /**
     * File the error stream of the process is redirected to.
     *
     */
    public void setError(File error) {
        this.error = error;
    }

    /**
     * Property name whose value should be set to the output of
     * the process.
     */
    public void setOutputProperty(String outputProperty) {
        this.outputProperty = outputProperty;
    }

    /**
     * Whether output should be appended to or overwrite an existing file.
     * Defaults to false.
     *
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * Property name whose value should be set to the error of
     * the process.
     *
     */
    public void setErrorProperty(String errorProperty) {
        this.errorProperty = errorProperty;
    }

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
     */
    public ExecuteStreamHandler createHandler() throws BuildException {
        createStreams();
        return new PumpStreamHandler(outputStream, errorStream, inputStream);
    }
   
    /**
     * Pass output sent to System.out to specified output file.
     *
     */
    protected void handleOutput(String line) {
        if (outPrintStream == null) {
            outPrintStream = new PrintStream(outputStream);
        }
        outPrintStream.println(line);
    }
    
    /**
     * Pass output sent to System.out to specified output file.
     *
     */
    protected void handleFlush(String line) {
        if (outPrintStream == null) {
            outPrintStream = new PrintStream(outputStream);
        }
        outPrintStream.print(line);
    }
    
    /**
     * Pass output sent to System.err to specified output file.
     *
     */
    protected void handleErrorOutput(String line) {
        if (errorPrintStream == null) {
            errorPrintStream = new PrintStream(errorStream);
        }
        errorPrintStream.println(line);
    }
    
    /**
     * Pass output sent to System.err to specified output file.
     *
     */
    protected void handleErrorFlush(String line) {
        if (errorPrintStream == null) {
            errorPrintStream = new PrintStream(errorStream);
        }
        errorPrintStream.print(line);
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
    
    public OutputStream getErrorStream() {
        return errorStream;
    }

    
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
