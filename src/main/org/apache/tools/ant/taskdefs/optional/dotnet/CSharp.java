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

/*
 *  build notes
 *  -The reference CD to listen to while editing this file is
 *  nap: Underworld  - Everything, Everything
 */
// ====================================================================
// place in the optional ant tasks package
// but in its own dotnet group
// ====================================================================

package org.apache.tools.ant.taskdefs.optional.dotnet;

// ====================================================================
// imports
// ====================================================================

import java.io.File;

// ====================================================================

/**
 *  Compiles C# source into executables or modules.
 *
 * csc.exe on Windows or mcs on other platforms must be on the execute
 * path, unless another executable or the full path to that executable
 * is specified in the <tt>executable</tt> parameter
 * <p>
 * All parameters are optional: &lt;csc/&gt; should suffice to produce a debug
 * build of all *.cs files. However, naming an <tt>destFile</tt>stops the
 * csc compiler from choosing an output name from random, and
 * allows the dependency checker to determine if the file is out of date.
 * <p>
 *  The task is a directory based task, so attributes like <b>includes="*.cs"
 *  </b> and <b>excludes="broken.cs"</b> can be used to control the files pulled
 *  in. By default, all *.cs files from the project folder down are included in
 *  the command. When this happens the output file -if not specified- is taken
 *  as the first file in the list, which may be somewhat hard to control.
 *  Specifying the output file with <tt>destFile</tt> seems prudent. <p>
 *
 * <p>
 * For more complex source trees, nested <tt>src</tt> elemements can be
 * supplied. When such an element is present, the implicit fileset is ignored.
 * This makes sense, when you think about it :)
 *
 * <p>For historical reasons the pattern
 * <code>**</code><code>/*.cs</code> is preset as includes list and
 * you can not override it with an explicit includes attribute.  Use
 * nested <code>&lt;src&gt;</code> elements instead of the basedir
 * attribute if you need more control.</p>
 *
 * <p>
 * References to external files can be made through the references attribute,
 * or (since Ant1.6), via nested &lt;reference&gt; filesets. With the latter,
 * the timestamps of the references are also used in the dependency
 * checking algorithm.
 * <p>
 *
 * Example
 *
 * <pre>&lt;csc
 *       optimize=&quot;true&quot;
 *       debug=&quot;false&quot;
 *       docFile=&quot;documentation.xml&quot;
 *       warnLevel=&quot;4&quot;
 *       unsafe=&quot;false&quot;
 *       targetType=&quot;exe&quot;
 *       incremental=&quot;false&quot;
 *       mainClass = &quot;MainApp&quot;
 *       destFile=&quot;NetApp.exe&quot;
 *       &gt;
 *           &lt;src dir="src" includes="*.cs" /&gt;
 *       &lt;reference file="${testCSC.dll}" /&gt;
 *       &lt;define name="RELEASE" /&gt;
 *       &lt;define name="DEBUG" if="debug.property"/&gt;
 *       &lt;define name="def3" unless="def3.property"/&gt;
 *    &lt;/csc&gt;
 * </pre>
 *
 *
 * @ant.task    name="csc" category="dotnet"
 * @since Ant 1.3
 */

public class CSharp extends DotnetCompile {

    // CheckStyle:VisibilityModifier OFF - bc
    /**
     *  defines list: RELEASE;WIN32;NO_SANITY_CHECKS;;SOMETHING_ELSE'
     */
    String definitions;


    /**
     *  output XML documentation flag
     */
    private File docFile;

    /**
     *  file alignment; 0 means let the compiler decide
     */
    private int fileAlign = 0;

    /**
     *  use full paths to things
     */
    private boolean fullpaths = false;

    /**
     *  incremental build flag
     */
    private boolean incremental;

    /**
     *  enable unsafe code flag. Clearly set to false by default
     */
    protected boolean unsafe;

    /**
     * A flag that tells the compiler not to read in the compiler
     * settings files 'csc.rsp' in its bin directory and then the local directory
     */
    private boolean noconfig = false;
    // CheckStyle:VisibilityModifier ON


    /**
     *  constructor inits everything and set up the search pattern
     */

    public CSharp() {
        clear();
    }

    /**
     * full cleanup
     */
    public void clear() {
        super.clear();
        docFile = null;
        fileAlign = 0;
        fullpaths = true;
        incremental = false;
        unsafe = false;
        noconfig = false;
        definitions = null;
        setExecutable(isWindows ? "csc" : "mcs");
    }



    /**
     *  file for generated XML documentation
     *
     *@param  f  output file
     */
    public void setDocFile(File f) {
        docFile = f;
    }


    /**
     *  get the argument or null for no argument needed
     *
     *@return    The DocFile Parameter to CSC
     */
    protected String getDocFileParameter() {
        if (docFile != null) {
            return "/doc:" + docFile.toString();
        } else {
            return null;
        }
    }

    /**
     * Set the file alignment.
     * Valid values are 0,512, 1024, 2048, 4096, 8192,
     * and 16384, 0 means 'leave to the compiler'
     * @param fileAlign the value to use.
     */
    public void setFileAlign(int fileAlign) {
        this.fileAlign = fileAlign;
    }

    /**
     *  get the argument or null for no argument needed
     *
     *@return    The OutputFile Parameter to CSC
     */
    protected String getFileAlignParameter() {
        if (fileAlign != 0 && !"mcs".equals(getExecutable())) {
            return "/filealign:" + fileAlign;
        } else {
            return null;
        }
    }


    /**
     * If true, print the full path of files on errors.
     *
     *@param  enabled  The new fullPaths value
     */
    public void setFullPaths(boolean enabled) {
        fullpaths = enabled;
    }


    /**
     *  Gets the fullPathsParameter attribute of the CSharp object
     *
     *@return    The fullPathsParameter value or null if unset
     */
    protected String getFullPathsParameter() {
        return fullpaths ? "/fullpaths" : null;
    }


    /**
     *  set the incremental compilation flag on or off.
     *
     *@param  incremental  on/off flag
     */
    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }


    /**
     *  query the incrementalflag
     *
     *@return    true if incremental compilation is turned on
     */
    public boolean getIncremental() {
        return incremental;
    }


    /**
     *  get the incremental build argument
     *
     *@return    The Incremental Parameter to CSC
     */
    protected String getIncrementalParameter() {
        return "/incremental" + (incremental ? "+" : "-");
    }

    /**
     *  The output file. This is identical to the destFile attribute.
     *
     *@param  params  The new outputFile value
     */
    public void setOutputFile(File params) {
        setDestFile(params);
    }


    /**
     * If true, enables the unsafe keyword.
     *
     *@param  unsafe  The new Unsafe value
     */
    public void setUnsafe(boolean unsafe) {
        this.unsafe = unsafe;
    }


    /**
     *  query the Unsafe attribute
     *
     *@return    The Unsafe value
     */
    public boolean getUnsafe() {
        return this.unsafe;
    }


    /**
     *  get the argument or null for no argument needed
     *
     *@return    The Unsafe Parameter to CSC
     */
    protected String getUnsafeParameter() {
        return unsafe ? "/unsafe" : null;
    }


    /**
     * A flag that tells the compiler not to read in the compiler
     * settings files 'csc.rsp' in its bin directory and then the local directory
     *
     *@param  enabled  The new noConfig value
     */
    public void setNoConfig(boolean enabled) {
        noconfig = enabled;
    }


    /**
     *  Gets the noConfigParameter attribute of the CSharp object
     *
     *@return    The noConfigParameter value
     */
    protected String getNoConfigParameter() {
        return noconfig ? "/noconfig" : null;
    }


    /**
     *  Semicolon separated list of defined constants.
     *
     *@param  params  The new definitions value
     */
    public void setDefinitions(String params) {
        definitions = params;
    }

    /**
     * override the superclasses version of this method (which we call)
     * with a check for a definitions attribute, the contents of which
     * are appended to the list.
     *@return    The Definitions Parameter to CSC
     */
    protected String getDefinitionsParameter() {
        String predecessors = super.getDefinitionsParameter();
        if (notEmpty(definitions)) {
            if (predecessors == null) {
                predecessors = "/define:";
            }
            return  predecessors + definitions;
        } else {
            return predecessors;
        }
    }


    /**
     * add Commands unique to C#.
     * @param command ongoing command
     */
    public void addCompilerSpecificOptions(NetCommand command) {
        command.addArgument(getIncludeDefaultReferencesParameter());
        command.addArgument(getWarnLevelParameter());
        command.addArgument(getDocFileParameter());
        command.addArgument(getFullPathsParameter());
        command.addArgument(getFileAlignParameter());
        command.addArgument(getIncrementalParameter());
        command.addArgument(getNoConfigParameter());
        command.addArgument(getUnsafeParameter());
    }

    // end execute

    /**
     * Returns the delimiter which C# uses to separate references, i.e., a semi colon.
     * @return the delimiter.
     */
    public String getReferenceDelimiter() {
        return ";";
    }


    /**
     * This method indicates the filename extension for C# files.
     * @return the file extension for C#, i.e., "cs" (without the dot).
     */
    public String getFileExtension() {
        return "cs";
    }

    /**
     * Build a C# style parameter.
     * @param command the command.
     * @param resource the resource.
     */
    protected void createResourceParameter(
        NetCommand command, DotnetResource resource) {
        resource.getParameters(getProject(), command, true);
    }

}

