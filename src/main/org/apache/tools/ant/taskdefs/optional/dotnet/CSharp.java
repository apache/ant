/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

/* build notes

    -The reference CD to listen to while editing this file is 
        nap: Underworld  - Everything, Everything
    -variable naming policy from Fowler's refactoring book.
    -tested against the PDC pre-beta of csc.exe; future versions will 
     inevitably change things
*/

// ====================================================================
// place in the optional ant tasks package
// but in its own dotnet group
// ====================================================================

package org.apache.tools.ant.taskdefs.optional.dotnet;

// ====================================================================
// imports
// ====================================================================

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;
import java.io.*;


// ====================================================================
/**
This task compiles CSharp source into executables or modules.
The task will only work on win2K until other platforms support csc.exe or 
an equivalent. CSC.exe must be on the execute path too.

<p>
All parameters are optional: &lt;csc/&gt; should suffice to produce a debug
build of all *.cs files. References to external files do require explicit 
enumeration, so are one of the first attributes to consider adding. 

<p>

The task is a directory based task, so attributes like <b>includes="*.cs"</b> and 
<b>excludes="broken.cs"</b> can be used to control the files pulled in. By default, 
all *.cs files from the project folder down are included in the command. 
When this happens the output file -if not specified-
is taken as the first file in the list, which may be somewhat hard to control.
Specifying the output file with <b>'outfile'</b> seems prudent. 

<p>


<p>
TODO
<ol>
    <li>is incremental build still broken in beta-1?
    <li>is Win32Icon broken?
    <li>all the missing options
</ol>
<p>
History
    <Table>
    <tr><td>0.3</td>
        <td> Beta 1 edition </td>
        <td> To avoid having to remember which assemblies to include, 
        the task automatically refers to the main dotnet libraries in Beta1.
        
    </tr>    <tr><td>0.2</td>
        <td> Slightly different</td>
        <td> Split command execution to a separate class; 
    </tr>
    <tr><td>0.1</td>
        <td> "I can't believe it's so rudimentary"</td>
        <td>  First pass; minimal builds only support; 
    </tr>

    </table>
@version 0.3
@author Steve Loughran steve_l@iseran.com

 */

public class CSharp 
    extends org.apache.tools.ant.taskdefs.MatchingTask {

    /** constructor inits everything and set up the search pattern
    */
    
    public CSharp () {
        Clear();
        setIncludes(csc_file_pattern);
    }
    
    /** name of the executable. the .exe suffix is deliberately not included 
     * in anticipation of the unix version
     */
    protected static final String csc_exe_name="csc";
    
    /** what is the file extension we search on?
     */
    protected static final String csc_file_ext="cs";
    
    /** derive the search pattern from the extension 
     */
    protected static final String csc_file_pattern="**/*."+csc_file_ext;
    
    /** list of reference classes. (pretty much a classpath equivalent)
     */
    protected  String _references;
    
    /**
     *  Set the reference list to be used for this compilation. 
     *
     * @param  s  The new References value 
     */
    public void setReferences(String s) {
        _references=s;
    }
    
    /**
     *  get the reference string or null for no argument needed 
     *
     * @return    The References Parameter to CSC 
     */
    protected String getReferencesParameter() {
        //bail on no references
        if (notEmpty(_references)) 
            return "/reference:"+_references;
        else
            return null;
    }
                
    /**
     using the path approach didnt work as it could not handle the implicit
     execution path. Perhaps that could be extracted from the runtime and then
     the path approach would be viable
    */
    protected  Path _referenceFiles;
    
    /**
     * add another path to the reference file path list
     * @param path another path to append
     */
    public void setReferenceFiles(Path path) {
        //demand create pathlist
        if(_referenceFiles==null)
                _referenceFiles=new Path(this.project);
        _referenceFiles.append(path);
    }
    
    /**
     turn the path list into a list of files and a /references argument
     @return null or a string of references.
    */ 
    protected String getReferenceFilesParameter() {
        //bail on no references
        if (_references==null)
            return null;
        //iterate through the ref list & generate an entry for each
        //or just rely on the fact that the toString operator does this, but
        //noting that the separator is ';' on windows, ':' on unix
        String refpath=_references.toString();
    
        //bail on no references listed
        if (refpath.length()==0)
            return null;
        
        StringBuffer s=new StringBuffer("/reference:");
        s.append(refpath);
        return new String(s);
    }   
    
    /** 
     * Fix C# reference inclusion. 
     * C# is really dumb in how it handles inclusion. You have to list
     * every 'assembly' -read DLL that is imported. So already you are
     * making a platform assumption -shared libraries have a .dll;"+ extension
     * and the poor developer has to know every library which is included
     * why the compiler cant find classes on the path or in a directory,
     * is a mystery.
     *
     * To reduce the need to be explicit, here is a long list of 
     * the core libraries used in Beta-1 of .NET
     * ommitting the blatantly non portable (MS.win32.interop)
     * and the .designer libraries. (ripping out Com was tempting)
     * Casing is chosen to match that of the file system <i>exactly</i>
     * so may work on a unix box too. 
     */

    protected static final String DEFAULT_REFERENCE_LIST=
            "Accessibility.dll;"+
            "cscompmgd.dll;"+
            "CustomMarshalers.dll;"+
            "IEExecRemote.dll;"+
            "IEHost.dll;"+
            "IIEHost.dll;"+
            "ISymWrapper.dll;"+
            "Microsoft.JScript.dll;"+
            "Microsoft.VisualBasic.dll;"+
            "Microsoft.VisualC.dll;"+
            "Microsoft.Vsa.dll;"+
            "Mscorcfg.dll;"+
            "RegCode.dll;"+
            "System.Configuration.Install.dll;"+
            "System.Data.dll;"+
            "System.Design.dll;"+
            "System.DirectoryServices.dll;"+
            "System.EnterpriseServices.dll;"+
            "System.dll;"+
            "System.Drawing.Design.dll;"+
            "System.Drawing.dll;"+
            "System.Management.dll;"+
            "System.Messaging.dll;"+
            "System.Runtime.Remoting.dll;"+
            "System.Runtime.Serialization.Formatters.Soap.dll;"+
            "System.Security.dll;"+
            "System.ServiceProcess.dll;"+
            "System.Web.dll;"+
            "System.Web.RegularExpressions.dll;"+
            "System.Web.Services.dll;"+
            "System.Windows.Forms.dll;"+
            "System.XML.dll;";


         
    /** 
     * get default reference list
     * @return null or a string of references.
    */ 
    protected String getDefaultReferenceParameter() {
        if(_includeDefaultReferences) {
            StringBuffer s=new StringBuffer("/reference:");
            s.append(DEFAULT_REFERENCE_LIST);
            return new String(s);
        }
        else
            return null;            
    }
     
     /** flag to enable automatic reference inclusion
      *
     */
     protected boolean _includeDefaultReferences;

    /** set the automatic reference inclusion flag on or off
     *  this flag controls the string of references and the 
     *  /nostdlib option in CSC
    @param f on/off flag
    */
    public void setIncludeDefaultReferences(boolean f) {
        _includeDefaultReferences=f;
    }
    
    /** query the optimise flag
    @return true if optimise is turned on
     */
    public boolean getIncludeDefaultReferences() {
        return _includeDefaultReferences;
    }
    
    /**
     *  get the include default references flag or null for no argument needed 
     *
     * @return    The Parameter to CSC 
     */
    protected String getIncludeDefaultReferencesParameter() {
        return "/nostdlib"+(_includeDefaultReferences?"-":"+");
    }    
    
    /** optimise flag
     */
    protected boolean _optimize;
    
    /** set the optimise flag on or off
    @param f on/off flag
    */
    public void setOptimize(boolean f) {
        _optimize=f;
    }
    
    /** query the optimise flag
    @return true if optimise is turned on
     */
    public boolean getOptimize() {
        return _optimize;
    }
    
    /**
     *  get the optimise flag or null for no argument needed 
     *
     * @return    The Optimize Parameter to CSC 
     */
    protected String getOptimizeParameter() {
        return "/optimize"+(_optimize?"+":"-");
    }   
        
    /** incremental build flag */
    protected boolean _incremental;
    
    /** set the incremental compilation flag on or off
     * @param f on/off flag
     */
    public void setIncremental(boolean f){
        _incremental=f;
    }
    
    /** query the incrementalflag
     * @return true iff incremental compilation is turned on
     */
    public boolean getIncremental() {
        return _incremental;
    }
    
    /**
     *  get the incremental build argument
     *
     * @return    The Incremental Parameter to CSC 
     */
    protected String getIncrementalParameter() {
        return "/incremental"+(_incremental?"+":"-");
    }   
    
    /** debug flag. Controls generation of debug information. 
     */
    protected boolean _debug;
    
    /** set the debug flag on or off
     * @param f on/off flag
     */
    public void setDebug(boolean f) {
        _debug=f;
    }
        
    /** query the debug flag
     * @return true if debug is turned on
     */
    public boolean getDebug() {
        return _debug;
    }
    
    /**
     *  get the debug switch argument  
     *
     * @return    The Debug Parameter to CSC 
     */
    protected String getDebugParameter() {
        return "/debug"+(_debug?"+":"-");
    }   
    
    /** output XML documentation flag
    */  
    protected File _docFile;
        
    /** file for generated XML documentation
     * @param f output file
     */
    public void setDocFile(File f) {
        _docFile = f;
    }
            
    /** get the argument or null for no argument needed
     * @return    The DocFile Parameter to CSC 
     */
    protected String getDocFileParameter() {
        if (_docFile!=null)
            return "/doc:"+_docFile.toString();
        else
            return null;
    }   
        
    /** warning level: 0-4, with 4 being most verbose
    */
    private int _warnLevel;
    
    /** set warn level (no range checking)
     * @param warnLevel warn level -see .net docs for valid range (probably 0-4)
    */
    public void setWarnLevel(int warnLevel)
        {this._warnLevel=warnLevel;}
    
    /** query warn level
     * @return current value
    */
    public int getWarnLevel()
        {return _warnLevel;}
    
    /**
     *  get the warn level switch 
     *
     * @return    The WarnLevel Parameter to CSC 
     */
    protected String getWarnLevelParameter() {
        return "/warn:"+_warnLevel;
    }   
    
    /** enable unsafe code flag. Clearly set to false by default
     */
    protected boolean _unsafe;
    
    /**
     *  Sets the Unsafe attribute 
     *
     * @param  unsafe  The new Unsafe value 
     */
    public void setUnsafe(boolean unsafe)
        {this._unsafe=unsafe;}
    
    /**
     *  query the Unsafe attribute 
     *
     * @return    The Unsafe value 
     */
    public boolean getUnsafe()
        {return this._unsafe;}
    
    /** get the argument or null for no argument needed
     * @return    The Unsafe Parameter to CSC 
    */
    protected String getUnsafeParameter(){
        return _unsafe?"/unsafe":null;
    }   
        
    /** main class (or null for automatic choice)
     */
    protected String _mainClass;
    
    /**
     *  Sets the MainClass attribute
     *
     * @param  mainClass  The new MainClass value 
     */
    public void setMainClass(String mainClass)
        {this._mainClass=mainClass;}
    
    /**
     *  Gets the MainClass attribute 
     *
     * @return    The MainClass value 
     */
    public String getMainClass()
        {return this._mainClass;}
    
    /**
     *  get the /main argument or null for no argument needed 
     *
     * @return    The MainClass Parameter to CSC 
     */
    protected String getMainClassParameter(){
        if (_mainClass!=null && _mainClass.length()!=0)
            return "/main:"+_mainClass;
        else
            return null;
    } 
    
    /** any extra command options?
     */
    protected String _extraOptions;
    
    /**
     *  Sets the ExtraOptions attribute
     *
     * @param  extraOptions  The new ExtraOptions value 
     */
    public void setExtraOptions(String extraOptions)
        {this._extraOptions=extraOptions;}
    
    /**
     *  Gets the ExtraOptions attribute 
     *
     * @return    The ExtraOptions value 
     */
    public String getExtraOptions()
        {return this._extraOptions;}
    
    /**
     *  get any extra options or null for no argument needed 
     *
     * @return    The ExtraOptions Parameter to CSC 
     */
    protected String getExtraOptionsParameter() {
        if (_extraOptions!=null && _extraOptions.length()!=0)
            return _extraOptions;
        else
            return null;
    } 
    
    /** source directory upon which the search pattern is applied
     */
    private File _srcDir;
    
    /**
     * Set the source dir to find the files to be compiled
     * @param  srcDirName  The new SrcDir value 
     */
    public void setSrcDir(File srcDirName){
        _srcDir = srcDirName;
    }
    
    /** destination directory (null means use the source directory)
    * NB: this is currently not used 
    */
    private File _destDir;
    
    /**
     * Set the destination dir to find the files to be compiled
     * @param  dirName  The new DestDir value 
     */
    public void setDestDir(File dirName) {
        _destDir = dirName;
    }
    
    /** type of target. Should be one of exe|library|module|winexe|(null)
    default is exe; the actual value (if not null) is fed to the command line.
    <br>See /target
     */
    protected String _targetType;
    
    /** define the target
     * @param  targetType          The new TargetType value 
     * @exception  BuildException  if target is not one of exe|library|module|winexe 
     */
    public void setTargetType(String targetType)
        throws  BuildException {
        targetType=targetType.toLowerCase();
        if(targetType.equals("exe") || targetType.equals("library") ||
            targetType.equals("module") ||targetType.equals("winexe") ) {
            _targetType=targetType; 
        }
        else 
            throw new BuildException("targetType " +targetType+" is not a valid type");
    }
    
    /**
     *  Gets the TargetType attribute  
     *
     * @return    The TargetType value 
     */
    public String getTargetType() { 
            return _targetType;
    }   
    
    /**
     *  get the argument or null for no argument needed 
     *
     * @return    The TargetType Parameter to CSC 
     */
    protected String getTargetTypeParameter() {
        if (notEmpty(_targetType)) 
            return "/target:"+_targetType;
        else
            return null;
    }   
    
    /** icon for incorporation into apps
     */     
    protected File _win32icon;  
    
    /**
     * Set the win32 icon 
     * @param fileName path to the file. Can be relative, absolute, whatever.
     */
    public void setWin32Icon(File fileName) {
        _win32icon = fileName;
    }
    
    /**
     *  get the argument or null for no argument needed 
     *
     * @return    The Win32Icon Parameter to CSC 
     */
    protected String getWin32IconParameter() {
        if (_win32icon!=null)
            return "/win32icon:"+_win32icon.toString();
        else
            return null;
    }
    /** icon for incorporation into apps
     */     
    protected File _win32res;  
    
    /**
     * Set the win32 icon 
     * @param fileName path to the file. Can be relative, absolute, whatever.
     */
    public void setWin32Res(File fileName) {
        _win32res = fileName;
    }
    
    /**
     *  get the argument or null for no argument needed 
     *
     * @return    The Win32Icon Parameter to CSC 
     */
    protected String getWin32ResParameter() {
        if (_win32res!=null)
            return "/win32res:"+_win32res.toString();
        else
            return null;
    }    
    
    /** 
     * utf out flag
     */
     
    protected boolean _utf8output=false;
    
    /**
     * enable generation of utf8 output from the compiler. 
     */
    public void setUtf8Output(boolean enabled) {
        _utf8output=enabled;
    }
    
    protected String getUtf8OutpuParameter() {
        return _utf8output?"/utf8output":null;
    }
    
    protected boolean _noconfig=false;
    
    protected void setNoConfig(boolean enabled) {
        _noconfig=enabled;
    }
    
    protected String getNoConfigParameter() {
        return _noconfig?"/noconfig":null;
    }
    
    // /fullpaths
    protected boolean _fullpaths=false;
    
    public void setFullPaths(boolean enabled) {
        _fullpaths=enabled;
    }
    
    protected String getFullPathsParameter() {
        return _fullpaths?"/fullpaths":null;
    }
    
    /** defines list
    * something like 'RELEASE;WIN32;NO_SANITY_CHECKS;;SOMETHING_ELSE'
    */
    String _definitions;
    
    /**
     * Set the definitions
     * @param list of definitions split by ; or , or even :
     */
    public void setDefinitions(String params) {
        _definitions=params;
    }
      
    /**
    *  get the argument or null for no argument needed 
    *
    * @return    The Definitions Parameter to CSC 
    */
    protected String getDefinitionsParameter() {
        if (notEmpty(_definitions))
            return "/define:" + _definitions;
        else
            return null;
    }
    
    /** list of extra modules to refer to 
     *
    */    
    String _additionalModules;
    
    /**
     * Set the definitions
     * @param list of definitions split by ; or , or even :
     */
    public void setAdditionalModules(String params) {
        _additionalModules=params;
    }
    
    /** get the argument or null for no argument needed
    * @return    The AdditionalModules Parameter to CSC 
     */
    protected String getAdditionalModulesParameter() {
        if (notEmpty(_additionalModules)) 
            return "/addmodule:" + _additionalModules;
        else 
            return null;
    }
    
    /** output file. If not supplied this is derived from the
     *  source file
     */    
    protected File _outputFile;
    
    /**
     * Set the definitions
     * @param list of definitions split by ; or , or even :
     */
    public void setOutputFile(File params) {
        _outputFile = params;
    }
    
    /** get the argument or null for no argument needed
    * @return    The OutputFile Parameter to CSC 
    */
    protected String getOutputFileParameter() {
        if (_outputFile != null) {
            File f = _outputFile;
            return "/out:"+f.toString();
        }
        else            
            return null;
    }
    
    /** flag to control action on execution trouble
    */
    protected boolean _failOnError;
    
    /**set fail on error flag
    * @param  b  The new FailOnError value 
    */
    public void setFailOnError(boolean b){
        _failOnError=b;
    }
    
    /** query fail on error flag
     * @return    The FailFailOnError value 
    */
    public boolean getFailFailOnError() {
            return _failOnError;
    }
    
    /** reset all contents. 
    */
    public void Clear() {
        _targetType=null;
        _win32icon=null;
        _srcDir=null;
        _destDir=null;
        _mainClass=null;
        _unsafe=false;
        _warnLevel=3;
        _docFile = null;
        _incremental=false;
        _optimize=false;
        _debug=true;
        _references=null;
        _failOnError=true;
        _definitions=null;
        _additionalModules=null;
        _includeDefaultReferences=true;
        _extraOptions=null;
        _fullpaths=true;
    }
    
    /**
     *  test for a string containing something useful 
     *
     * @param  s  string in 
     * @return    true if the argument is not null or empty 
     */
    protected boolean notEmpty(String s) {
        return s != null && s.length() != 0;
    }
    
    /** do the work by building the command line and then calling it
    */
    public void execute() 
        throws BuildException {
        if (_srcDir == null)
            _srcDir=project.resolveFile(".");
    
        NetCommand command=new NetCommand(this,"CSC",csc_exe_name);
        command.setFailOnError(getFailFailOnError());
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
        command.addArgument(getUtf8OutpuParameter());
        command.addArgument(getNoConfigParameter());
        command.addArgument(getFullPathsParameter());
        command.addArgument(getExtraOptionsParameter());
        
        //get dependencies list. 
        DirectoryScanner scanner = super.getDirectoryScanner(_srcDir);
        String[] dependencies = scanner.getIncludedFiles();
        log("compiling "+dependencies.length+" file"+((dependencies.length==1)?"":"s"));
        String baseDir=scanner.getBasedir().toString();
        //add to the command
        for (int i = 0; i < dependencies.length; i++) {
            String targetFile=dependencies[i];
            targetFile=baseDir+File.separator+targetFile;
            command.addArgument(targetFile);
        }
        
        //now run the command of exe + settings + files
        command.runCommand();
    } // end execute
    
} //end class
