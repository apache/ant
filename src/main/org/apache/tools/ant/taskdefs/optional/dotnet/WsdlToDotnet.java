/*
 * Copyright  2002-2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.dotnet;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Converts a WSDL file or URL resource into a .NET language.
 *
 * Why add a wrapper to the MS WSDL tool?
 * So that you can verify that your web services, be they written with Axis or
 *anyone else's SOAP toolkit, work with .NET clients.
 *
 *This task is dependency aware when using a file as a source and destination;
 *so if you &lt;get&gt; the file (with <code>usetimestamp="true"</code>) then
 *you only rebuild stuff when the WSDL file is changed. Of course,
 *if the server generates a new timestamp every time you ask for the WSDL,
 *this is not enough...use the &lt;filesmatch&gt; &lt;condition&gt; to
 *to byte for byte comparison against a cached WSDL file then make
 *the target conditional on that test failing.

 * See "Creating an XML Web Service Proxy", "wsdl.exe" docs in
 * the framework SDK documentation
 * @version     0.5
 * @ant.task    category="dotnet"
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
     * Whether or not a failure should halt the build.
     * Optional - default is <code>true</code>.
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
        if (srcFile != null) {
            command.addArgument(srcFile.toString());
            //rebuild unless the dest file is newer than the source file
            if (srcFile.exists() && destFile.exists()
                && srcFile.lastModified() <= destFile.lastModified()) {
                rebuild = false;
            }
        } else {
            //no source file? must be a url, which has no dependency
            //handling
            rebuild = true;
            command.addArgument(url);
        }
        if (rebuild) {
            command.runCommand();
        }
    }
}

