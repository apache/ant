/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional;





import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.Mkdir;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Instruments Java classes with <a href="http://www.reliable-systems.com/tools/">iContract<a>
 * DBC preprocessor.
 *
 * @author <a href="mailto:aslak.hellesoy@bekk.no">Aslak Hellesøy</a>
 *
 * <p/>
 * <table border="1" cellpadding="2" cellspacing="0">
 *   <tr>
 *     <td valign="top"><b>Attribute</b></td>
 *     <td valign="top"><b>Description</b></td>
 *     <td align="center" valign="top"><b>Required</b></td>
 *   </tr>
 *   <tr>
 *     <td valign="top">srcdir</td>
 *     <td valign="top">Location of the java files</td>
 *     <td valign="top" align="center">Yes</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">instrumentdir</td>
 *     <td valign="top">Indicates where the instrumented java and class files
 *       should go</td>
 *     <td valign="top" align="center">Yes</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">repositorydir</td>
 *     <td valign="top">Indicates where the repository java and class files should
 *       go</td>
 *     <td valign="top" align="center">Yes</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">pre</td>
 *     <td valign="top">Indicates whether or not to instrument for preconditions.
 *       Defaults to <code>true</code></td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">post</td>
 *     <td valign="top">Indicates whether or not to instrument for postconditions.
 *       Defaults to <code>true</code></td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">invariant</td>
 *     <td valign="top">Indicates whether or not to instrument for invariants.
 *       Defaults to <code>true</code></td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">failthrowable</td>
 *     <td valign="top">The full name of the Throwable (Exception) that should be
 *       thrown when an assertion is violated. Defaults to <code>java.lang.Error</code></td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">controlfile</td>
 *     <td valign="top">The name of the control file to pass to iContract. Default
 *       is to not pass a file</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">verbosity</td>
 *     <td valign="top">Indicates the verbosity level of iContract. Any combination
 *       of error*,warning*,note*,info*,progress*,debug* (comma separated) can be
 *       used. Defaults to <code>error*,warning*</code></td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 * </table>
 *
 * <p/>
 * <b>Note:</b> iContract will use the java compiler indicated by the project's
 * <code>build.compiler</code> property. See documentation for the Javac task for
 * more information.
 *
 * <p><b>Example:</b></p>
 *
 * <pre>
 * &lt;!-- =================================================================== -->
 * &lt;!-- Instruments source codes with iContract                             -->
 * &lt;!-- =================================================================== -->
 * &lt;target name="instrument" depends="compile">
 *   &lt;icontract
 *     srcdir="${build.src}"
 *     instrumentdir="${instrumented.dir}"
 *     repositorydir="${repository.dir}"
 *   >
 *     &lt;classpath>
 *       &lt;fileset dir="./lib">
 *         &lt;include name="*.jar"/>
 *       &lt;/fileset>
 *     &lt;/classpath>
 *   &lt;/icontract>
 * &lt;/target>
 * </pre>
 *
 */
public class IContract extends Task {

    /** \ on windows, / on linux/unix */
    private static final String ps = System.getProperty( "path.separator" );

    /** compiler to use for instrumenation */
    private String icCompiler = "javac";

    /** temporary file with file names of all java files to be instrumented */
    private File targets = null;

    /** will be set to true if any of the sourca files are newer than the instrumented files */
    private boolean dirty = false;

    /** set to true if the iContract jar is missing */
    private boolean iContractMissing = false;

    /** source file root */
    private File srcDir = null;

    /** instrumentation root */
    private File instrumentDir = null;

    /** repository root */
    private File repositoryDir = null;

    /** classpath */
    private Path classpath = null;

    /** The class of the Throwable to be thrown on failed assertions */
    private String failThrowable = "java.lang.Error";

    /** The -v option */
    private String verbosity = "error*,warning*";

    /** Indicates whether or not to use internal compilation */
    private boolean internalcompilation = false;

    /** The -m option */
    private File controlFile = null;

    /** Indicates whether or not to instrument for preconditions */
    private boolean pre = true;

    /** Indicates whether or not to instrument for postconditions */
    private boolean post = true;

    /** Indicates whether or not to instrument for invariants */
    private boolean invariant = true;

    /** Indicates whether or not to instrument all files regardless of timestamp */
    // can't be explicitly set, is set if control file exists and is newer than any source file
    private boolean instrumentall = true;

    /**
     * Sets the source directory
     *
     * @param srcDir the source directory
     */
    public void setSrcdir( File srcDir ) {
        this.srcDir = srcDir;
    }

    /**
     * Sets the instrumentation directory
     *
     * @param instrumentDir the source directory
     */
    public void setInstrumentdir( File instrumentDir ) {
        this.instrumentDir = instrumentDir;
    }

    /**
     * Sets the repository directory
     *
     * @param repositoryDir the source directory
     */
    public void setRepositorydir( File repositoryDir ) {
        this.repositoryDir = repositoryDir;
    }

    /**
     * Turns on/off precondition instrumentation
     *
     * @param pre true turns it on
     */
    public void setPre( boolean pre ) {
        this.pre = pre;
    }

    /**
     * Turns on/off postcondition instrumentation
     *
     * @param post true turns it on
     */
    public void setPost( boolean post ) {
        this.post = post;
    }

    /**
     * Turns on/off invariant instrumentation
     *
     * @param invariant true turns it on
     */
    public void setInvariant( boolean invariant ) {
        this.invariant = invariant;
    }

    /**
     * Sets the Throwable (Exception) to be thrown on assertion violation
     *
     * @param clazz the Throwable class
     */
    public void setFailthrowable( Class clazz ) {
        this.failThrowable = clazz.getName();
    }

    /**
     * Sets the verbosity level of iContract. Any combination of
     * error*,warning*,note*,info*,progress*,debug* (comma separated)
     * can be used. Defaults to error*,warning*
     *
     * @param clazz the Throwable class
     */
    public void setVerbosity( String verbosity ) {
        this.verbosity = verbosity;
    }

    /**
     * Turns on/off internal compilation.
     * <br/>
     * If set to true, Sun's javac will be run within the same VM as Ant.
     * <br/>
     * If set to false, the compiler indicated by the project property
     * <code>build.compiler</code> will be used, defaulting to javac,
     * and run in a separate VM.
     *
     * @param internalcompilation set to true for internal compilation
     */
    /* FIXME: Doesn't work
       public void setInternalcompilation( boolean internalcompilation ) {
       this.internalcompilation = internalcompilation;
       }
    */

    /**
     * Sets the control file to pass to iContract.
     *
     * @param clazz the Throwable class
     */
    public void setControlfile( File controlFile ) {
        this.controlFile = controlFile;
    }

    /**
     * Sets the classpath to be used for invocation of iContract.
     *
     * @path the classpath
     */
    public void setClasspath( Path path ) {
        createClasspath().append( path );
    }

    /**
     * Creates a nested classpath element
     *
     * @return the nested classpath element
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path( getProject() );
        }
        return classpath;
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     *
     * @param reference referenced classpath
     */
    public void setClasspathRef( Reference reference ) {
        createClasspath().setRefid( reference );
    }

    /**
     * Executes the task
     *
     * @exception BuildException if the instrumentation fails
     */
    public void execute() throws BuildException {
        preconditions();
        scan();
        if( dirty ) {
            // We want to be notified if iContract jar is missing. This makes life easier for the user
            // who didn't understand that iContract is a separate library (duh!)
            getProject().addBuildListener( new IContractPresenceDetector() );

            // Prepare the directories for iContract. iContract will make them if they
            // don't exist, but for some reason I don't know, it will complain about the REP files
            // afterwards
            Mkdir mkdir = (Mkdir) project.createTask( "mkdir" );
            mkdir.setDir( instrumentDir );
            mkdir.execute();
            mkdir.setDir( repositoryDir );
            mkdir.execute();

            // Set the compiler
            setCompiler();

            // Set the classpath that is needed for regular Javac compilation
            Path baseClasspath = createClasspath();

            // Create the classpath required to compile the sourcefiles BEFORE instrumentation
            Path beforeInstrumentationClasspath = ((Path) baseClasspath.clone());
            beforeInstrumentationClasspath.append( new Path( getProject(), srcDir.getAbsolutePath() ) );

            // Create the classpath required to compile the sourcefiles AFTER instrumentation
            Path afterInstrumentationClasspath = ((Path) baseClasspath.clone());
            afterInstrumentationClasspath.append( new Path( getProject(), instrumentDir.getAbsolutePath() ) );
            afterInstrumentationClasspath.append( new Path( getProject(), repositoryDir.getAbsolutePath() ) );
            afterInstrumentationClasspath.append( new Path( getProject(), srcDir.getAbsolutePath() ) );

            // Create the classpath required to automatically compile the repository files
            Path repositoryClasspath = ((Path) baseClasspath.clone());
            repositoryClasspath.append( new Path( getProject(), instrumentDir.getAbsolutePath() ) );
            repositoryClasspath.append( new Path( getProject(), srcDir.getAbsolutePath() ) );
            repositoryClasspath.append( new Path( getProject(), repositoryDir.getAbsolutePath() ) );

            // Create the classpath required for iContract itself
            Path iContractClasspath = ((Path) baseClasspath.clone());
            iContractClasspath.append( new Path( getProject(), System.getProperty( "java.home" ) + File.separator + ".." + File.separator + "lib" + File.separator + "tools.jar" ) );
            iContractClasspath.append( new Path( getProject(), srcDir.getAbsolutePath() ) );
            iContractClasspath.append( new Path( getProject(), repositoryDir.getAbsolutePath() ) );
            iContractClasspath.append( new Path( getProject(), instrumentDir.getAbsolutePath() ) );

            // Create a forked java process
            Java iContract = (Java) project.createTask( "java" );
            iContract.setTaskName( getTaskName() );
            iContract.setFork( true );
            iContract.setClassname( "com.reliablesystems.iContract.Tool" );
            iContract.setClasspath( iContractClasspath );

            // Build the arguments to iContract
            StringBuffer args = new StringBuffer();
            args.append( directiveString() );
            args.append( "-v" ).append( verbosity ).append( " " );
            args.append( "-b" ).append( icCompiler ).append( "\"" ).append( " -classpath " ).append( beforeInstrumentationClasspath ).append( "\" " );
            args.append( "-c" ).append( icCompiler ).append( "\"" ).append( " -classpath " ).append( afterInstrumentationClasspath ).append( "\" " );
            args.append( "-n" ).append( icCompiler ).append( "\"" ).append( " -classpath " ).append( repositoryClasspath ).append( "\" " );
            args.append( "-d" ).append( failThrowable ).append( " " );
            args.append( "-o" ).append( instrumentDir ).append( File.separator ).append( "@p" ).append( File.separator ).append( "@f.@e " );
            args.append( "-k" ).append( repositoryDir ).append( File.separator ).append( "@p " );
            args.append( instrumentall ? "-a " : "" ); // reinstrument everything if controlFile exists and is newer than source
            args.append( "@" ).append( targets.getName() );
            iContract.createArg().setLine( args.toString() );

// System.out.println( "JAVA -classpath " + iContractClasspath + " com.reliablesystems.iContract.Tool " + args.toString() );

            int result = iContract.executeJava();
            if( result != 0 ) {
                if( iContractMissing ) {
                    log( "iContract can't be found on your classpath. Your classpath is:" );
                    log( classpath.toString() );
                    log( "If you don't have the iContract jar, go get it at http://www.reliable-systems.com/tools/" );
                }
                throw new BuildException( "iContract instrumentation failed. Code=" + result );
            }
        } else {
            //log( "Nothing to do. Everything up to date." );
        }
    }

    /**
     * Checks that the required attributes are set.
     */
    private void preconditions() throws BuildException {
        if (srcDir == null) {
            throw new BuildException( "srcdir attribute must be set!", location );
        }
        if (!srcDir.exists()) {
            throw new BuildException( "srcdir \"" + srcDir.getPath() + "\" does not exist!", location );
        }
        if (instrumentDir == null) {
            throw new BuildException( "instrumentdir attribute must be set!", location );
        }
        if (repositoryDir == null) {
            throw new BuildException( "repositorydir attribute must be set!", location );
        }
    }

    /**
     * Verifies whether any of the source files have changed. Done by comparing date of source/class files.
     * The whole lot is "dirty" if at least one source file is newer than the instrumented files. If not dirty,
     * iContract will not be executed.
     * <br/>
     * Also creates a temporary file with a list of the source files, that will be deleted upon exit.
     */
    private void scan() throws BuildException {
        long now = (new Date()).getTime();

        FileSet fileset = new FileSet();
        fileset.setDefaultexcludes( true );
        fileset.setDir( srcDir );
        DirectoryScanner ds = fileset.getDirectoryScanner( project );

        String[] files = ds.getIncludedFiles();

        try {
            targets = File.createTempFile( "iContractTargets", "tmp", new File( System.getProperty( "user.dir" ) ) );
            targets.deleteOnExit();
            FileOutputStream fos = new FileOutputStream( targets );
            PrintStream ps = new PrintStream( fos );
            for (int i = 0; i < files.length; i++ ) {
                File srcFile = new File(srcDir, files[i]);
                if (files[i].endsWith(".java")) {
                    ps.println( srcFile.getAbsolutePath() );

                    File classFile = new File( instrumentDir, files[i].substring( 0, files[i].indexOf( ".java" ) ) + ".class" );

                    if (srcFile.lastModified() > now) {
                        log("Warning: file modified in the future: " +
                            files[i], Project.MSG_WARN);
                    }

                    if (!classFile.exists() || srcFile.lastModified() > classFile.lastModified()) {
                        //log( "Found a file newer than the instrumentDir class file: " + srcFile.getPath() + " newer than " + classFile.getPath() + ". Running iContract again..." );
                        dirty = true;
                    }
                }
            }
            ps.flush();
            ps.close();
        } catch( IOException e ) {
            throw new BuildException( "Could not create temporary file:" + e.getMessage() );
        }

        // also, check controlFile timestamp
        long controlFileTime = -1;
        if( controlFile != null ) {
            if( controlFile.exists() ) {
                controlFileTime = controlFile.lastModified();
                fileset.setDir( instrumentDir );
                ds = fileset.getDirectoryScanner( project );
                files = ds.getIncludedFiles();
                for( int i = 0; i < files.length; i++ ) {
                    File srcFile = new File(srcDir, files[i]);
                    if( files[i].endsWith( ".class" ) ) {
                        if( controlFileTime > srcFile.lastModified() ) {
                            if( !dirty ) {
                                log( "Control file " + controlFile.getAbsolutePath() + " has been updated. Instrumenting all files..." );
                            }
                            dirty = true;
                            instrumentall = true;
                        }
                    }
                }
            }
        }
    }


    /**
     * Creates the -m option based on the values of controlFile, pre, post and invariant.
     */
    private final String directiveString() {
        StringBuffer sb = new StringBuffer();
        boolean comma = false;
        if( (controlFile != null) || pre || post || invariant ) {
            sb.append( "-m" );
        }
        if(controlFile != null) {
            sb.append( "@" ).append( controlFile );
            comma = true;
        }
        if( pre ) {
            if( comma ) {
                sb.append( "," );
            }
            sb.append( "pre" );
            comma = true;
        }
        if( post ) {
            if( comma ) {
                sb.append( "," );
            }
            sb.append( "post" );
            comma = true;
        }
        if( invariant ) {
            if( comma ) {
                sb.append( "," );
            }
            sb.append( "inv" );
            comma = true;
        }
        sb.append( " " );
        return sb.toString();
    }

    /**
     * Sets the compiler as specified by the project's build.compiler property
     * If the internalcompilation attribute is set to true, Sun's javac
     * will be run from the same VM as Ant.
     *
     * NOTE: This has not been tested, as I only have JDK.
     */
    private void setCompiler() {
        if( !internalcompilation ) {
            String compiler = project.getProperty("build.compiler");
            if (compiler == null) {
                if (Project.getJavaVersion().startsWith("1.3")) {
                    compiler = "modern";
                } else {
                    compiler = "classic";
                }
            }

            if (compiler.equalsIgnoreCase("classic")) {
                icCompiler = "javac";
            } else if (compiler.equalsIgnoreCase("modern")) {
                icCompiler = "javac";
            } else if (compiler.equalsIgnoreCase("jikes")) {
                icCompiler = "jikes";
            } else if (compiler.equalsIgnoreCase("jvc")) {
                icCompiler = "jvc";
            } else {
                String msg = "Don't know how to use compiler " + compiler;
                throw new BuildException(msg, location);
            }
        } else {
            // This is how we tell iContract to use internal compiler
            // FIXME: Doesn't work
//                        icCompiler = ":";
        }
    }

    /**
     * BuildListener that sets the iContractMissing flag to true if a
     * message about missing iContract is missing. Used to indicate
     * a more verbose error to the user, with advice about how to solve
     * the problem
     */
    private class IContractPresenceDetector implements BuildListener {
        public void buildFinished(BuildEvent event) {}
        public void buildStarted(BuildEvent event) {}
        public void messageLogged(BuildEvent event) {
            if( "java.lang.NoClassDefFoundError: com/reliablesystems/iContract/Tool".equals( event.getMessage() ) ) {
                iContractMissing = true;
            }
        }
        public void targetFinished(BuildEvent event) {}
        public void targetStarted(BuildEvent event) {}
        public void taskFinished(BuildEvent event) {}
        public void taskStarted(BuildEvent event) {}
    }
}
