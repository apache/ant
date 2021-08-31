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
package org.apache.tools.ant.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Redirector;
import org.apache.tools.ant.util.MergingMapper;

/**
 * Element representation of a <code>Redirector</code>.
 * @since Ant 1.6.2
 */
public class RedirectorElement extends DataType {

    /**
     * Whether the input mapper was set via <code>setOutput</code>.
     */
    private boolean usingInput = false;

    /**
     * Whether the output mapper was set via <code>setOutput</code>.
     */
    private boolean usingOutput = false;

    /**
     * Whether the error mapper was set via <code>setError</code>.
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

    /** Flag which indicates that output should be always sent to the log */
    private Boolean alwaysLog;

    /** Flag which indicates whether files should be created even if empty. */
    private Boolean createEmptyFiles;

    /** Input file mapper. */
    private Mapper inputMapper;

    /** Output file mapper. */
    private Mapper outputMapper;

    /** Error file mapper. */
    private Mapper errorMapper;

    /** input filter chains. */
    private Vector<FilterChain> inputFilterChains = new Vector<>();

    /** output filter chains. */
    private Vector<FilterChain> outputFilterChains = new Vector<>();

    /** error filter chains. */
    private Vector<FilterChain> errorFilterChains = new Vector<>();

    /** The output encoding */
    private String outputEncoding;

    /** The error encoding */
    private String errorEncoding;

    /** The input encoding */
    private String inputEncoding;

    /** whether to log the inputstring */
    private Boolean logInputString;

    /** Is the output binary or can we safely split it into lines? */
    private boolean outputIsBinary = false;

    /**
     * Add the input file mapper.
     * @param inputMapper   <code>Mapper</code>.
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
        setChecked(false);
        this.inputMapper = inputMapper;
    }

    /**
     * Add the output file mapper.
     * @param outputMapper   <code>Mapper</code>.
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
        setChecked(false);
        this.outputMapper = outputMapper;
    }

    /**
     * Add the error file mapper.
     * @param errorMapper   <code>Mapper</code>.
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
        setChecked(false);
        this.errorMapper = errorMapper;
    }

    /**
     * Make this instance in effect a reference to another instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     * @param r the reference to use.
     * @throws BuildException on error.
     */
    public void setRefid(Reference r) throws BuildException {
        if (usingInput
            || usingOutput
            || usingError
            || inputString != null
            || logError != null
            || append != null
            || createEmptyFiles != null
            || inputEncoding != null
            || outputEncoding != null
            || errorEncoding != null
            || outputProperty != null
            || errorProperty != null
            || logInputString != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Set the input to use for the task.
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
     * Set whether to include the value of the input string in log messages.
     * Defaults to true.
     * @param logInputString true or false.
     * @since Ant 1.7
     */
    public void setLogInputString(boolean logInputString) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.logInputString = logInputString ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * File the output of the process is redirected to. If error is not
     * redirected, it too will appear in the output.
     *
     * @param out the file to which output stream is written.
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
     * @param outputEncoding   <code>String</code>.
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
     * @param errorEncoding   <code>String</code>.
     */
    public void setErrorEncoding(String errorEncoding) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.errorEncoding = errorEncoding;
    }

    /**
     * Set the input encoding.
     * @param inputEncoding   <code>String</code>.
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
     * Ant log.
     * @param logError if true the standard error is sent to the Ant log system
     *        and not sent to output.
     */
    public void setLogError(boolean logError) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.logError = ((logError) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Set the file to which standard error is to be redirected.
     * @param error the file to which error is to be written.
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
        this.append = ((append) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * If true, (error and non-error) output will be "teed", redirected
     * as specified while being sent to Ant's logging mechanism as if no
     * redirection had taken place.  Defaults to false.
     * @param alwaysLog <code>boolean</code>
     * @since Ant 1.6.3
     */
    public void setAlwaysLog(boolean alwaysLog) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.alwaysLog = ((alwaysLog) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Whether output and error files should be created even when empty.
     * Defaults to true.
     * @param createEmptyFiles <code>boolean</code>.
     */
    public void setCreateEmptyFiles(boolean createEmptyFiles) {
        if (isReference()) {
            throw tooManyAttributes();
        }
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
     * Create a nested input <code>FilterChain</code>.
     * @return <code>FilterChain</code>.
     */
    public FilterChain createInputFilterChain() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        FilterChain result = new FilterChain();
        result.setProject(getProject());
        inputFilterChains.add(result);
        setChecked(false);
        return result;
    }

    /**
     * Create a nested output <code>FilterChain</code>.
     * @return <code>FilterChain</code>.
     */
    public FilterChain createOutputFilterChain() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        FilterChain result = new FilterChain();
        result.setProject(getProject());
        outputFilterChains.add(result);
        setChecked(false);
        return result;
    }

    /**
     * Create a nested error <code>FilterChain</code>.
     * @return <code>FilterChain</code>.
     */
    public FilterChain createErrorFilterChain() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        FilterChain result = new FilterChain();
        result.setProject(getProject());
        errorFilterChains.add(result);
        setChecked(false);
        return result;
    }

    /**
     * Whether to consider the output created by the process binary.
     *
     * <p>Binary output will not be split into lines which may make
     * error and normal output look mixed up when they get written to
     * the same stream.</p>
     * @param b boolean
     * @since 1.9.4
     */
    public void setBinaryOutput(boolean b) {
        outputIsBinary = b;
    }

    /**
     * Configure the specified <code>Redirector</code>.
     * @param redirector   <code>Redirector</code>.
     */
    public void configure(Redirector redirector) {
        configure(redirector, null);
    }

    /**
     * Configure the specified <code>Redirector</code>
     * for the specified sourcefile.
     * @param redirector   <code>Redirector</code>.
     * @param sourcefile   <code>String</code>.
     */
    public void configure(Redirector redirector, String sourcefile) {
        if (isReference()) {
            getRef().configure(redirector, sourcefile);
            return;
        }
        dieOnCircularReference();
        if (alwaysLog != null) {
            redirector.setAlwaysLog(alwaysLog);
        }
        if (logError != null) {
            redirector.setLogError(logError);
        }
        if (append != null) {
            redirector.setAppend(append);
        }
        if (createEmptyFiles != null) {
            redirector.setCreateEmptyFiles(createEmptyFiles);
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
        if (logInputString != null) {
            redirector.setLogInputString(logInputString);
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
        if (!inputFilterChains.isEmpty()) {
            redirector.setInputFilterChains(inputFilterChains);
        }
        if (!outputFilterChains.isEmpty()) {
            redirector.setOutputFilterChains(outputFilterChains);
        }
        if (!errorFilterChains.isEmpty()) {
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
        redirector.setBinaryOutput(outputIsBinary);
    }

    /**
     * Create a merge mapper pointing to the specified destination file.
     * @param destfile   <code>File</code>
     * @return <code>Mapper</code>.
     */
    protected Mapper createMergeMapper(File destfile) {
        Mapper result = new Mapper(getProject());
        result.setClassname(MergingMapper.class.getName());
        result.setTo(destfile.getAbsolutePath());
        return result;
    }

    /**
     * Return a <code>File[]</code> from the specified set of filenames.
     * @param name   <code>String[]</code>
     * @return <code>File[]</code>.
     */
    protected File[] toFileArray(String[] name) {
        if (name == null) {
            return null;
        }
        //remove any null elements
        ArrayList<File> list = new ArrayList<>(name.length);
        for (String n : name) {
            if (n != null) {
                list.add(getProject().resolveFile(n));
            }
        }
        return list.toArray(new File[0]);
    }

    /**
     * Overrides the version of DataType to recurse on all DataType
     * child elements that may have been added.
     * @param stk the stack of data types to use (recursively).
     * @param p   the project to use to dereference the references.
     * @throws BuildException on error.
     */
    @Override
    protected void dieOnCircularReference(Stack<Object> stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            for (Mapper m : Arrays.asList(inputMapper, outputMapper, errorMapper)) {
                if (m != null) {
                    stk.push(m);
                    m.dieOnCircularReference(stk, p);
                    stk.pop();
                }
            }
            final List<? extends List<FilterChain>> filterChainLists = Arrays
                    .<List<FilterChain>> asList(inputFilterChains, outputFilterChains,
                            errorFilterChains);
            for (List<FilterChain> filterChains : filterChainLists) {
                if (filterChains != null) {
                    for (FilterChain fc : filterChains) {
                        pushAndInvokeCircularReferenceCheck(fc, stk, p);
                    }
                }
            }
            setChecked(true);
        }
    }

    /**
     * Perform the check for circular references, returning the
     * referenced RedirectorElement.
     * @return the referenced RedirectorElement.
     */
    private RedirectorElement getRef() {
        return getCheckedRef(RedirectorElement.class);
    }

}
