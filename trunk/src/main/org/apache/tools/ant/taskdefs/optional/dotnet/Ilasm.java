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
/*
 *  build notes
 *  -The reference CD to listen to while editing this file is
 *  nap: Underworld  - Everything, Everything
 */

package org.apache.tools.ant.taskdefs.optional.dotnet;


import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;

/**
 * Assembles .NET Intermediate Language files.
 * ilasm.exe must be on the execute path, unless another executable
 * or the full path to that executable is specified in the <tt>executable</tt>
 * parameter
 *  <p>
 *
 *  <p>
 *
 *  All parameters are optional: &lt;il/&gt; should suffice to produce a debug
 *  build of all *.il files. The option set is roughly compatible with the
 *  CSharp class; even though the command line options are only vaguely
 *  equivalent. [The low level commands take things like /OUT=file, csc wants
 *  /out:file ... /verbose is used some places; /quiet here in ildasm... etc.]
 *  It would be nice if someone made all the command line tools consistent (and
 *  not as brittle as the java cmdline tools) <p>
 *  <p>
 *  The task is a directory based task, so attributes like <b>includes="*.il"
 *  </b> and <b>excludes="broken.il"</b> can be used to control the files pulled
 *  in. You can also use nested &lt;src&gt filesets to refer to source.
 * <p>
 *
 * @ant.task    name="ilasm" category="dotnet"
 */

public class Ilasm
         extends DotnetBaseMatchingTask {

    // CheckStyle:VisibilityModifier OFF - bc
    // CheckStyle:ConstantNameCheck OFF - bc
    /**
     *  Name of the executable. The .exe suffix is deliberately not included in
     *  anticipation of the unix version
     */
    protected static final String exe_name = "ilasm";

    /**
     *  what is the file extension we search on?
     */
    protected static final String file_ext = "il";

    /**
     *  and now derive the search pattern from the extension
     */
    protected static final String file_pattern = "**/*." + file_ext;

    /**
     *  title of task for external presentation
     */
    protected static final String exe_title = "ilasm";

    /**
     *  type of target. Should be one of exe|library|module|winexe|(null)
     *  default is exe; the actual value (if not null) is fed to the command
     *  line. <br>
     *  See /target
     */
    protected String targetType;

    /**
     *  verbose flag
     */
    protected boolean verbose;

    /**
     *  listing flag
     */

    protected boolean listing;

    /**
     *  resource file (.res format) to include in the app.
     */
    protected File resourceFile;

    /**
     *  flag to control action on execution trouble
     */
    protected boolean failOnError;

    /**
     *  debug flag. Controls generation of debug information.
     */
    protected boolean debug;

    /**
     *  file containing private key
     */

    private File keyfile;

    /**
     *  any extra command options?
     */
    protected String extraOptions;

    /**
     * filesets of references
     */
    protected Vector referenceFilesets = new Vector();

    // CheckStyle:ConstantNameCheck ON
    // CheckStyle:VisibilityModifier ON

    /**
     * @since Ant 1.7
     */
    private boolean isMono = !Os.isFamily("windows");

    /**
     *  constructor inits everything and set up the search pattern
     */
    public Ilasm() {
        Clear();
        setIncludes(file_pattern);
    }

    // CheckStyle:MethodNameCheck OFF - bc
    /**
     *  reset all contents.
     */
    public void Clear() {
        targetType = null;
        srcDir = null;
        listing = false;
        verbose = false;
        debug = true;
        outputFile = null;
        failOnError = true;
        resourceFile = null;
        extraOptions = null;
    }
    // CheckStyle:MethodNameCheck ON



    /**
     * Sets the type of target, either "exe" or "library".
     *
     *@param  targetType          one of exe|library|
     *@exception  BuildException  if target is not one of
     *      exe|library
     */
    public void setTargetType(String targetType)
             throws BuildException {
        this.targetType = targetType.toLowerCase();
        if (!targetType.equals("exe") && !targetType.equals("library")) {
            throw new BuildException("targetType " + targetType + " is not a valid type");
        }
    }


    /**
     *  accessor method for target type
     *
     *@return    the current target option
     */
    public String getTargetType() {
        return targetType;
    }


    /**
     *  g get the target type or null for no argument needed
     *
     *@return    The TargetTypeParameter value
     */

    protected String getTargetTypeParameter() {
        if (!notEmpty(targetType)) {
            return null;
        }
        if (targetType.equals("exe")) {
            return "/exe";
        } else if (targetType.equals("library")) {
            return "/dll";
        } else {
            return null;
        }
    }


    /**
     * Sets the Owner attribute.
     *
     * @param  s  The new Owner value
     * @ant.attribute ignore="true"
     */
    public void setOwner(String s) {
        log("This option is not supported by ILASM as of Beta-2, "
            + "and will be ignored", Project.MSG_WARN);
    }


    /**
     *  test for a string containing something useful
     *
     *@param  s       any string
     *@return         true if the argument is not null or empty
     */
    protected boolean notEmpty(String s) {
        return s != null && s.length() != 0;
    }


    /**
     *  If true, enable verbose ILASM output.
     *
     *@param  b  flag set to true for verbose on
     */
    public void setVerbose(boolean b) {
        verbose = b;
    }


    /**
     *  turn the verbose flag into a parameter for ILASM
     *
     *@return    null or the appropriate command line string
     */
    protected String getVerboseParameter() {
        return verbose ? null : "/quiet";
    }


    /**
     * If true, produce a listing (off by default).
     *
     *@param  b  flag set to true for listing on
     */
    public void setListing(boolean b) {
        listing = b;
    }


    /**
     *  turn the listing flag into a parameter for ILASM
     *
     *@return    the appropriate string from the state of the listing flag
     */
    protected String getListingParameter() {
        if (!isMono) {
            return listing ? "/listing" : "/nolisting";
        }
        return null;
    }


    /**
     * Set the output file; identical to setDestFile
     * @see DotnetBaseMatchingTask#setDestFile
     *@param  params  The new outputFile value
     */
    public void setOutputFile(File params) {
        outputFile = params;
    }


    /**
     *  get the output file
     *
     *@return    the argument string or null for no argument
     */
    protected String getOutputFileParameter() {
        if (outputFile == null) {
            return null;
        }
        return "/output=" + outputFile.toString();
    }


    /**
     * name of resource file to include.
     *
     * @param  fileName  path to the file. Can be relative, absolute, whatever.
     */
    public void setResourceFile(File fileName) {
        resourceFile = fileName;
    }


    /**
     *  Gets the resourceFileParameter attribute of the Ilasm task
     *
     *@return    The resourceFileParameter value
     */
    protected String getResourceFileParameter() {
        if (resourceFile != null) {
            return "/resource=" + resourceFile.toString();
        } else {
            return null;
        }
    }


    /**
     * If true, fails if ilasm tool fails.
     *
     *@param  b  The new failOnError value
     */
    public void setFailOnError(boolean b) {
        failOnError = b;
    }


    /**
     *  query fail on error flag
     *
     *@return    The failFailOnError value
     */
    public boolean getFailOnError() {
        return failOnError;
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
     *  get the argument or null for no argument needed
     *
     *@return    The debugParameter value
     */
    protected String getDebugParameter() {
        return debug ? "/debug" : null;
    }


    /**
     * the name of a file containing a private key.
     *
     *@param  keyfile  The new keyfile value
     */
    public void setKeyfile(File keyfile) {
        this.keyfile = keyfile;
    }


    /**
     *  get the argument or null for no argument needed
     *
     *@return    The keyfileParameter value
     */
    protected String getKeyfileParameter() {
        if (keyfile != null) {
            return "/keyfile:" + keyfile.toString();
        } else {
            return null;
        }
    }


    /**
     * Any extra options which are not explicitly
     * supported by this task.
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
     * set the target type to one of exe|library
     * @param targetType the enumerated value.
     */
    public void setTargetType(TargetTypes targetType) {
        this.targetType = targetType.getValue();
    }

    /**
     * Explicitly override the Mono auto-detection.
     *
     * <p>Defaults to false on Windows and true on any other platform.</p>
     * @param b a <code>boolean</code> value.
     * @since Ant 1.7
     */
    public void setMono(boolean b) {
        isMono = b;
    }

    /**
     *  This is the execution entry point. Build a list of files and call ilasm
     *  on each of them.
     *
     *@throws  BuildException  if the assembly failed and FailOnError is true
     */
    public void execute()
             throws BuildException {
        log("This task is deprecated and will be removed in a future version\n"
            + "of Ant.  It is now part of the .NET Antlib:\n"
            + "http://ant.apache.org/antlibs/dotnet/index.html",
            Project.MSG_WARN);
        NetCommand command = buildIlasmCommand();

        addFilesAndExecute(command, false);

    }
    // end execute


    /**
     * build up our ilasm command
     * @return
     */
    private NetCommand buildIlasmCommand() {
        NetCommand command = new NetCommand(this, exe_title, exe_name);
        command.setFailOnError(getFailOnError());
        //fill in args
        command.addArgument(getDebugParameter());
        command.addArgument(getTargetTypeParameter());
        command.addArgument(getListingParameter());
        command.addArgument(getOutputFileParameter());
        command.addArgument(getResourceFileParameter());
        command.addArgument(getVerboseParameter());
        command.addArgument(getKeyfileParameter());
        command.addArgument(getExtraOptionsParameter());

        /*
         *  space for more argumentativeness
         *  command.addArgument();
         *  command.addArgument();
         */
        return command;
    }

    /**
     * add a new reference fileset to the compilation
     * @param reference the fileset to use.
     */
    public void addReference(FileSet reference) {
        referenceFilesets.add(reference);
    }

    /**
     * test for a file being managed or not
     * @param file the file to test.
     * @return true if we think this is a managed executable, and thus OK
     * for linking
     * @todo look at the PE header of the exe and see if it is managed or not.
     */
    protected static boolean isFileManagedBinary(File file) {
        String filename = file.toString().toLowerCase();
        return filename.endsWith(".exe") || filename.endsWith(".dll")
                || filename.endsWith(".netmodule");
    }


    /**
     * Target types to build.
     * valid build types are exe|library|module|winexe
     */
    public static class TargetTypes extends EnumeratedAttribute {
        /** {@inheritDoc}. */
        public String[] getValues() {
            return new String[]{
                "exe",
                "library",
            };
        }
    }

}

