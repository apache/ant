/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.tools.ant.types;

import java.io.File;
import java.util.Vector;
import java.util.ArrayList;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Redirector;
import org.apache.tools.ant.types.DataType;

/**
 * Element representation of a <CODE>Redirector</CODE>.
 */
public class RedirectorElement extends DataType {

    /**
     * Whether the input mapper was set via <CODE>setOutput</CODE>.
     */
    private boolean usingInput = false;

    /**
     * Whether the output mapper was set via <CODE>setOutput</CODE>.
     */
    private boolean usingOutput = false;

    /**
     * Whether the error mapper was set via <CODE>setError</CODE>.
     */
    private boolean usingError = false;

    /**
      * Indicates if standard error should be logged to Ant's log system
      * rather than the output. This has no effect if standard error is
      * redirected to a file or property.
      */
    private Boolean logError;

    /** The name of the property into which output is to be stored */
    private String outputProperty;

    /** The name of the property into which error output is to be stored */
    private String errorProperty;

    /** String from which input is taken */
    private String inputString;

    /** Flag which indicates if error and output files are to be appended. */
    private Boolean append;

    /** Flag which indicates whether files should be created even if empty. */
    private Boolean createEmptyFiles;

    /** Input file mapper. */
    private Mapper inputMapper;

    /** Output file mapper. */
    private Mapper outputMapper;

    /** Error file mapper. */
    private Mapper errorMapper;

    /** input filter chains. */
    private Vector inputFilterChains = new Vector();

    /** output filter chains. */
    private Vector outputFilterChains = new Vector();

    /** error filter chains. */
    private Vector errorFilterChains = new Vector();

    /** The output encoding */
    private String outputEncoding;

    /** The error encoding */
    private String errorEncoding;

    /** The input encoding */
    private String inputEncoding;

    /**
     * Add the input file mapper.
     * @param inputMapper   <CODE>Mapper</CODE>.
     */
    public void addConfiguredInputMapper(Mapper inputMapper) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.inputMapper != null) {
            if (usingInput) {
                throw new BuildException("attribute \"input\"" 
                    + " cannot coexist with a nested <inputmapper>");
            } else {
                throw new BuildException("Cannot have > 1 <inputmapper>");
            }
        }
        this.inputMapper = inputMapper;
    }

    /**
     * Add the output file mapper.
     * @param outputMapper   <CODE>Mapper</CODE>.
     */
    public void addConfiguredOutputMapper(Mapper outputMapper) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.outputMapper != null) {
            if (usingOutput) {
                throw new BuildException("attribute \"output\""
                    + " cannot coexist with a nested <outputmapper>");
            } else {
                throw new BuildException("Cannot have > 1 <outputmapper>");
            }
        }
        this.outputMapper = outputMapper;
    }

    /**
     * Add the error file mapper.
     * @param errorMapper   <CODE>Mapper</CODE>.
     */
    public void addConfiguredErrorMapper(Mapper errorMapper) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.errorMapper != null) {
            if (usingError) {
                throw new BuildException("attribute \"error\"" 
                    + " cannot coexist with a nested <errormapper>");
            } else {
                throw new BuildException("Cannot have > 1 <errormapper>");
            }
        }
        this.errorMapper = errorMapper;
    }

    /**
     * Makes this instance in effect a reference to another instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     */
    public void setRefid(Reference r) throws BuildException {
        if (usingInput
            || usingOutput
            || usingError
            || inputString != null
            || logError != null
            || append != null
            || outputProperty != null
            || errorProperty != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Set the input to use for the task
     * @param input the file from which input is read.
     */
    public void setInput(File input) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (inputString != null) {
            throw new BuildException("The \"input\" and \"inputstring\" "
                + "attributes cannot both be specified");
        }
        usingInput = true;
        inputMapper = createMergeMapper(input);
    }

    /**
     * Set the string to use as input
     * @param inputString the string which is used as the input source
     */
    public void setInputString(String inputString) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (usingInput) {
            throw new BuildException("The \"input\" and \"inputstring\" "
                + "attributes cannot both be specified");
        }
        this.inputString = inputString;
    }


    /**
     * File the output of the process is redirected to. If error is not
     * redirected, it too will appear in the output
     *
     * @param out the file to which output stream is written
     */
    public void setOutput(File out) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (out == null) {
            throw new IllegalArgumentException("output file specified as null");
        }
        usingOutput = true;
        outputMapper = createMergeMapper(out);
    }

    /**
     * Set the output encoding.
     * @param outputEncoding   <CODE>String</CODE>.
     */
    public void setOutputEncoding(String outputEncoding) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.outputEncoding = outputEncoding;
    }

    /**
     * Set the error encoding.
     *
     * @param errorEncoding   <CODE>String</CODE>.
     */
    public void setErrorEncoding(String errorEncoding) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.errorEncoding = errorEncoding;
    }

    /**
     * Set the input encoding.
     * @param inputEncoding   <CODE>String</CODE>.
     */
    public void setInputEncoding(String inputEncoding) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.inputEncoding = inputEncoding;
    }

    /**
     * Controls whether error output of exec is logged. This is only useful
     * when output is being redirected and error output is desired in the
     * Ant log
     * @param logError if true the standard error is sent to the Ant log system
     *        and not sent to output.
     */
    public void setLogError(boolean logError) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        //pre JDK 1.4 compatible
        this.logError = ((logError) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Set the file to which standard error is to be redirected.
     * @param error the file to which error is to be written
     */
    public void setError(File error) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (error == null) {
            throw new IllegalArgumentException("error file specified as null");
        }
        usingError = true;
        errorMapper = createMergeMapper(error);
    }

    /**
     * Property name whose value should be set to the output of
     * the process.
     * @param outputProperty the name of the property to be set with the
     *        task's output.
     */
    public void setOutputProperty(String outputProperty) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.outputProperty = outputProperty;
    }

    /**
     * Whether output should be appended to or overwrite an existing file.
     * Defaults to false.
     * @param append if true output and error streams are appended to their
     *        respective files, if specified.
     */
    public void setAppend(boolean append) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        //pre JDK 1.4 compatible
        this.append = ((append) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Whether output and error files should be created even when empty.
     * Defaults to true.
     * @param createEmptyFiles <CODE>boolean</CODE>.
     */
    public void setCreateEmptyFiles(boolean createEmptyFiles) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        //pre JDK 1.4 compatible
        this.createEmptyFiles = ((createEmptyFiles)
            ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Property name whose value should be set to the error of
     * the process.
     * @param errorProperty the name of the property to be set
     *        with the error output.
     */
    public void setErrorProperty(String errorProperty) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.errorProperty = errorProperty;
    }

    /**
     * Create a nested input <CODE>FilterChain</CODE>.
     * @return <CODE>FilterChain</CODE>.
     */
    public FilterChain createInputFilterChain() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        FilterChain result = new FilterChain();
        result.setProject(getProject());
        inputFilterChains.add(result);
        return result;
    }

    /**
     * Create a nested output <CODE>FilterChain</CODE>.
     * @return <CODE>FilterChain</CODE>.
     */
    public FilterChain createOutputFilterChain() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        FilterChain result = new FilterChain();
        result.setProject(getProject());
        outputFilterChains.add(result);
        return result;
    }

    /**
     * Create a nested error <CODE>FilterChain</CODE>.
     * @return <CODE>FilterChain</CODE>.
     */
    public FilterChain createErrorFilterChain() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        FilterChain result = new FilterChain();
        result.setProject(getProject());
        errorFilterChains.add(result);
        return result;
    }

    /**
     * Configure the specified <CODE>Redirector</CODE>.
     * @param redirector   <CODE>Redirector</CODE>.
     */
    public void configure(Redirector redirector) {
        configure(redirector, null);
    }

    /**
     * Configure the specified <CODE>Redirector</CODE>
     * for the specified sourcefile.
     * @param redirector   <CODE>Redirector</CODE>.
     * @param sourcefile   <CODE>String</CODE>.
     */
    public void configure(Redirector redirector, String sourcefile) {
        if (logError != null) {
            redirector.setLogError(logError.booleanValue());
        }
        if (append != null) {
            redirector.setAppend(append.booleanValue());
        }
        if (createEmptyFiles != null) {
            redirector.setCreateEmptyFiles(createEmptyFiles.booleanValue());
        }
        if (outputProperty != null) {
            redirector.setOutputProperty(outputProperty);
        }
        if (errorProperty != null) {
            redirector.setErrorProperty(errorProperty);
        }
        if (inputString != null) {
            redirector.setInputString(inputString);
        }
        if (inputMapper != null) {
            String[] inputTargets = null;
            try {
                inputTargets =
                    inputMapper.getImplementation().mapFileName(sourcefile);
            } catch (NullPointerException enPeaEx) {
                if (sourcefile != null) {
                    throw enPeaEx;
                }
            }
            if (inputTargets != null && inputTargets.length > 0) {
                redirector.setInput(toFileArray(inputTargets));
            }
        }
        if (outputMapper != null) {
            String[] outputTargets = null;
            try {
                outputTargets =
                    outputMapper.getImplementation().mapFileName(sourcefile);
            } catch (NullPointerException enPeaEx) {
                if (sourcefile != null) {
                    throw enPeaEx;
                }
            }
            if (outputTargets != null && outputTargets.length > 0) {
                redirector.setOutput(toFileArray(outputTargets));
            }
        }
        if (errorMapper != null) {
            String[] errorTargets = null;
            try {
                errorTargets =
                    errorMapper.getImplementation().mapFileName(sourcefile);
            } catch (NullPointerException enPeaEx) {
                if (sourcefile != null) {
                    throw enPeaEx;
                }
            }
            if (errorTargets != null && errorTargets.length > 0) {
                redirector.setError(toFileArray(errorTargets));
            }
        }
        if (inputFilterChains.size() > 0) {
            redirector.setInputFilterChains(inputFilterChains);
        }
        if (outputFilterChains.size() > 0) {
            redirector.setOutputFilterChains(outputFilterChains);
        }
        if (errorFilterChains.size() > 0) {
            redirector.setErrorFilterChains(errorFilterChains);
        }
        if (inputEncoding != null) {
            redirector.setInputEncoding(inputEncoding);
        }
        if (outputEncoding != null) {
            redirector.setOutputEncoding(outputEncoding);
        }
        if (errorEncoding != null) {
            redirector.setErrorEncoding(errorEncoding);
        }
    }

    /**
     * Create a merge mapper pointing to the specified destination file.
     * @param destfile   <CODE>File</CODE>
     * @return <CODE>Mapper</CODE>.
     */
    protected Mapper createMergeMapper(File destfile) {
        Mapper result = new Mapper(getProject());
        result.setClassname(
            org.apache.tools.ant.util.MergingMapper.class.getName());
        result.setTo(destfile.getAbsolutePath());
        return result;
    }

    /**
     * Return a <CODE>File[]</CODE> from the specified set of filenames.
     * @param name   <CODE>String[]</CODE>
     * @return <CODE>File[]</CODE>.
     */
    protected File[] toFileArray(String[] name) {
        if (name == null) {
            return null;
        }
        //remove any null elements
        ArrayList list = new ArrayList(name.length);
        for (int i = 0; i < name.length; i++) {
            if (name[i] != null) {
                list.add(getProject().resolveFile(name[i]));
            }
        }
        return (File[])(list.toArray(new File[list.size()]));
    }

}
