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
package org.apache.tools.ant.taskdefs.optional.dotnet;

import java.io.File;
import java.util.Vector;
import java.util.Iterator;
import java.net.MalformedURLException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
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
     * used for timestamp checking
     */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * name of output file (required)
     */
    private File destFile = null;

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

    // CheckStyle:VisibilityModifier OFF - bc
    /**
     *  any extra command options?
     */
    protected String extraOptions = null;

    // CheckStyle:VisibilityModifier ON


    /**
     * protocol string. Exact value set depends on SOAP stack version.
     * @since Ant 1.7
     */
    private String protocol = null;

    /**
     * should errors come in an IDE format. This
     * is WSE only.
     * @since Ant 1.7
     */
    private boolean ideErrors = false;

    /**
     * filesets of file to compile
     * @since Ant 1.7
     */
    private Vector schemas = new Vector();

    /**
     * our WSDL file.
     * @since ant1.7
     */
    private Schema wsdl = new Schema();

    /**
     * compiler
     * @since ant1.7
     */
    private Compiler compiler = null;

    /**
     * error message: dest file is a directory
     */
    public static final String ERROR_DEST_FILE_IS_DIR = "destination file is a directory";

    /**
     * error message: no dest file
     */
    public static final String ERROR_NO_DEST_FILE = "destination file must be specified";

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
        wsdl.setUrl(url);
    }

    /**
     * The local WSDL file to parse; either url or srcFile is required.
     * @param srcFile WSDL file
     */
    public void setSrcFile(File srcFile) {
        wsdl.setFile(srcFile);
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
     * Defines wether errors are machine parseable.
     * Optional, default=true
     *
     * @since Ant 1.7
     * @param ideErrors a <code>boolean</code> value.
     */
    public void setIdeErrors(boolean ideErrors) {
        this.ideErrors = ideErrors;
    }

    /**
     * what protocol to use. SOAP, SOAP1.2, HttpPost and HttpGet
     * are the base options. Different version and implementations may.
     * offer different options.
     * @since Ant 1.7
     *
     * @param protocol the protocol to use.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * add a new source schema to the compilation
     * @since Ant 1.7
     *
     * @param source a nested schema element.
     */
    public void addSchema(Schema source) {
        schemas.add(source);
    }

    /**
     * flag to trigger turning a filename into a file:url
     * ignored for the mono compiler.
     * @param b a <code>boolean</code> value.
     */
    public void setMakeURL(boolean b) {
        wsdl.setMakeURL(b);
    }

    /**
     * identify the compiler
     * @since Ant 1.7
     * @param compiler the enumerated value.
     */
    public void setCompiler(Compiler compiler) {
        this.compiler = compiler;
    }

    /**
     * validation code
     * @throws  BuildException  if validation failed
     */
    protected void validate()
            throws BuildException {
        if (destFile == null) {
            throw new BuildException(ERROR_NO_DEST_FILE);
        }
        if (destFile.isDirectory()) {
            throw new BuildException(
                    ERROR_DEST_FILE_IS_DIR);
        }
        wsdl.validate();
    }

    /**
     *  do the work by building the command line and then calling it
     *
     *@throws  BuildException  if validation or execution failed
     */
    public void execute()
             throws BuildException {
        log("This task is deprecated and will be removed in a future version\n"
            + "of Ant.  It is now part of the .NET Antlib:\n"
            + "http://ant.apache.org/antlibs/dotnet/index.html",
            Project.MSG_WARN);

        if (compiler == null) {
            compiler = Compiler.createDefaultCompiler();
        }
        validate();
        NetCommand command = new NetCommand(this,
                "WSDL",
                compiler.getCommand());
        command.setFailOnError(failOnError);
        //fill in args
        compiler.applyExtraArgs(command);
        command.addArgument("/nologo");
        command.addArgument("/out:" + destFile);
        command.addArgument("/language:", language);
        if (server) {
            command.addArgument("/server");
        }
        command.addArgument("/namespace:", namespace);
        if (protocol != null) {
            command.addArgument("/protocol:" + protocol);
        }
        if (ideErrors) {
            command.addArgument("/parsableErrors");
        }
        command.addArgument(extraOptions);

        //set source and rebuild options
        boolean rebuild = true;
        long destLastModified = -1;

        //rebuild unless the dest file is newer than the source file
        if (destFile.exists()) {
            destLastModified = destFile.lastModified();
            rebuild = isRebuildNeeded(wsdl, destLastModified);
        }
        String path;
        //mark for a rebuild if the dest file is newer
        path = wsdl.evaluate();
        if (!compiler.supportsAbsoluteFiles() && wsdl.getFile() != null) {
            // Mono 1.0's wsdl doesn't deal with absolute paths
            File f = wsdl.getFile();
            command.setDirectory(f.getParentFile());
            path = f.getName();
        }
        command.addArgument(path);
        //add in any extra files.
        //this is an error in mono, but we do not warn on it as they may fix that outside
        //the ant build cycle.
        Iterator it = schemas.iterator();
        while (it.hasNext()) {
            Schema schema = (Schema) it.next();
            //mark for a rebuild if we are newer
            rebuild |= isRebuildNeeded(schema, destLastModified);
            command.addArgument(schema.evaluate());
        }
        //conditionally compile
        if (rebuild) {
            command.runCommand();
        }
    }

    /**
     * checks for a schema being out of data
     * @param schema url/file
     * @param destLastModified timestamp, -1 for no dest
     * @return true if a rebuild is needed.
     */
    private boolean isRebuildNeeded(Schema schema, long destLastModified) {
        if (destLastModified == -1) {
            return true;
        }
        return !FILE_UTILS.isUpToDate(schema.getTimestamp(), destLastModified);
    }


    /**
     * nested schema class
     * Only supported on NET until mono add multi-URL handling on the command line
     */
    public static class Schema {
        private File file;
        private String url;
        private boolean makeURL = false;

        // Errors
        /** One of file or url must be set */
        public static final String ERROR_NONE_DECLARED = "One of file and url must be set";
        /** Only one of file or url */
        public static final String ERROR_BOTH_DECLARED = "Only one of file or url can be set";
        /** Not found */
        public static final String ERROR_FILE_NOT_FOUND = "Not found: ";
        /** File is a directory */
        public static final String ERROR_FILE_IS_DIR = "File is a directory: ";
        /** Could not URL convert */
        public static final String ERROR_NO_URL_CONVERT = "Could not URL convert ";

        /**
         * validate the schema
         */
        public  void validate() {

            if (file != null) {
                if (!file.exists()) {
                    throw new BuildException(ERROR_FILE_NOT_FOUND + file.toString());
                }
                if (file.isDirectory()) {
                    throw new BuildException(ERROR_FILE_IS_DIR + file.toString());
                }
            }
            if (file != null && url != null) {
                throw new BuildException(ERROR_BOTH_DECLARED);
            }
            if (file == null && url == null) {
                throw new BuildException(ERROR_NONE_DECLARED);
            }
        }

        /**
         * Validate our settings.
         * @return either the URL or the full file path
         */
        public String evaluate() {
            validate();
            if (url != null) {
                return getUrl();
            }
            if (makeURL) {
                try {
                    return file.toURL().toExternalForm();
                } catch (MalformedURLException e) {
                    throw new BuildException(ERROR_NO_URL_CONVERT + file);
                }
            }
            return file.toString();
        }

        /**
         * Get the file.
         * @return the file used.
         */
        public File getFile() {
            return file;
        }

        /**
         * name of a file to use as a source of WSDL or XSD data
         * @param file the file to use.
         */
        public void setFile(File file) {
            this.file = file;
        }

        /**
         * Get the url.
         * @return the URL of the resource.
         */
        public String getUrl() {
            return url;
        }

        /**
         * url of a resource.
         * URLs have no timestamp checking, and are not validated
         * @param url the URL string to use.
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * Get the makeURL attribute.
         * @return the attribute.
         */
        public boolean isMakeURL() {
            return makeURL;
        }

        /**
         * flag to request that a file is turned into an absolute file: URL
         * before being passed to the WSDL compiler
         * @param makeURL a <code>boolean</code> value.
         */
        public void setMakeURL(boolean makeURL) {
            this.makeURL = makeURL;
        }

        /**
         * Gets the file timestamp.
         * @return the timestamp of a file, or -1 for a URL (meaning we do not know its age)
         */
        public long getTimestamp() {
            if (file != null) {
                return file.lastModified();
            } else {
                return -1;
            }
        }
    }

    /**
     * The enumerated values for our compiler
     */
    public static class Compiler extends EnumeratedAttribute {

        /** microsoft */
        public static final String COMPILER_MS = "microsoft";
        /** mono */
        public static final String COMPILER_MONO = "mono";
        /** microsoft-on-mono */
        public static final String COMPILER_MS_ON_MONO = "microsoft-on-mono";
        // CheckStyle:VisibilityModifier OFF - bc
        /** the index to string mappings */
        String[] compilers = {
            COMPILER_MS,
            COMPILER_MONO,
            COMPILER_MS_ON_MONO
        };

        /** WSDL */
        public static final String EXE_WSDL = "wsdl";
        /** MONO */
        public static final String EXE_MONO = "mono";
        /**
         * programs to run
         */
        String[] compilerExecutables = {
            EXE_WSDL,
            EXE_WSDL,
            EXE_MONO
        };


        /**
         * extra things
         */
        String[][] extraCompilerArgs = {
            {},
            {},
            {EXE_WSDL + ".exe"}
        };

        boolean[] absoluteFiles = {
            true,
            false,
            true
        };

        // CheckStyle:VisibilityModifier ON
        /**
         * This is the only method a subclass needs to implement.
         *
         * @return an array holding all possible values of the enumeration.
         *         The order of elements must be fixed so that <tt>indexOfValue(String)</tt>
         *         always return the same index for the same value.
         */
        public String[] getValues() {
            return compilers;
        }

        /**
         * Create the default compiler for this platform.
         * @return the default compiler
         */
        public static Compiler createDefaultCompiler() {
            Compiler c = new Compiler();
            String compilerName;
            compilerName = Os.isFamily("windows") ? COMPILER_MS : COMPILER_MONO;
            c.setValue(compilerName);
            return c;
        }

        /**
         * return the command to run
         * @return the command
         */
        public String getCommand() {
            return compilerExecutables[getIndex()];
        }

        /**
         * return any extra arguments for the compiler
         * @return extra compiler arguments
         */
        public String[] getExtraArgs() {
            return extraCompilerArgs[getIndex()];
        }

        /**
         * Get where the current value supports absolute files.
         * @return true if the compiler does supports absolute files.
         */
        public boolean supportsAbsoluteFiles() {
            return absoluteFiles[getIndex()];
        }

        /**
         * apply any extra arguments of this class
         * @param command the command to apply the arguments to.
         */
        public void applyExtraArgs(NetCommand command) {
            String[] args = getExtraArgs();
            for (int i = 0; i < args.length; i++) {
               command.addArgument(args[i]);
            }
        }

    }

}
