/* -*-Java-*-
*******************************************************************
*
* File:         Csharp.java
* RCS:          $Header$
* Author:       Steve Loughran
* Created:      July 21,  2000
* Modified:		$Modtime: 00-11-01 10:25 $
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

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;
import java.io.*;
import java.util.*;
import java.text.*;

/**
   Task to assemble .net 'Intermediate Language' files.
   The task will only work on win2K until other platforms support csc.exe or 
   an equivalent. ilasm.exe must be on the execute path too.
   <p>

   <p>
   All parameters are optional: &lt;il/&gt; should suffice to produce a debug
   build of all *.il files.
   The option set is roughly compatible with the CSharp class;
   even though the command line options are only vaguely
   equivalent. [The low level commands take things like /OUT=file,
   csc wants /out:file ... /verbose is used some places; /quiet here in
   ildasm... etc.] It would be nice if someone made all the command line
   tools consistent (and not as brittle as the java cmdline tools) 


   <p>

   The task is a directory based task, so attributes like <b>includes="*.il"</b> and 
   <b>excludes="broken.il"</b> can be used to control the files pulled in. 
   Each file is built on its own, producing an appropriately named output file unless
   manually specified with <b>outfile</b>

   <p>

   <table border="1" cellpadding="2" cellspacing="0">
   <tr>
   <td valign="top"><b>Attribute</b></td>
   <td valign="top"><b>Description</b></td>
   <td align="center" valign="top"><b>Example</b></td>
   </tr>


   <tr>
   <tr>
   <td valign="top">defaultexcludes</td>
   <td valign="top">indicates whether default excludes should be used or not
   (&quot;yes&quot;/&quot;no&quot;). Default excludes are used when omitted.</td>
   </tr>

   <tr>
   <td valign="top">debug
   <td valign="top">include debug information
   <td valign="top">true (default)
   </tr>
   <tr>
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
   <td valign="top">true(default)
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
   <td valign="top">listing
   <td valign="top">Produce a listing (off by default)
   <td valign="top">off (default)
   </tr>
   <tr>
   <td valign="top">outputFile
   <td valign="top">filename of output
   <td valign="top">"example.exe"
   </tr>
   <tr>
   <td valign="top">owner
   <td valign="top">restrict disassembly by setting the 'owner' string
   <td valign="top">"secret"
   </tr>
   <tr>
   <td valign="top">resourceFile
   <td valign="top">name of resource file to include
   <td valign="top">"resources.res"
   </tr>
   <tr>
   <td valign="top">srcDir
   <td valign="top">source directory (default = project directory)
   <td valign="top">
   </tr>
   <tr>
   <td valign="top">targetType
   <td valign="top">Type of target. library means DLL is output. 
   <td valign="top">"exe","library"
   </tr>
   <tr>
   <td valign="top">verbose
   <td valign="top">output progress messages
   <td valign="top">off (default)
   </tr>

   </table>




   @author Steve Loughran steve_l@iseran.com
   @version 0.1
*/
// ====================================================================

public class Ilasm
    extends org.apache.tools.ant.taskdefs.MatchingTask {

    //=============================================================================	
    /** constructor inits everything and set up the search pattern
     */

    public Ilasm () {
        Clear();
        setIncludes(file_pattern);
    }


    //-----------------------------------------------------------------------------
    /** name of the executable. the .exe suffix is deliberately not included 
     * in anticipation of the unix version
     */
    protected static final String exe_name="ilasm";

    /** what is the file extension we search on?
     */
    protected static final String file_ext="il";

    /** and now derive the search pattern from the extension 
     */
    protected static final String file_pattern="*."+file_ext;

    /** title of task for external presentation
     */
    protected static final String exe_title="ilasm";

    //=============================================================================	
    /** reset all contents. 
     */
    public void Clear() {
        _targetType=null;
        _srcDir=null;
        _listing = false;
        _verbose=false;
        _debug=true;
        _owner=null;
        _outputFile=null;
        _failOnError=true;
        _resourceFile=null;
        _owner=null;
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
        if(targetType.equals("exe") || targetType.equals("library")) {
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
        if(!notEmpty(_targetType))
            return null;
        if (_targetType.equals("exe"))
            return "/exe";
        else 
            if (_targetType.equals("library"))
                return "/dll";
            else
                return null;
    }	
	
    //=============================================================================	
    /** owner string is a slightly trivial barrier to disassembly
     */

    protected String _owner;

    public void setOwner(String s) {
        _owner=s;
    }
    
    protected String getOwnerParameter() {
        if(notEmpty(_owner))
            return "/owner="+_owner;
        else 
            return null;
    }

    //=============================================================================	
    /** test for a string containing something useful
     * @param string to test
     * @returns true if the argument is not null or empty
     */
    protected boolean notEmpty(String s)
    {return s!=null && s.length()!=0;}

    //=============================================================================	
    /** verbose flag
     */
     
    protected boolean _verbose;

    public void setVerbose(boolean b) {
        _verbose=b;
    }

    protected String getVerboseParameter() {
        return _verbose?null:"/quiet";
    }   

    //=============================================================================	
    /** listing flag
     */
     
    protected boolean _listing;

    public void setListing(boolean b) {
        _listing=b;
    }

    protected String getListingParameter() {
        return _listing?"/listing":"/nolisting";
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
        return "/output="+f.toString();
    }

    //=============================================================================	
    /** resource file (.res format) to include in the app. 
     */
 
    protected String _resourceFile;

    public void setResourceFile(String s) {
        _resourceFile=s;
    }
    
    protected String getResourceFileParameter() {
        if(notEmpty(_resourceFile)) {
            return "/resource="+_resourceFile;
        }
        else 
            return null;
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
        return _debug?"/debug":null;
    }	
   
    //=============================================================================	
    /** This is the execution entry point. Build a list of files and
     *  call ilasm on each of them.
     */

    public void execute() 
        throws BuildException {
        if (_srcDir == null)
            _srcDir=project.resolveFile(".");

        //get dependencies list. 
        DirectoryScanner scanner = super.getDirectoryScanner(_srcDir);
        String[] dependencies = scanner.getIncludedFiles();
        log("assembling "+dependencies.length+" file"+((dependencies.length==1)?"":"s"));
        String baseDir=scanner.getBasedir().toString();
        //add to the command
        for (int i = 0; i < dependencies.length; i++) {
            String targetFile=dependencies[i];
            targetFile=baseDir+File.separator+targetFile;
            executeOneFile(targetFile);
        }
    
    } // end execute

    //=============================================================================	
    /** do the work by building the command line and then calling it
     */

    public void executeOneFile(String targetFile) 
        throws BuildException {
        NetCommand command=new NetCommand(this,exe_title,exe_name);
        command.setFailOnError(getFailFailOnError());
        //DEBUG helper
        command.setTraceCommandLine(true);
        //fill in args
        command.addArgument(getDebugParameter());
        command.addArgument(getTargetTypeParameter());
        command.addArgument(getListingParameter());
        command.addArgument(getOutputFileParameter());   
        command.addArgument(getOwnerParameter());
        command.addArgument(getResourceFileParameter());
        command.addArgument(getVerboseParameter());

 
        /* space for more argumentativeness
           command.addArgument();
           command.addArgument();
        */
    
        command.addArgument(targetFile);
        //now run the command of exe + settings + file
        command.runCommand();
    } // end executeOneFile



} //class
