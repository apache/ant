/*
*******************************************************************
*
* File:         Csharp.java
* RCS:          $Header$
* Author:       Steve Loughran
* Created:      July 21,  2000
* Modified:     $Modtime: 00-11-01 12:57 $
* Language:     Java
* Status:       Experimental 
*
*********************************************************************/

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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import java.io.*;
import java.text.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;


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

<table border="1" cellpadding="2" cellspacing="0">
  <tr>
    <td valign="top"><b>Attribute</b></td>
    <td valign="top"><b>Description</b></td>
    <td align="center" valign="top"><b>Example</b></td>
  </tr>


<tr>
    <td valign="top">additionalModules
    <td valign="top">Semicolon separated list of modules to refer to
    </tr>
  <tr>
    <td valign="top">defaultexcludes</td>
    <td valign="top">indicates whether default excludes should be used or not
      (&quot;yes&quot;/&quot;no&quot;). Default excludes are used when omitted.</td>
  </tr>
<tr>
    <td valign="top">definitions
    <td valign="top">defined constants
    <td valign="top"> "RELEASE;BETA1"
    </tr>
<tr>
    <td valign="top">debug
    <td valign="top">include debug information
    <td valign="top">"true" or "false"
    </tr>

<tr>
    <td valign="top">docFile
    <td valign="top">name of file for documentation
    <td valign="top">"doc.xml"
    </tr>
  <tr>
    <td valign="top">excludes</td>
    <td valign="top">comma separated list of patterns of files that must be
      excluded. No files (except default excludes) are excluded when omitted.</td>
  </tr>
  <tr>
    <td valign="top">excludesfile</td>
    <td valign="top">the name of a file. Each line of this file is
      taken to be an exclude pattern</td>
  </tr>
<tr>
    <td valign="top">failOnError
    <td valign="top">Should a failed compile halt the build?
    <td valign="top">"true" or "false"
    </tr>
  <tr>
    <td valign="top">includes</td>
    <td valign="top">comma separated list of patterns of files that must be
      included. All files are included when omitted.</td>
  </tr>
  <tr>
    <td valign="top">includesfile</td>
    <td valign="top">the name of a file. Each line of this file is
      taken to be an include pattern</td>
  </tr>
<tr>
    <td valign="top">incremental
    <td valign="top">Incremental build flag. Off by default
    <td valign="top">"false"
    </tr>
<tr>
    <td valign="top">mainClass
    <td valign="top">name of main class for executables
    <td valign="top">com.example.project.entrypoint
    </tr>
<tr>
    <td valign="top">optimize
    <td valign="top">optimisation flag
    <td valign="top">"true" or "false"
    </tr>
<tr>
    <td valign="top">outputFile
    <td valign="top">filename of output
    <td valign="top">"example.exe"
    </tr>
<tr>
    <td valign="top">references
    <td valign="top">Semicolon separated list of dlls to refer to
    </tr>
<tr>
    <td valign="top">srcDir
    <td valign="top">source directory (default = project directory)
    <td valign="top">
    </tr>
<tr>
    <td valign="top">targetType
    <td valign="top">Type of target
    <td valign="top">"exe","module","winexe","library"
    </tr>
<tr>
    <td valign="top">unsafe
    <td valign="top">enable unsafe code
    <td valign="top">"true" or "false"
    </tr>
<tr>
    <td valign="top">warnLevel
    <td valign="top">level of warning
    <td valign="top">1-4
    </tr>
<tr>
    <td valign="top">win32Icon
    <td valign="top">filename of icon to include
    <td valign="top">
    </tr>

</table>



<p>
The first pass is just a proof of concept; enough to test. 
<p>
TODO
<ol>
    <li>get PATH incorporated into reference/module lookup
    <li>is Win32Icon broken?
    <li>all the missing options
</ol>
<p>
History
        <Table>
        <tr><td>0.2</td>
                <td> Slightly different</td>
                <td> Split command execution to a separate class; 
        </tr>
        <tr><td>0.1</td>
                <td> "I can't believe it's so rudimentary"</td>
                <td>  First pass; minimal builds only support; 
        </tr>

        </table>
@version 0.2
@author Steve Loughran steve_l@iseran.com

 */
// ====================================================================

public class CSharp 
    extends org.apache.tools.ant.taskdefs.MatchingTask {

    //=============================================================================        
    /** constructor inits everything and set up the search pattern
     */

    public CSharp () {
        Clear();
        setIncludes(csc_file_pattern);
    }


    //-----------------------------------------------------------------------------
    /** name of the executable. the .exe suffix is deliberately not included 
     * in anticipation of the unix version
     */
    protected static final String csc_exe_name="csc";

    /** what is the file extension we search on?
     */
    protected static final String csc_file_ext="cs";

    /** and now derive the search pattern from the extension 
     */
    protected static final String csc_file_pattern="*."+csc_file_ext;


    //=============================================================================        
    /** list of reference classes. (pretty much a classpath equivalent)
     */

    protected  String _references;

    /**
     * Set the reference list to be used for this compilation.
     */

    public void setReferences(String s) {
        _references=s;
    }

    /** get the argument or null for no argument needed
     */
    protected String getReferencesParameter() {
        //bail on no references
        if (_references==null ||_references.length()==0)
            return null;
        else
            return "/reference:"+_references;
    }
            

    /* using the path approach didnt work as it could not handle the implicit
       execution path. Perhaps that could be extracted from the runtime and then
       the path approach would be viable


       protected  Path _references;

       public void setReferences(Path s) {
       //demand create pathlist
       if(_references==null)
       _references=new Path(this.project);
       _references.append(s);
       }

       protected String getReferencesParameter()
       {
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
    */

    //=============================================================================        
    /* optimise flag
     */
 
    protected boolean _optimize;

    /** set the optimise flag on or off
        @param on/off flag
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

    /** get the argument or null for no argument needed
     */
    protected String getOptimizeParameter() {
        return "/optimize"+(_optimize?"+":"-");
    }        
        
    //=============================================================================        
    /** incremental build flag */
    protected boolean _incremental;

    /** set the incremental compilation flag on or off
     *@param on/off flag
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

    /** get the argument or null for no argument needed
     */
    protected String getIncrementalParameter() {
        return "/incremental"+(_incremental?"+":"-");
    }        

    //=============================================================================        
    /** debug flag. Controls generation of debug information. 
     */
 
    protected boolean _debug;

    /** set the debug flag on or off
     * @param on/off flag
     */
 
    public void setDebug(boolean f)
    {_debug=f;}
        
    /** query the debug flag
     * @return true if debug is turned on
     */
 
    public boolean getDebug() {
        return _debug;
    }

    /** get the argument or null for no argument needed
     */
    protected String getDebugParameter() {
        return "/debug"+(_debug?"+":"-");
    }        


    //=============================================================================        
    /** output XML documentation flag
     */        
        
    protected File _docFile;
        
    /** file for generated XML documentation
     * @param output file
     */
 
    public void setDocFile(String f) {
        _docFile=project.resolveFile(f);
    }



    /** get the argument or null for no argument needed
     */
    protected String getDocFileParameter() {
        if (_docFile!=null)
            return "/doc:"+_docFile.toString();
        else
            return null;
    }        
        
    //=============================================================================        
    /** warning level: 0-4, with 4 being most verbose
     */
    private int _warnLevel;

    /** set warn level (no range checking)
     * @param warn level -see .net docs for valid range (probably 0-4)
     */
    public void setWarnLevel(int warnLevel)
    {this._warnLevel=warnLevel;}

    /** query warn level
     * @return current value
     */
    public int getWarnLevel()
    {return _warnLevel;}

    /** get the argument or null for no argument needed
     */
    protected String getWarnLevelParameter() {
        return "/warn:"+_warnLevel;
    }        

    //=============================================================================        
    /** enable unsafe code flag. Clearly set to false by default
     */

    protected boolean _unsafe;

    public void setUnsafe(boolean unsafe)
    {this._unsafe=unsafe;}

    public boolean getUnsafe()
    {return this._unsafe;}

    /** get the argument or null for no argument needed
     */
    protected String getUnsafeParameter(){
        return _unsafe?"/unsafe":null;
    }        
        
    //=============================================================================        
    /** main class (or null for automatic choice)
     */
    protected String _mainClass;

    public void setMainClass(String mainClass)
    {this._mainClass=mainClass;}

    public String getMainClass()
    {return this._mainClass;}

    /** get the argument or null for no argument needed
     */
    protected String getMainClassParameter(){
        if (_mainClass!=null && _mainClass.length()!=0)
            return "/main:"+_mainClass;
        else
            return null;
    }        

    //=============================================================================        
    /** source directory upon which the search pattern is applied
     */
    private File _srcDir;

    /**
     * Set the source dir to find the files to be compiled
     */
    public void setSrcDir(String srcDirName){
        _srcDir = project.resolveFile(srcDirName);
    }

    //=============================================================================        
    /** destination directory (null means use the source directory)
     */
    private File _destDir;

    /**
     * Set the source dir to find the files to be compiled
     */
    public void setDestDir(String dirName) {
        _destDir = project.resolveFile(dirName);
    }
        

    //=============================================================================        
    /** type of target. Should be one of exe|library|module|winexe|(null)
        default is exe; the actual value (if not null) is fed to the command line.
        <br>See /target
    */
    protected String _targetType;

    /** define the target
     * param target. 
     * @throws BuildException if target is not one of exe|library|module|winexe 
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

    public String getTargetType() { 
        return _targetType;
    }         

    /** get the argument or null for no argument needed
     */
    protected String getTargetTypeParameter() {
        if (_targetType!=null)
            return "/target:"+_targetType;
        else
            return null;
    }        


    //=============================================================================        
    /* icon for incorporation into apps
     */
 
    protected File _win32icon;        

    /**
     * Set the win32 icon 
     * @param path to the file. Can be relative, absolute, whatever.
     */
    public void setWin32Icon(String fileName) {
        _win32icon = project.resolveFile(fileName);
    }

    /** get the argument or null for no argument needed
     */
    protected String getWin32IconParameter() {
        if (_win32icon!=null)
            return "/win32icon:"+_win32icon.toString();
        else
            return null;
    }

    //=============================================================================        
    /** defines list 'RELEASE;WIN32;NO_SANITY_CHECKS;;SOMETHING_ELSE'
     *
     */

    String _definitions;

    /**
     * Set the definitions
     * @param list of definitions split by ; or , or even :
     */
    public void setDefinitions(String params) {
        _definitions=params;
    }

    /** get the argument or null for no argument needed
     */
    protected String getDefinitionsParameter() {
        if (_definitions==null || _definitions.length()==0)
            return null;
        else return "/DEFINE:"+_definitions;
    }

    //=============================================================================        
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
     */
    protected String getAdditionalModulesParameter() {
        if (_additionalModules==null || _additionalModules.length()==0)
            return null;
        else return "/addmodule:"+_additionalModules;
    }

    //=============================================================================        
    /** output file. If not supplied this is derived from the
     *  source file
     */

    protected String _outputFile;

    /**
     * Set the definitions
     * @param list of definitions split by ; or , or even :
     */
    public void setOutputFile(String params) {
        _outputFile=params;
    }

    /** get the argument or null for no argument needed
     */
    protected String getOutputFileParameter() {
        if (_outputFile==null || _outputFile.length()==0)
            return null;
        File f=project.resolveFile(_outputFile);
        return "/out:"+f.toString();
    }

    //=============================================================================        
    /** flag to control action on execution trouble
     */

    protected boolean _failOnError;

    /**set fail on error flag
     */
    public void setFailOnError(boolean b){
        _failOnError=b;
    }

    /** query fail on error flag
     */
    public boolean getFailFailOnError() {
        return _failOnError;
    }

    //=============================================================================        
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
    }


    //=============================================================================        
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
        /* space for more argumentativeness
           command.addArgument();
           command.addArgument();
        */


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
