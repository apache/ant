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
package org.apache.tools.ant.taskdefs.optional.metamata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

/**
 * Invokes the Metamata Audit/ Webgain Quality Analyzer on a set of Java files.
 * <p>
 * <i>maudit</i> performs static analysis of the Java source code and byte code files to find and report
 * errors of style and potential problems related to performance, maintenance and robustness.
 *  As a convenience, a stylesheet is given in <tt>etc</tt> directory, so that an HTML report 
 * can be generated from the XML file.
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class MAudit extends AbstractMetamataTask {

    /* As of Metamata 2.0, the command line of MAudit is as follows:
    Usage
        maudit <option>... <path>... [-unused <search-path>...]

    Parameters
        path               File or directory to audit.
        search-path        File or directory to search for declaration uses.

    Options
        -arguments  -A     <file>     Includes command line arguments from file.
        -classpath  -cp    <path>     Sets class path (also source path unless one
                                      explicitly set). Overrides METAPATH/CLASSPATH.
        -exit       -x                Exits after the first error.
        -fix        -f                Automatically fixes certain errors.
        -fullpath                     Prints full path for locations.
        -help       -h                Prints help and exits.
        -list       -l                Creates listing file for each audited file.
        -offsets    -off              Offset and length for locations.
        -output     -o     <file>     Prints output to file.
        -quiet      -q                Suppresses copyright and summary messages.
        -sourcepath        <path>     Sets source path. Overrides SOURCEPATH.
        -tab        -t                Prints a tab character after first argument.
        -unused     -u                Finds declarations unused in search paths.
        -verbose    -v                Prints all messages.
        -version    -V                Prints version and exits.
    */

    //---------------------- PUBLIC METHODS ------------------------------------

    /** pattern used by maudit to report the error for a file */
    /** RE does not seems to support regexp pattern with comments so i'm stripping it*/
    // (?:file:)?((?#filepath).+):((?#line)\\d+)\\s*:\\s+((?#message).*)
    static final String AUDIT_PATTERN = "(?:file:)?(.+):(\\d+)\\s*:\\s+(.*)";

    private File outFile = null;

    private Path searchPath = null;

    private Path rulesPath = null;

    private boolean fix = false;

    private boolean list = false;

    private boolean unused = false;

//  add a bunch of undocumented options for the task
    private boolean quiet = false;
    private boolean exit = false;
    private boolean offsets = false;
    private boolean verbose = false;
    private boolean fullsemanticize = false;

    /** default constructor */
    public MAudit() {
        super("com.metamata.gui.rc.MAudit");
    }

    /** 
     * The XML file to which the Audit result should be written to; required
     */
     
    public void setTofile(File outFile) {
        this.outFile = outFile;
    }

    /**
     * Automatically fix certain errors 
     * (those marked as fixable in the manual);
     * optional, default=false
     */
    public void setFix(boolean flag) {
        this.fix = flag;
    }

    /**
     * Creates listing file for each audited file; optional, default false. 
     * When set, a .maudit file will be generated in the
     * same location as the source file.
     */
    public void setList(boolean flag) {
        this.list = flag;
    }

    /**
     * Finds declarations unused in search paths; optional, default false. 
     * It will look for unused global declarations
     * in the source code within a use domain specified by the 
     * <tt>searchpath</tt> element.
     */
    public void setUnused(boolean flag) {
        this.unused = flag;
    }

    /**
     * flag to suppress copyright and summary messages; default false.
     * internal/testing only
     * @ant.attribute ignore="true"
     */
    public void setQuiet(boolean flag) {
        this.quiet = flag;
    }

    /**
     * flag to tell the task to exit after the first error. 
     * internal/testing only
     * @ant.attribute ignore="true"
     */
    public void setExit(boolean flag) {
        this.exit = flag;
    }

    /**
     * internal/testing only
     * @ant.attribute ignore="true"
     */
    public void setOffsets(boolean flag) {
        this.offsets = flag;
    }

    /**
     * flag to print all messages; optional, default false.
     * internal/testing only
     * @ant.attribute ignore="true"
     */
    public void setVerbose(boolean flag) {
        this.verbose = flag;
    }

    /**
     * internal/testing only
     * @ant.attribute ignore="true"
     */
    public void setFullsemanticize(boolean flag) {
        this.fullsemanticize = flag;
    }

    /** 
     * classpath for additional audit rules
     * these must be placed before metamata.jar !! 
     */
    public Path createRulespath() {
        if (rulesPath == null) {
            rulesPath = new Path(getProject());
        }
        return rulesPath;
    }

    /** 
     * search path to use for unused global declarations; 
     * required when <tt>unused</tt> is set. 
     */
    public Path createSearchpath() {
        if (searchPath == null) {
            searchPath = new Path(getProject());
        }
        return searchPath;
    }

    /**
     * create the option vector for the command
     */
    protected Vector getOptions() {
        Vector options = new Vector(512);
        // add the source path automatically from the fileset.
        // to avoid redundancy...
        for (int i = 0; i < fileSets.size(); i++) {
            FileSet fs = (FileSet) fileSets.elementAt(i);
            Path path = createSourcepath();
            File dir = fs.getDir(getProject());
            path.setLocation(dir);
        }

        // there is a bug in Metamata 2.0 build 37. The sourcepath argument does
        // not work. So we will use the sourcepath prepended to classpath. (order
        // is important since Metamata looks at .class and .java)
        if (sourcePath != null) {
            sourcePath.append(classPath); // srcpath is prepended
            classPath = sourcePath;
            sourcePath = null; // prevent from using -sourcepath
        }

        // don't forget to modify the pattern if you change the options reporting
        if (classPath != null) {
            options.addElement("-classpath");
            options.addElement(classPath.toString());
        }
        // suppress copyright msg when running, we will let it so that this
        // will be the only output to the console if in xml mode
        if (quiet) {
            options.addElement("-quiet");
        }
        if (fullsemanticize) {
            options.addElement("-full-semanticize");
        }
        if (verbose) {
            options.addElement("-verbose");
        }
        if (offsets) {
            options.addElement("-offsets");
        }
        if (exit) {
            options.addElement("-exit");
        }
        if (fix) {
            options.addElement("-fix");
        }
        options.addElement("-fullpath");

        // generate .maudit files much more detailed than the report
        // I don't like it very much, I think it could be interesting
        // to get all .maudit files and include them in the XML.
        if (list) {
            options.addElement("-list");
        }
        if (sourcePath != null) {
            options.addElement("-sourcepath");
            options.addElement(sourcePath.toString());
        }
        addAllVector(options, includedFiles.keys());
        if (unused) {
            options.addElement("-unused");
            options.addElement(searchPath.toString());
        }
        return options;
    }

    /**
     * validate the settings
     */
    protected void checkOptions() throws BuildException {
        super.checkOptions();
        if (unused && searchPath == null) {
            throw new BuildException("'searchpath' element must be set when looking for 'unused' declarations.");
        }
        if (!unused && searchPath != null) {
            log("'searchpath' element ignored. 'unused' attribute is disabled.", Project.MSG_WARN);
        }
        if (rulesPath != null) {
            cmdl.createClasspath(getProject()).addExisting(rulesPath);
        }
    }

    protected ExecuteStreamHandler createStreamHandler() throws BuildException {
        // if we didn't specify a file, then use a screen report
        if (outFile == null) {
            return new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_ERR);
        }
        ExecuteStreamHandler handler = null;
        OutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
            handler = new MAuditStreamHandler(this, out);
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            if (out == null){
                try {
                    out.close();
                } catch (IOException e){
                }
            }
        }
        return handler;
    }

    protected void cleanUp() throws BuildException {
        super.cleanUp();
        // at this point if -list is used, we should move
        // the .maudit file since we cannot choose their location :(
        // the .maudit files match the .java files
        // we'll use includedFiles to get the .maudit files.

        /*if (out != null){
            // close it if not closed by the handler...
        }*/
    }

}

