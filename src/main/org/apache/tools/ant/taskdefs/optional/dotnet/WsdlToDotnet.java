/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import java.io.File;

/**
 * Converts a WSDL file or URL resource into a .NET language.
 *
 * See "Creating an XML Web Service Proxy", "wsdl.exe" docs in
 * the framework SDK documentation
 * @author      Steve Loughran steve_l@iseran.com
 * @version     0.5
 * @ant.task    name="wsdltodotnet" category="dotnet"
 * @since       Ant 1.5
 */

public class WsdlToDotnet extends Task  { 
    
    /**
     * name of output file (required)
     */ 
    private File destFile = null;
    
    /**
     * url to retrieve
     */ 
    private String url = null;
    
    /**
     * name of source file
     */ 
    private File srcFile = null;
    
    /**
     * language; defaults to C#
     */ 
    private String language = "CS";
    
    /**
     * flag set to true to generate server side skeleton
     */ 
    private boolean server = false;
    
    /**
     * namespace
     */ 
    private String namespace = null;
    
    /**
     *  flag to control action on execution trouble
     */
    private boolean failOnError = true;

    /**
     *  any extra command options?
     */
    protected String extraOptions = null;
    
    /**
     * Name of the file to generate. Required
     * @param destFile filename
     */
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    /**
     * Sets the URL to fetch. Fetching is by wsdl.exe; Ant proxy settings
     * are ignored; either url or srcFile is required.
     * @param url url to save
     */

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * The local WSDL file to parse; either url or srcFile is required.
     * @param srcFile name of WSDL file
     */
    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }

    /**
     * set the language; one of "CS", "JS", or "VB"
     * optional, default is CS for C# source
     * @param language language to generate
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * flag to enable server side code generation;
     * optional, default=false
     * @param server server-side flag
     */

    public void setServer(boolean server) {
        this.server = server;
    }

    /**
     * namespace to place  the source in.
     * optional; default ""
     * @param namespace new namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Should failure halt the build? optional, default=true
     * @param failOnError new failure option
     */
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     *  Any extra WSDL.EXE options which aren't explicitly
     *  supported by the ant wrapper task; optional
     *
     *@param  extraOptions  The new ExtraOptions value
     */
    public void setExtraOptions(String extraOptions) {
        this.extraOptions = extraOptions;
    }
    
    /**
     * validation code
     * @throws  BuildException  if validation failed
     */ 
    protected void validate() 
            throws BuildException {
        if (destFile == null) {
            throw new BuildException("destination file must be specified");
        }
        if (destFile.isDirectory()) {
            throw new BuildException(
                "destination file is a directory");
        }        
        if (url != null && srcFile != null) {
            throw new BuildException(
                    "you can not specify both a source file and a URL");
        }
        if (url == null && srcFile == null) {
            throw new BuildException(
                    "you must specify either a source file or a URL");
        }
        if (srcFile != null) {
            if (!srcFile.exists()) {
                throw new BuildException(
                    "source file does not exist");
            }
            if (srcFile.isDirectory()) {
                throw new BuildException(
                    "source file is a directory");
            }
        }

    }

    /**
     *  do the work by building the command line and then calling it
     *
     *@throws  BuildException  if validation or execution failed
     */
    public void execute()
             throws BuildException {
        validate();
        NetCommand command = new NetCommand(this, "WSDL", "wsdl");
        command.setFailOnError(failOnError);
        //DEBUG helper
        command.setTraceCommandLine(true);
        //fill in args
        command.addArgument("/nologo");
        command.addArgument("/out:" + destFile);
        command.addArgument("/language:", language);
        if (server) {
            command.addArgument("/server");
        }
        command.addArgument("/namespace:", namespace);
        command.addArgument(extraOptions);

        //set source and rebuild options
        boolean rebuild = true;
        if(srcFile!=null) {
            command.addArgument(srcFile.toString());
            //rebuild unless the dest file is newer than the source file
            if (srcFile.exists() && destFile.exists() &&
                srcFile.lastModified() <= destFile.lastModified()) {
                rebuild = false;
            }
        } else {
            //no source file? must be a url, which has no dependency
            //handling
            rebuild=true;
            command.addArgument(url);
        }
        if (rebuild) {
            command.runCommand();
        }
    }
}

