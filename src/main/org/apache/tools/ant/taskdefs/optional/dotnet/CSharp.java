/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
 *  The task will only work on win2K until other platforms support
 *  csc.exe or an equivalent. CSC.exe must be on the execute path too. <p>
 *
 *  All parameters are optional: &lt;csc/&gt; should suffice to produce a debug
 *  build of all *.cs files. References to external files do require explicit
 *  enumeration, so are one of the first attributes to consider adding. <p>
 *
 *  The task is a directory based task, so attributes like <b>includes="*.cs"
 *  </b> and <b>excludes="broken.cs"</b> can be used to control the files pulled
 *  in. By default, all *.cs files from the project folder down are included in
 *  the command. When this happens the output file -if not specified- is taken
 *  as the first file in the list, which may be somewhat hard to control.
 *  Specifying the output file with <b>'outfile'</b> seems prudent. <p>
 *
 *  <p>
 *
 *  TODO
 *  <ol>
 *    <li> is incremental build still broken in beta-1?
 *    <li> is Win32Icon broken?
 *    <li> all the missing options
 *  </ol>
 *  <p>
 *
 *  History
 *  <Table>
 *
 *    <tr>
 *
 *      <td>
 *        0.3
 *      </td>
 *
 *      <td>
 *        Beta 1 edition
 *      </td>
 *
 *      <td>
 *        To avoid having to remember which assemblies to include, the task
 *        automatically refers to the main dotnet libraries in Beta1.
 *      </tr>
 *
 *      <tr>
 *
 *        <td>
 *          0.2
 *        </td>
 *
 *        <td>
 *          Slightly different
 *        </td>
 *
 *        <td>
 *          Split command execution to a separate class;
 *        </tr>
 *
 *        <tr>
 *
 *          <td>
 *            0.1
 *          </td>
 *
 *          <td>
 *            "I can't believe it's so rudimentary"
 *          </td>
 *
 *          <td>
 *            First pass; minimal builds only support;
 *          </tr>
 *
 *        </table>
 *
 *
 * @author      Steve Loughran steve_l@iseran.com
 * @version     0.5
 * @ant.task    name="csc" category="dotnet"
 * @since Ant 1.3
 */

public class CSharp extends DotnetCompile {

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
        setExecutable("csc");
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
        if (fileAlign != 0) {
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
     *@return    true iff incremental compilation is turned on
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
     * how does C# separate references? with a semi colon
     */
    public String getReferenceDelimiter() {
        return ";";
    }


    /**
     * extension is '.cs'
     * @return
     */
    public String getFileExtension() {
        return "cs";
    }

    /**
     * from a resource, get the resource param string
     * @param resource
     * @return a string containing the resource param, or a null string
     * to conditionally exclude a resource.
     */
    protected String createResourceParameter(DotnetResource resource) {
        return resource.getCSharpStyleParameter();
    }

}

