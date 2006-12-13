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

import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;

/**
 * Task to take a .NET or Mono -generated managed executable and turn it
 * into ILASM assembly code. Useful when converting imported typelibs into
 * assembler before patching and recompiling, as one has to do when doing
 * advanced typelib work.
 * <p>
 * As well as generating the named output file, the ildasm program
 * will also generate resource files <code>Icons.resources</code>
 * <code>Message.resources</code> and a .res file whose filename stub is derived
 * from the source in ways to obscure to determine.
 * There is no way to control whether or not these files are created, or where they are created
 * (they are created in the current directory; their names come from inside the
 * executable and may be those used by the original developer). This task
 * creates the resources in the directory specified by <code>resourceDir</code> if
 * set, else in the same directory as the <code>destFile</code>.
 *
 * <p>
 * This task requires the .NET SDK installed and ildasm on the path.
 * To disassemble using alternate CLR systems, set the executable attribute
 * to the name/path of the alternate implementation -one that must
 * support all the classic ildasm commands.
 *
 * <p>
 * Dependency logic: the task executes the command if the output file is missing
 * or older than the source file. It does not take into account changes
 * in the options of the task, or timestamp differences in resource files.
 * When the underlying ildasm executable fails for some reason, it leaves the
 * .il file in place with some error message. To prevent this from confusing
 * the dependency logic, the file specified by the <code>dest</code>
 * attribute is <i>always</i> deleted after an unsuccessful build.
 * @ant.task category="dotnet"
 */
public class Ildasm extends Task {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * source file (mandatory)
     */
    private File sourceFile;

    /**
     * dest file (mandatory)
     */
    private File destFile;
    /**
     * progress bar switch
     */
    private boolean progressBar = false;

    /**
     * what is our encoding
     */
    private String encoding;

    /**
     * /bytes flag for byte markup
     */

    private boolean bytes = false;

    /**
     * line numbers? /linenum
     */
    private boolean linenumbers = false;

    /**
     * /raweh flag for raw exception handling
     */
    private boolean rawExceptionHandling = false;

    /**
     * show the source; /source
     */
    private boolean showSource = false;

    /**
     * /quoteallnames to quote all names
     */
    private boolean quoteallnames = false;

    /**
     * /header for header information
     */
    private boolean header = false;

    /**
     * when false, sets the /noil attribute
     * to suppress assembly info
     */
    private boolean assembler = true;

    /**
     * include metadata
     * /tokens
     */

    private boolean metadata = false;

    /**
     * what visibility do we want.
     *
     */
    private String visibility;

    /**
     * specific item to disassemble
     */

    private String item;

    /**
     * override for the executable
     */
    private String executable = "ildasm";

    /**
     *  name of the directory for resources to be created. We cannot control
     * their names, but we can say where they get created. If not set, the
     * directory of the dest file is used
     */
    private File resourceDir;


    /**
     * Set the name of the directory for resources to be created. We cannot control
     * their names, but we can say where they get created. If not set, the
     * directory of the dest file is used
     * @param resourceDir the directory in which to create resources.
     */
    public void setResourceDir(File resourceDir) {
        this.resourceDir = resourceDir;
    }

    /**
     * override the name of the executable (normally ildasm) or set
     * its full path. Do not set a relative path, as the ugly hacks
     * needed to create resource files in the dest directory
     * force us to change to this directory before running the application.
     * i.e use &lt;property location&gt to create an absolute path from a
     * relative one before setting this value.
     * @param executable the name of the executable to use.
     */
    public void setExecutable(String executable) {
        this.executable = executable;
    }

    /**
     * Select the output encoding: ascii, utf8 or unicode
     * @param encoding the enumerated value.
     */
    public void setEncoding(EncodingTypes encoding) {
        this.encoding = encoding.getValue();
    }

    /**
     * enable (default) or disable assembly language in the output
     * @param assembler a <code>boolean</code> value.
     */
    public void setAssembler(boolean assembler) {
        this.assembler = assembler;
    }

    /**
     * enable or disable (default) the original bytes as comments
     * @param bytes a <code>boolean</code> value.
     */
    public void setBytes(boolean bytes) {
        this.bytes = bytes;
    }

    /**
     * the output file (required)
     * @param destFile the destination file.
     */
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    /**
     * include header information; default false.
     * @param header a <code>boolean</code> value.
     */
    public void setHeader(boolean header) {
        this.header = header;
    }

    /**
     * name a single item to decode; a class or a method
     * e.g item="Myclass::method" or item="namespace1::namespace2::Myclass:method(void(int32))
     * @param item the item to decode.
     */
    public void setItem(String item) {
        this.item = item;
    }

    /**
     * include line number information; default=false
     * @param linenumbers a <code>boolean</code> value.
     */
    public void setLinenumbers(boolean linenumbers) {
        this.linenumbers = linenumbers;
    }

    /**
     * include metadata information
     * @param metadata a <code>boolean</code> value.
     */
    public void setMetadata(boolean metadata) {
        this.metadata = metadata;
    }

    /**
     * show a graphical progress bar in a window during the process; off by default
     * @param progressBar a <code>boolean</code> value.
     */
    public void setProgressBar(boolean progressBar) {
        this.progressBar = progressBar;
    }

    /**
     * quote all names.
     * @param quoteallnames a <code>boolean</code> value.
     */
    public void setQuoteallnames(boolean quoteallnames) {
        this.quoteallnames = quoteallnames;
    }

    /**
     * enable raw exception handling (default = false)
     * @param rawExceptionHandling a <code>boolean</code> value.
     */
    public void setRawExceptionHandling(boolean rawExceptionHandling) {
        this.rawExceptionHandling = rawExceptionHandling;
    }

    /**
     * include the source as comments (default=false)
     * @param showSource a <code>boolean</code> value.
     */
    public void setShowSource(boolean showSource) {
        this.showSource = showSource;
    }

    /**
     * the file to disassemble -required
     * @param sourceFile the file to disassemble.
     */
    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    /**
     * alternate name for sourceFile
     * @param sourceFile the source file.
     */
    public void setSrcFile(File sourceFile) {
        setSourceFile(sourceFile);
    }
    /**
     * This method sets the visibility options. It chooses one
     * or more of the following, with + signs to concatenate them:
     * <pre>
     * pub : Public
     * pri : Private
     * fam : Family
     * asm : Assembly
     * faa : Family and Assembly
     * foa : Family or Assembly
     * psc : Private Scope
     *</pre>
     * e.g. visibility="pub+pri".
     * Family means <code>protected</code> in C#;
     * @param visibility the options to use.
     */
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    /**
     *  verify that source and dest are ok
     */
    private void validate() {
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            throw new BuildException("invalid source");
        }
        if (destFile == null || destFile.isDirectory()) {
            throw new BuildException("invalid dest");
        }
        if (resourceDir != null
                && (!resourceDir.exists() || !resourceDir.isDirectory())) {
            throw new BuildException("invalid resource directory");
        }
    }

    /**
     * Test for disassembly being needed; use existence and granularity
     * correct date stamps
     * @return true iff a rebuild is required.
     */
    private boolean isDisassemblyNeeded() {
        if (!destFile.exists()) {
            log("Destination file does not exist: a build is required",
                    Project.MSG_VERBOSE);
            return true;
        }
        long sourceTime = sourceFile.lastModified();
        long destTime = destFile.lastModified();
        if (sourceTime > (destTime + FILE_UTILS.getFileTimestampGranularity())) {
            log("Source file is newer than the dest file: a rebuild is required",
                    Project.MSG_VERBOSE);
            return true;
        } else {
            log("The .il file is up to date", Project.MSG_VERBOSE);
            return false;
        }

    }
    /**
     * do the work
     * @throws BuildException if there is an error.
     */
    public void execute() throws BuildException {
        log("This task is deprecated and will be removed in a future version\n"
            + "of Ant.  It is now part of the .NET Antlib:\n"
            + "http://ant.apache.org/antlibs/dotnet/index.html",
            Project.MSG_WARN);
        validate();
        if (!isDisassemblyNeeded()) {
            return;
        }
        NetCommand command = new NetCommand(this, "ildasm", executable);
        command.setFailOnError(true);
        //fill in args
        command.addArgument("/text");
        command.addArgument("/out=" + destFile.toString());
        if (!progressBar) {
            command.addArgument("/nobar");
        }
        if (linenumbers) {
            command.addArgument("/linenum");
        }
        if (showSource) {
            command.addArgument("/source");
        }
        if (quoteallnames) {
            command.addArgument("/quoteallnames");
        }
        if (header) {
            command.addArgument("/header");
        }
        if (!assembler) {
            command.addArgument("/noil");
        }
        if (metadata) {
            command.addArgument("/tokens");
        }
        command.addArgument("/item:", item);
        if (rawExceptionHandling) {
            command.addArgument("/raweh");
        }
        command.addArgument(EncodingTypes.getEncodingOption(encoding));
        if (bytes) {
            command.addArgument("/bytes");
        }
        command.addArgument("/vis:", visibility);

        //add the source file
        command.addArgument(sourceFile.getAbsolutePath());

        //determine directory: resourceDir if set,
        //the dir of the destFile if not
        File execDir = resourceDir;
        if (execDir == null) {
            execDir = destFile.getParentFile();
        }
        command.setDirectory(execDir);

        //now run
        try {
            command.runCommand();
        } catch (BuildException e) {
            //forcibly delete the output file in case of trouble
            if (destFile.exists()) {
                log("Deleting destination file as it may be corrupt");
                destFile.delete();
            }
            //then rethrow the exception
            throw e;
        }

    }

    /**
     * encoding options; the default is ascii
     */
    public static class EncodingTypes extends EnumeratedAttribute {
        /** Unicode */
        public static final String UNICODE = "unicode";
        /** UTF8 */
        public static final String UTF8 = "utf8";
        /** ASCII */
        public static final String ASCII = "ascii";
        /** {@inheritDoc}. */
        public String[] getValues() {
            return new String[]{
                ASCII,
                UTF8,
                UNICODE,
            };
        }

        /**
         * This method maps from an encoding enum to an encoding option.
         * @param enumValue the value to use.
         * @return The encoding option indicated by the enum value.
         */
        public static String getEncodingOption(String enumValue) {
            if (UNICODE.equals(enumValue)) {
                return "/unicode";
            }
            if (UTF8.equals(enumValue)) {
                return "/utf8";
            }
            return null;
        }
    }

    /**
     * visibility options for decoding
     */
    public static class VisibilityOptions extends EnumeratedAttribute {
        /** {@inheritDoc}. */
        public String[] getValues() {
            return new String[]{
                "pub", //Public
                "pri", //Private
                "fam", //Family
                "asm", //Assembly
                "faa", //Family and Assembly
                "foa", //Family or Assembly
                "psc", //Private Scope
            };
        }

    }
}
