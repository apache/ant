/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;

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
 *@author      Steve Loughran steve_l@iseran.com
 *@version     0.5
 *@ant.task    name="csc" category="dotnet"
 * @since Ant 1.3
 */

public class CSharp
         extends MatchingTask {

    /**
     *  Name of the executable. The .exe suffix is deliberately not included in
     *  anticipation of the unix version
     */
    private static final String csc_exe_name = "csc";

    /**
     *  what is the file extension we search on?
     */
    private static final String csc_file_ext = "cs";

    /**
     *  derive the search pattern from the extension
     */
    private static final String csc_file_pattern = "**/*." + csc_file_ext;

    /**
     *  list of reference classes. (pretty much a classpath equivalent)
     */
    private String references;

    /**
     *  flag to enable automatic reference inclusion
     */
    private boolean includeDefaultReferences;

    /**
     *  incremental build flag
     */
    private boolean incremental;

    /**
     *  output XML documentation flag
     */
    private File docFile;

    /**
     *  icon for incorporation into apps
     */
    private File win32icon;

    /**
     *  icon for incorporation into apps
     */
    private File win32res;

    /**
     * A flag that tells the compiler not to read in the compiler 
     * settings files 'csc.rsp' in its bin directory and then the local directory
     */
    private boolean noconfig = false;

    /**
     *  use full paths to things
     */
    private boolean fullpaths = false;

    /**
     *  output file. If not supplied this is derived from the source file
     */
    private File outputFile;

    /**
     *  flag to control action on execution trouble
     */
    private boolean failOnError;

    /**
     *  using the path approach didnt work as it could not handle the implicit
     *  execution path. Perhaps that could be extracted from the runtime and
     *  then the path approach would be viable
     */
    private Path referenceFiles;

    /**
     *  optimise flag
     */
    private boolean optimize;

    /**
     *  file alignment; 0 means let the compiler decide
     */
    private int fileAlign = 0;

    /**
     *  Fix C# reference inclusion. C# is really dumb in how it handles
     *  inclusion. You have to list every 'assembly' -read DLL that is imported.
     *  So already you are making a platform assumption -shared libraries have a
     *  .dll;"+ extension and the poor developer has to know every library which
     *  is included why the compiler cant find classes on the path or in a
     *  directory, is a mystery. To reduce the need to be explicit, here is a
     *  long list of the core libraries used in Beta-1 of .NET ommitting the
     *  blatantly non portable (MS.win32.interop) and the .designer libraries.
     *  (ripping out Com was tempting) Casing is chosen to match that of the
     *  file system <i>exactly</i> so may work on a unix box too. there is no
     *  need to reference mscorlib.dll, cos it is always there
     */

    protected static final String DEFAULT_REFERENCE_LIST =
            "Accessibility.dll;" +
            "cscompmgd.dll;" +
            "CustomMarshalers.dll;" +
            "Mscorcfg.dll;" +
            "System.Configuration.Install.dll;" +
            "System.Data.dll;" +
            "System.Design.dll;" +
            "System.DirectoryServices.dll;" +
            "System.EnterpriseServices.dll;" +
            "System.dll;" +
            "System.Drawing.Design.dll;" +
            "System.Drawing.dll;" +
            "System.Management.dll;" +
            "System.Messaging.dll;" +
            "System.Runtime.Remoting.dll;" +
            "System.Runtime.Serialization.Formatters.Soap.dll;" +
            "System.Security.dll;" +
            "System.ServiceProcess.dll;" +
            "System.Web.dll;" +
            "System.Web.RegularExpressions.dll;" +
            "System.Web.Services.dll;" +
            "System.Windows.Forms.dll;" +
            "System.XML.dll;";

    /**
     *  debug flag. Controls generation of debug information.
     */
    protected boolean debug;

    /**
     *  warning level: 0-4, with 4 being most verbose
     */
    private int warnLevel;

    /**
     *  enable unsafe code flag. Clearly set to false by default
     */
    protected boolean unsafe;

    /**
     *  main class (or null for automatic choice)
     */
    protected String mainClass;

    /**
     *  any extra command options?
     */
    protected String extraOptions;

    /**
     *  source directory upon which the search pattern is applied
     */
    private File srcDir;

    /**
     *  destination directory (null means use the source directory) NB: this is
     *  currently not used
     */
    private File destDir;

    /**
     *  type of target. Should be one of exe|library|module|winexe|(null)
     *  default is exe; the actual value (if not null) is fed to the command
     *  line. <br>
     *  See /target
     */
    protected String targetType;

    /**
     *  utf out flag
     */

    protected boolean utf8output = false;

    /**
     *  defines list something like 'RELEASE;WIN32;NO_SANITY_CHECKS;;SOMETHING_ELSE'
     */
    String definitions;

    /**
     *  list of extra modules to refer to
     */
    String additionalModules;


    /**
     *  constructor inits everything and set up the search pattern
     */

    public CSharp() {
        Clear();
        setIncludes(csc_file_pattern);
    }


    /**
     * Semicolon separated list of DLLs to refer to.
     *
     *@param  s  The new References value
     */
    public void setReferences(String s) {
        references = s;
    }


    /**
     *  get the reference string or null for no argument needed
     *
     *@return    The References Parameter to CSC
     */
    protected String getReferencesParameter() {
        //bail on no references
        if (notEmpty(references)) {
            return "/reference:" + references;
        } else {
            return null;
        }
    }

    /**
     * Path of references to include.
     * Wildcards should work.
     *
     *@param  path  another path to append
     */
    public void setReferenceFiles(Path path) {
        //demand create pathlist
        if (referenceFiles == null) {
            referenceFiles = new Path(this.project);
        }
        referenceFiles.append(path);
    }


    /**
     *  turn the path list into a list of files and a /references argument
     *
     *@return    null or a string of references.
     */
    protected String getReferenceFilesParameter() {
        //bail on no references
        if (references == null) {
            return null;
        }
        //iterate through the ref list & generate an entry for each
        //or just rely on the fact that the toString operator does this, but
        //noting that the separator is ';' on windows, ':' on unix
        String refpath = references.toString();

        //bail on no references listed
        if (refpath.length() == 0) {
            return null;
        }

        StringBuffer s = new StringBuffer("/reference:");
        s.append(refpath);
        return new String(s);
    }


    /**
     *  get default reference list
     *
     *@return    null or a string of references.
     */
    protected String getDefaultReferenceParameter() {
        if (includeDefaultReferences) {
            StringBuffer s = new StringBuffer("/reference:");
            s.append(DEFAULT_REFERENCE_LIST);
            return new String(s);
        } else {
            return null;
        }
    }


    /**
     * If true, automatically includes the common assemblies
     * in dotnet, and tells the compiler to link in mscore.dll.
     *
     *  set the automatic reference inclusion flag on or off this flag controls
     *  the string of references and the /nostdlib option in CSC
     *
     *@param  f  on/off flag
     */
    public void setIncludeDefaultReferences(boolean f) {
        includeDefaultReferences = f;
    }


    /**
     *  query automatic reference inclusion flag
     *
     *@return    true if flag is turned on
     */
    public boolean getIncludeDefaultReferences() {
        return includeDefaultReferences;
    }


    /**
     *  get the include default references flag or null for no argument needed
     *
     *@return    The Parameter to CSC
     */
    protected String getIncludeDefaultReferencesParameter() {
        return "/nostdlib" + (includeDefaultReferences ? "-" : "+");
    }



    /**
     * If true, enables optimization flag.
     *
     *@param  f  on/off flag
     */
    public void setOptimize(boolean f) {
        optimize = f;
    }


    /**
     *  query the optimise flag
     *
     *@return    true if optimise is turned on
     */
    public boolean getOptimize() {
        return optimize;
    }


    /**
     *  get the optimise flag or null for no argument needed
     *
     *@return    The Optimize Parameter to CSC
     */
    protected String getOptimizeParameter() {
        return "/optimize" + (optimize ? "+" : "-");
    }


    /**
     *  set the incremental compilation flag on or off.
     *
     *@param  f  on/off flag
     */
    public void setIncremental(boolean f) {
        incremental = f;
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
     *  set the debug flag on or off.
     *
     *@param  f  on/off flag
     */
    public void setDebug(boolean f) {
        debug = f;
    }


    /**
     *  query the debug flag
     *
     *@return    true if debug is turned on
     */
    public boolean getDebug() {
        return debug;
    }


    /**
     *  get the debug switch argument
     *
     *@return    The Debug Parameter to CSC
     */
    protected String getDebugParameter() {
        return "/debug" + (debug ? "+" : "-");
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
     * Level of warning currently between 1 and 4
     * with 4 being the strictest.
     *
     *@param  warnLevel  warn level -see .net docs for valid range (probably
     *      0-4)
     */
    public void setWarnLevel(int warnLevel) {
        this.warnLevel = warnLevel;
    }


    /**
     *  query warn level
     *
     *@return    current value
     */
    public int getWarnLevel() {
        return warnLevel;
    }


    /**
     *  get the warn level switch
     *
     *@return    The WarnLevel Parameter to CSC
     */
    protected String getWarnLevelParameter() {
        return "/warn:" + warnLevel;
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
     *  Sets the name of main class for executables.
     *
     *@param  mainClass  The new MainClass value
     */
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }


    /**
     *  Gets the MainClass attribute
     *
     *@return    The MainClass value
     */
    public String getMainClass() {
        return this.mainClass;
    }


    /**
     *  get the /main argument or null for no argument needed
     *
     *@return    The MainClass Parameter to CSC
     */
    protected String getMainClassParameter() {
        if (mainClass != null && mainClass.length() != 0) {
            return "/main:" + mainClass;
        } else {
            return null;
        }
    }


    /**
     * Any extra options which are not explicitly supported
     * by this task.
     *
     *@param  extraOptions  The new ExtraOptions value
     */
    public void setExtraOptions(String extraOptions) {
        this.extraOptions = extraOptions;
    }


    /**
     *  Gets the ExtraOptions attribute
     *
     *@return    The ExtraOptions value
     */
    public String getExtraOptions() {
        return this.extraOptions;
    }


    /**
     *  get any extra options or null for no argument needed
     *
     *@return    The ExtraOptions Parameter to CSC
     */
    protected String getExtraOptionsParameter() {
        if (extraOptions != null && extraOptions.length() != 0) {
            return extraOptions;
        } else {
            return null;
        }
    }


    /**
     *  Set the source directory of the files to be compiled.
     *
     *@param  srcDirName  The new SrcDir value
     */
    public void setSrcDir(File srcDirName) {
        this.srcDir = srcDirName;
    }


    /**
     * Set the destination directory of files to be compiled.
     *
     *@param  dirName  The new DestDir value
     */
    public void setDestDir(File dirName) {
        this.destDir = dirName;
    }


    /**
     * Set the type of target.
     *
     *@param  ttype          The new TargetType value
     *@exception  BuildException  if target is not one of
     *      exe|library|module|winexe
     */
    public void setTargetType(String ttype)
             throws BuildException {
        targetType = ttype.toLowerCase();
        if (targetType.equals("exe") || targetType.equals("library") ||
                targetType.equals("module") || targetType.equals("winexe")) {
            targetType = targetType;
        } else {
            throw new BuildException("targetType " + targetType 
                    + " is not one of 'exe', 'module', 'winexe' or 'library'" );
        }
    }


    /**
     *  Gets the TargetType attribute
     *
     *@return    The TargetType value
     */
    public String getTargetType() {
        return targetType;
    }


    /**
     *  get the argument or null for no argument needed
     *
     *@return    The TargetType Parameter to CSC
     */
    protected String getTargetTypeParameter() {
        if (notEmpty(targetType)) {
            return "/target:" + targetType;
        } else {
            return null;
        }
    }


    /**
     *  Set the filename of icon to include.
     *
     *@param  fileName  path to the file. Can be relative, absolute, whatever.
     */
    public void setWin32Icon(File fileName) {
        win32icon = fileName;
    }


    /**
     *  get the argument or null for no argument needed
     *
     *@return    The Win32Icon Parameter to CSC
     */
    protected String getWin32IconParameter() {
        if (win32icon != null) {
            return "/win32icon:" + win32icon.toString();
        } else {
            return null;
        }
    }


    /**
     * Sets the filename of a win32 resource (.RES) file to include.
     * This is not a .NET resource, but what Windows is used to.
     *
     *@param  fileName  path to the file. Can be relative, absolute, whatever.
     */
    public void setWin32Res(File fileName) {
        win32res = fileName;
    }


    /**
     *  get the argument or null for no argument needed
     *
     *@return    The Win32Res Parameter to CSC
     */
    protected String getWin32ResParameter() {
        if (win32res != null) {
            return "/win32res:" + win32res.toString();
        } else {
            return null;
        }
    }


    /**
     * If true, require all compiler output to be in UTF8 format.
     *
     *@param  enabled  The new utf8Output value
     */
    public void setUtf8Output(boolean enabled) {
        utf8output = enabled;
    }


    /**
     *  Gets the utf8OutpuParameter attribute of the CSharp object
     *
     *@return    The utf8OutpuParameter value
     */
    protected String getUtf8OutputParameter() {
        return utf8output ? "/utf8output" : null;
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
     *@return    The fullPathsParameter value
     */
    protected String getFullPathsParameter() {
        return fullpaths ? "/fullpaths" : null;
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
     *  get the argument or null for no argument needed
     *
     *@return    The Definitions Parameter to CSC
     */
    protected String getDefinitionsParameter() {
        if (notEmpty(definitions)) {
            return "/define:" + definitions;
        } else {
            return null;
        }
    }


    /**
     * Semicolon separated list of modules to refer to.
     *
     *@param  params  The new additionalModules value
     */
    public void setAdditionalModules(String params) {
        additionalModules = params;
    }


    /**
     *  get the argument or null for no argument needed
     *
     *@return    The AdditionalModules Parameter to CSC
     */
    protected String getAdditionalModulesParameter() {
        if (notEmpty(additionalModules)) {
            return "/addmodule:" + additionalModules;
        } else {
            return null;
        }
    }


    /**
     *  Set the output file
     *
     *@param  params  The new outputFile value
     */
    public void setOutputFile(File params) {
        outputFile = params;
    }

    /**
     *  Set the name of exe/library to create.
     *
     *@param  file  The new outputFile value
     */
    public void setDestFile(File file) {
        outputFile = file;
    }
    

    /**
     *  get the argument or null for no argument needed
     *
     *@return    The OutputFile Parameter to CSC
     */
    protected String getOutputFileParameter() {
        if (outputFile != null) {
            File f = outputFile;
            return "/out:" + f.toString();
        } else {
            return null;
        }
    }


    /**
     * If true, fail on compilation errors.
     *
     *@param  b  The new FailOnError value
     */
    public void setFailOnError(boolean b) {
        failOnError = b;
    }


    /**
     *  query fail on error flag
     *
     *@return    The FailFailOnError value
     */
    public boolean getFailOnError() {
        return failOnError;
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
     *  reset all contents.
     */
    public void Clear() {
        targetType = null;
        win32icon = null;
        srcDir = null;
        destDir = null;
        mainClass = null;
        unsafe = false;
        warnLevel = 3;
        docFile = null;
        incremental = false;
        optimize = false;
        debug = true;
        references = null;
        failOnError = true;
        definitions = null;
        additionalModules = null;
        includeDefaultReferences = true;
        extraOptions = null;
        fullpaths = true;
        fileAlign = 0;
    }


    /**
     *  test for a string containing something useful
     *
     *@param  s  string in
     *@return    true if the argument is not null or empty
     */
    protected boolean notEmpty(String s) {
        return s != null && s.length() != 0;
    }

    /**
     * validation code
     * @throws  BuildException  if validation failed
     */ 
    protected void validate() 
            throws BuildException {
        if (outputFile != null && outputFile.isDirectory()) {
            throw new BuildException("destFile cannot be a directory");
        }
    }

    /**
     *  do the work by building the command line and then calling it
     *
     *@throws  BuildException  if validation or execution failed
     */
    public void execute()
             throws BuildException {
        if (srcDir == null) {
            srcDir = project.resolveFile(".");
        }
        log("CSC working from source directory " + srcDir, Project.MSG_VERBOSE);
        validate();

        NetCommand command = new NetCommand(this, "CSC", csc_exe_name);
        command.setFailOnError(getFailOnError());
        //DEBUG helper
        command.setTraceCommandLine(true);
        //fill in args
        command.addArgument("/nologo");
        command.addArgument(getAdditionalModulesParameter());
        command.addArgument(getDefinitionsParameter());
        command.addArgument(getDebugParameter());
        command.addArgument(getDocFileParameter());
        command.addArgument(getIncrementalParameter());
        command.addArgument(getMainClassParameter());
        command.addArgument(getOptimizeParameter());
        command.addArgument(getReferencesParameter());
        command.addArgument(getTargetTypeParameter());
        command.addArgument(getUnsafeParameter());
        command.addArgument(getWarnLevelParameter());
        command.addArgument(getWin32IconParameter());
        command.addArgument(getOutputFileParameter());
        command.addArgument(getIncludeDefaultReferencesParameter());
        command.addArgument(getDefaultReferenceParameter());
        command.addArgument(getWin32ResParameter());
        command.addArgument(getUtf8OutputParameter());
        command.addArgument(getNoConfigParameter());
        command.addArgument(getFullPathsParameter());
        command.addArgument(getExtraOptionsParameter());
        command.addArgument(getFileAlignParameter());

        long outputTimestamp;
        if (outputFile != null && outputFile.exists()) {
            outputTimestamp = outputFile.lastModified();
        } else {
            outputTimestamp = 0;
        }
        int filesOutOfDate = 0;
        //get dependencies list.
        DirectoryScanner scanner = super.getDirectoryScanner(srcDir);
        String[] dependencies = scanner.getIncludedFiles();
        log("compiling " + dependencies.length + " file" + ((dependencies.length == 1) ? "" : "s"));
        String baseDir = scanner.getBasedir().toString();
        File base = scanner.getBasedir();
        //add to the command
        for (int i = 0; i < dependencies.length; i++) {
            File targetFile = new File(base, dependencies[i]);
            log(targetFile.toString(), Project.MSG_VERBOSE);
            command.addArgument(targetFile.toString());
            if (targetFile.lastModified() > outputTimestamp) {
                filesOutOfDate++;
                log("Source file " + targetFile.toString() + " is out of date",
                        Project.MSG_VERBOSE);
            } else {
                log("Source file " + targetFile.toString() + " is up to date",
                        Project.MSG_VERBOSE);
            }
            
        }

        //now run the command of exe + settings + files
        if (filesOutOfDate > 0) {
            command.runCommand();
        }
    }
    // end execute

}

