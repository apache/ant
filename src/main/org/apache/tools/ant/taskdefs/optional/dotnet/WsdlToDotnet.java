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
import java.util.Vector;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.util.FileUtils;

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
    private String srcFileName = null;

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
     * mono flag; we ignore the Rotor implementation of the CLR
     * @since Ant 1.7
     */
    private boolean isMono = !Os.isFamily("windows");


    /**
     * protocol string. Exact value set depends on SOAP stack version.
     * @since Ant 1.7
     */
    private String protocol = null;

    /**
     * should errors come in a machine parseable format. This
     * is WSE only.
     * @since Ant 1.7
     */
    private boolean parseableErrors = false;

    /**
     * filesets of file to compile
     * @since Ant 1.7
     */
    private Vector schemas = new Vector();

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
     * @param srcFileName name of WSDL file
     */
    public void setSrcFile(String srcFileName) {
        if (new File(srcFileName).isAbsolute()) {
            srcFileName = FileUtils.newFileUtils()
                .removeLeadingPath(getProject().getBaseDir(), 
                                   new File(srcFileName));;
        }
        this.srcFileName = srcFileName;
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
     * Explicitly override the Mono auto-detection.
     *
     * <p>Defaults to false on Windows and true on any other platform.</p>
     *
     * @since Ant 1.7
     */
    public void setMono(boolean b) {
        isMono = b;
    }


    /**
     * Should errors be machine parseable?
     * Optional, default=true
     *
     * @since Ant 1.7
     * @param parseableErrors
     */
    public void setParseableErrors(boolean parseableErrors) {
        this.parseableErrors = parseableErrors;
    }

    /**
     * what protocol to use. SOAP, SOAP1.2, HttpPost and HttpGet
     * are the base options. Different version and implementations may.
     * offer different options.
     * @since Ant 1.7
     *
     * @param protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * add a new source schema to the compilation
     * @since Ant 1.7
     *
     * @param source
     */
    public void addSchema(Schema source) {
        schemas.add(source);
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
        if (url != null && srcFileName != null) {
            throw new BuildException(
                    "you can not specify both a source file and a URL");
        }
        if (url == null && srcFileName == null) {
            throw new BuildException(
                    "you must specify either a source file or a URL");
        }
        if (srcFileName != null) {
            File srcFile = getProject().resolveFile(srcFileName);
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
        if(protocol!=null) {
            command.addArgument("/protocol:"+protocol);
        }
        if(parseableErrors) {
            command.addArgument("/parseableErrors");
        }
        command.addArgument(extraOptions);

        //set source and rebuild options
        boolean rebuild = true;
        long destLastModified = -1;
        if (srcFileName != null) {
            File srcFile = getProject().resolveFile(srcFileName);
            if (isMono) {
                // Mono 1.0's wsdl doesn't deal with absolute paths
                command.addArgument(srcFileName);
            } else {
                command.addArgument(srcFile.toString());
            }
            //rebuild unless the dest file is newer than the source file
            if ( destFile.exists() ) {
                destLastModified = destFile.lastModified();
            }
            if (srcFile.exists()
                && srcFile.lastModified() <= destLastModified) {
                rebuild = false;
            }
        } else {
            //no source file? must be a url, which has no dependency
            //handling
            rebuild = true;
            command.addArgument(url);
        }
        //add in any extra files.
        //this is an error in mono, but we do not warn on it as they may fix that outside
        //the ant build cycle.
        Iterator it=schemas.iterator();
        while ( it.hasNext() ) {
            Schema schema = (Schema) it.next();
            //get date, mark for a rebuild if we are newer
            long schemaTimestamp;
            schemaTimestamp=schema.getTimestamp();
            if(schemaTimestamp>destLastModified) {
                rebuild=true;
            }
            command.addArgument(schema.evaluate());
        }
        //conditionally compile
        if (rebuild) {
            command.runCommand();
        }
    }


    /**
     * nested schema class
     * Only supported on NET until mono add multi-URL handling on the command line
     */
    public static class Schema {
        private File file;
        private String url;
        public static final String ERROR_NONE_DECLARED = "One of file and url must be set";
        public static final String ERROR_BOTH_DECLARED = "Only one of file or url can be set";
        public static final String ERROR_FILE_NOT_FOUND = "Not found: ";

        public  void validate() {

            if(file!=null && !file.exists()) {
                throw new BuildException(ERROR_FILE_NOT_FOUND+file.toString());
            }
            if(file!=null && url!=null) {
                throw new BuildException(ERROR_BOTH_DECLARED);
            }
            if(file==null && url==null) {
                throw new BuildException(ERROR_NONE_DECLARED);
            }
        }

        /**
         * validate our settings then return either the url or the full file path.
         * @return
         */
        public String evaluate() {
            validate();
            if(file!=null) {
                return file.toString();
            } else {
                return getUrl();
            }
        }
        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * return the timestamp of a file, or -1 for a url (meaning we do not know its age)
         * @return
         */
        public long getTimestamp() {
            if(file!=null) {
                return file.lastModified();
            } else
                return -1;
        }
    }
}

