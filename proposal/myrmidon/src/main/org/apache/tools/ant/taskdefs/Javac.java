/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.JavaVersion;
import org.apache.aut.nativelib.Os;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapterFactory;
import org.apache.tools.ant.types.Argument;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.SourceFileScanner;

/**
 * Task to compile Java source files. This task can take the following
 * arguments:
 * <ul>
 *   <li> sourcedir
 *   <li> destdir
 *   <li> deprecation
 *   <li> classpath
 *   <li> bootclasspath
 *   <li> extdirs
 *   <li> optimize
 *   <li> debug
 *   <li> encoding
 *   <li> target
 *   <li> depend
 *   <li> vebose
 *   <li> failonerror
 *   <li> includeantruntime
 *   <li> includejavaruntime
 *   <li> source
 * </ul>
 * Of these arguments, the <b>sourcedir</b> and <b>destdir</b> are required. <p>
 *
 * When this task executes, it will recursively scan the sourcedir and destdir
 * looking for Java source files to compile. This task makes its compile
 * decision based on timestamp.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com
 *      </a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 */

public class Javac extends MatchingTask
{
    private final static String FAIL_MSG
        = "Compile failed, messages should have been provided.";
    private boolean debug = false;
    private boolean optimize = false;
    private boolean deprecation = false;
    private boolean depend = false;
    private boolean verbose = false;
    private boolean includeAntRuntime = true;
    private boolean includeJavaRuntime = false;
    private String fork = "false";
    private String forkedExecutable = null;
    private boolean nowarn = false;
    private ArrayList implementationSpecificArgs = new ArrayList();

    protected boolean failOnError = true;
    protected File[] compileList = new File[ 0 ];
    private Path bootclasspath;
    private Path compileClasspath;
    private String debugLevel;
    private File destDir;
    private String encoding;
    private Path extdirs;
    private String memoryInitialSize;
    private String memoryMaximumSize;

    private String source;

    private Path src;
    private String target;

    /**
     * Sets the bootclasspath that will be used to compile the classes against.
     *
     * @param bootclasspath The new Bootclasspath value
     */
    public void setBootclasspath( Path bootclasspath )
        throws TaskException
    {
        if( this.bootclasspath == null )
        {
            this.bootclasspath = bootclasspath;
        }
        else
        {
            this.bootclasspath.append( bootclasspath );
        }
    }

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( Path classpath )
        throws TaskException
    {
        if( compileClasspath == null )
        {
            compileClasspath = classpath;
        }
        else
        {
            compileClasspath.append( classpath );
        }
    }

    /**
     * Set the debug flag.
     *
     * @param debug The new Debug value
     */
    public void setDebug( boolean debug )
    {
        this.debug = debug;
    }

    /**
     * Set the value of debugLevel.
     *
     * @param v Value to assign to debugLevel.
     */
    public void setDebugLevel( String v )
    {
        this.debugLevel = v;
    }

    /**
     * Set the depend flag.
     *
     * @param depend The new Depend value
     */
    public void setDepend( boolean depend )
    {
        this.depend = depend;
    }

    /**
     * Set the deprecation flag.
     *
     * @param deprecation The new Deprecation value
     */
    public void setDeprecation( boolean deprecation )
    {
        this.deprecation = deprecation;
    }

    /**
     * Set the destination directory into which the Java source files should be
     * compiled.
     *
     * @param destDir The new Destdir value
     */
    public void setDestdir( File destDir )
    {
        this.destDir = destDir;
    }

    /**
     * Set the Java source file encoding name.
     *
     * @param encoding The new Encoding value
     */
    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }

    /**
     * Sets the extension directories that will be used during the compilation.
     *
     * @param extdirs The new Extdirs value
     */
    public void setExtdirs( Path extdirs )
        throws TaskException
    {
        if( this.extdirs == null )
        {
            this.extdirs = extdirs;
        }
        else
        {
            this.extdirs.append( extdirs );
        }
    }

    /**
     * Throw a TaskException if compilation fails
     *
     * @param fail The new Failonerror value
     */
    public void setFailonerror( boolean fail )
    {
        failOnError = fail;
    }

    /**
     * Sets whether to fork the javac compiler.
     *
     * @param f "true|false|on|off|yes|no" or the name of the javac executable.
     */
    public void setFork( String f )
    {
        if( f.equalsIgnoreCase( "on" )
            || f.equalsIgnoreCase( "true" )
            || f.equalsIgnoreCase( "yes" ) )
        {
            fork = "true";
            forkedExecutable = getSystemJavac();
        }
        else if( f.equalsIgnoreCase( "off" )
            || f.equalsIgnoreCase( "false" )
            || f.equalsIgnoreCase( "no" ) )
        {
            fork = "false";
            forkedExecutable = null;
        }
        else
        {
            fork = "true";
            forkedExecutable = f;
        }
    }

    /**
     * Include ant's own classpath in this task's classpath?
     *
     * @param include The new Includeantruntime value
     */
    public void setIncludeantruntime( boolean include )
    {
        includeAntRuntime = include;
    }

    /**
     * Sets whether or not to include the java runtime libraries to this task's
     * classpath.
     *
     * @param include The new Includejavaruntime value
     */
    public void setIncludejavaruntime( boolean include )
    {
        includeJavaRuntime = include;
    }

    /**
     * Set the memoryInitialSize flag.
     *
     * @param memoryInitialSize The new MemoryInitialSize value
     */
    public void setMemoryInitialSize( String memoryInitialSize )
    {
        this.memoryInitialSize = memoryInitialSize;
    }

    /**
     * Set the memoryMaximumSize flag.
     *
     * @param memoryMaximumSize The new MemoryMaximumSize value
     */
    public void setMemoryMaximumSize( String memoryMaximumSize )
    {
        this.memoryMaximumSize = memoryMaximumSize;
    }

    /**
     * Sets whether the -nowarn option should be used.
     *
     * @param flag The new Nowarn value
     */
    public void setNowarn( boolean flag )
    {
        this.nowarn = flag;
    }

    /**
     * Set the optimize flag.
     *
     * @param optimize The new Optimize value
     */
    public void setOptimize( boolean optimize )
    {
        this.optimize = optimize;
    }

    /**
     * Proceed if compilation fails
     *
     * @param proceed The new Proceed value
     */
    public void setProceed( boolean proceed )
    {
        failOnError = !proceed;
    }

    /**
     * Set the value of source.
     *
     * @param v Value to assign to source.
     */
    public void setSource( String v )
    {
        this.source = v;
    }

    /**
     * Set the source dirs to find the source Java files.
     *
     * @param srcDir The new Srcdir value
     */
    public void setSrcdir( Path srcDir )
        throws TaskException
    {
        if( src == null )
        {
            src = srcDir;
        }
        else
        {
            src.append( srcDir );
        }
    }

    /**
     * Sets the target VM that the classes will be compiled for. Valid strings
     * are "1.1", "1.2", and "1.3".
     *
     * @param target The new Target value
     */
    public void setTarget( String target )
    {
        this.target = target;
    }

    /**
     * Set the verbose flag.
     *
     * @param verbose The new Verbose value
     */
    public void setVerbose( boolean verbose )
    {
        this.verbose = verbose;
    }

    /**
     * Gets the bootclasspath that will be used to compile the classes against.
     *
     * @return The Bootclasspath value
     */
    public Path getBootclasspath()
    {
        return bootclasspath;
    }

    /**
     * Gets the classpath to be used for this compilation.
     *
     * @return The Classpath value
     */
    public Path getClasspath()
    {
        return compileClasspath;
    }

    protected File getBaseDir()
    {
        return getBaseDirectory();
    }

    /**
     * Get the additional implementation specific command line arguments.
     *
     * @return array of command line arguments, guaranteed to be non-null.
     */
    public String[] getCurrentCompilerArgs()
    {
        ArrayList args = new ArrayList();
        for( Iterator enum = implementationSpecificArgs.iterator();
             enum.hasNext();
            )
        {
            String[] curr =
                ( (ImplementationSpecificArgument)enum.next() ).getParts();
            for( int i = 0; i < curr.length; i++ )
            {
                args.add( curr[ i ] );
            }
        }
        final String[] res = new String[ args.size() ];
        return (String[])args.toArray( res );
    }

    /**
     * Gets the debug flag.
     *
     * @return The Debug value
     */
    public boolean getDebug()
    {
        return debug;
    }

    /**
     * Get the value of debugLevel.
     *
     * @return value of debugLevel.
     */
    public String getDebugLevel()
    {
        return debugLevel;
    }

    /**
     * Gets the depend flag.
     *
     * @return The Depend value
     */
    public boolean getDepend()
    {
        return depend;
    }

    /**
     * Gets the deprecation flag.
     *
     * @return The Deprecation value
     */
    public boolean getDeprecation()
    {
        return deprecation;
    }

    /**
     * Gets the destination directory into which the java source files should be
     * compiled.
     *
     * @return The Destdir value
     */
    public File getDestdir()
    {
        return destDir;
    }

    /**
     * Gets the java source file encoding name.
     *
     * @return The Encoding value
     */
    public String getEncoding()
    {
        return encoding;
    }

    /**
     * Gets the extension directories that will be used during the compilation.
     *
     * @return The Extdirs value
     */
    public Path getExtdirs()
    {
        return extdirs;
    }

    /**
     * Gets the failonerror flag.
     *
     * @return The Failonerror value
     */
    public boolean getFailonerror()
    {
        return failOnError;
    }

    /**
     * Gets the list of files to be compiled.
     *
     * @return The FileList value
     */
    public File[] getFileList()
    {
        return compileList;
    }

    /**
     * Gets whether or not the ant classpath is to be included in the task's
     * classpath.
     *
     * @return The Includeantruntime value
     */
    public boolean getIncludeantruntime()
    {
        return includeAntRuntime;
    }

    /**
     * Gets whether or not the java runtime should be included in this task's
     * classpath.
     *
     * @return The Includejavaruntime value
     */
    public boolean getIncludejavaruntime()
    {
        return includeJavaRuntime;
    }

    /**
     * The name of the javac executable to use in fork-mode.
     *
     * @return The JavacExecutable value
     */
    public String getJavacExecutable()
    {
        if( forkedExecutable == null && isForkedJavac() )
        {
            forkedExecutable = getSystemJavac();
        }
        else if( forkedExecutable != null && !isForkedJavac() )
        {
            forkedExecutable = null;
        }
        return forkedExecutable;
    }

    /**
     * Gets the memoryInitialSize flag.
     *
     * @return The MemoryInitialSize value
     */
    public String getMemoryInitialSize()
    {
        return memoryInitialSize;
    }

    /**
     * Gets the memoryMaximumSize flag.
     *
     * @return The MemoryMaximumSize value
     */
    public String getMemoryMaximumSize()
    {
        return memoryMaximumSize;
    }

    /**
     * Should the -nowarn option be used.
     *
     * @return The Nowarn value
     */
    public boolean getNowarn()
    {
        return nowarn;
    }

    /**
     * Gets the optimize flag.
     *
     * @return The Optimize value
     */
    public boolean getOptimize()
    {
        return optimize;
    }

    /**
     * Get the value of source.
     *
     * @return value of source.
     */
    public String getSource()
    {
        return source;
    }

    /**
     * Gets the source dirs to find the source java files.
     *
     * @return The Srcdir value
     */
    public Path getSrcdir()
    {
        return src;
    }

    /**
     * Gets the target VM that the classes will be compiled for.
     *
     * @return The Target value
     */
    public String getTarget()
    {
        return target;
    }

    /**
     * Gets the verbose flag.
     *
     * @return The Verbose value
     */
    public boolean getVerbose()
    {
        return verbose;
    }

    /**
     * Is this a forked invocation of JDK's javac?
     *
     * @return The ForkedJavac value
     */
    public boolean isForkedJavac()
    {
        return !"false".equals( fork ) ||
            "extJavac".equals( getProperty( "build.compiler" ) );
    }

    /**
     * Maybe creates a nested classpath element.
     *
     * @return Description of the Returned Value
     */
    public Path createBootclasspath()
        throws TaskException
    {
        if( bootclasspath == null )
        {
            bootclasspath = new Path();
        }
        return bootclasspath.createPath();
    }

    /**
     * Maybe creates a nested classpath element.
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
        throws TaskException
    {
        if( compileClasspath == null )
        {
            compileClasspath = new Path();
        }
        return compileClasspath.createPath();
    }

    /**
     * Adds an implementation specific command line argument.
     *
     * @return Description of the Returned Value
     */
    public ImplementationSpecificArgument createCompilerArg()
    {
        ImplementationSpecificArgument arg =
            new ImplementationSpecificArgument();
        implementationSpecificArgs.add( arg );
        return arg;
    }

    /**
     * Maybe creates a nested classpath element.
     *
     * @return Description of the Returned Value
     */
    public Path createExtdirs()
        throws TaskException
    {
        if( extdirs == null )
        {
            extdirs = new Path();
        }
        return extdirs.createPath();
    }

    /**
     * Create a nested src element for multiple source path support.
     *
     * @return a nested src element.
     */
    public Path createSrc()
        throws TaskException
    {
        if( src == null )
        {
            src = new Path();
        }
        return src.createPath();
    }

    /**
     * Executes the task.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        // first off, make sure that we've got a srcdir

        if( src == null )
        {
            throw new TaskException( "srcdir attribute must be set!" );
        }
        String[] list = src.list();
        if( list.length == 0 )
        {
            throw new TaskException( "srcdir attribute must be set!" );
        }

        if( destDir != null && !destDir.isDirectory() )
        {
            throw new TaskException( "destination directory \"" + destDir + "\" does not exist or is not a directory" );
        }

        // scan source directories and dest directory to build up
        // compile lists
        resetFileLists();
        for( int i = 0; i < list.length; i++ )
        {
            File srcDir = (File)resolveFile( list[ i ] );
            if( !srcDir.exists() )
            {
                throw new TaskException( "srcdir \"" + srcDir.getPath() + "\" does not exist!" );
            }

            DirectoryScanner ds = this.getDirectoryScanner( srcDir );

            String[] files = ds.getIncludedFiles();

            scanDir( srcDir, destDir != null ? destDir : srcDir, files );
        }

        // compile the source files

        String compiler = determineCompiler();

        if( compileList.length > 0 )
        {

            CompilerAdapter adapter = CompilerAdapterFactory.getCompiler(
                compiler, getLogger() );
            final String message = "Compiling " + compileList.length + " source file" +
                ( compileList.length == 1 ? "" : "s" ) +
                ( destDir != null ? " to " + destDir : "" );
            getLogger().info( message );

            // now we need to populate the compiler adapter
            adapter.setJavac( this );

            // finally, lets execute the compiler!!
            if( !adapter.execute() )
            {
                if( failOnError )
                {
                    throw new TaskException( FAIL_MSG );
                }
                else
                {
                    getLogger().error( FAIL_MSG );
                }
            }
        }
    }

    protected String getSystemJavac()
    {
        // This is the most common extension case - exe for windows and OS/2,
        // nothing for *nix.
        String extension = Os.isFamily( "dos" ) ? ".exe" : "";

        // Look for java in the java.home/../bin directory.  Unfortunately
        // on Windows java.home doesn't always refer to the correct location,
        // so we need to fall back to assuming java is somewhere on the
        // PATH.
        java.io.File jExecutable =
            new java.io.File( System.getProperty( "java.home" ) +
                              "/../bin/javac" + extension );

        if( jExecutable.exists() && !Os.isFamily( "netware" ) )
        {
            return jExecutable.getAbsolutePath();
        }
        else
        {
            return "javac";
        }
    }

    protected boolean isJdkCompiler( String compiler )
    {
        return "modern".equals( compiler ) ||
            "classic".equals( compiler ) ||
            "javac1.1".equals( compiler ) ||
            "javac1.2".equals( compiler ) ||
            "javac1.3".equals( compiler ) ||
            "javac1.4".equals( compiler );
    }

    /**
     * Clear the list of files to be compiled and copied..
     */
    protected void resetFileLists()
    {
        compileList = new File[ 0 ];
    }

    /**
     * Scans the directory looking for source files to be compiled. The results
     * are returned in the class variable compileList
     *
     * @param srcDir Description of Parameter
     * @param destDir Description of Parameter
     * @param files Description of Parameter
     */
    protected void scanDir( File srcDir, File destDir, String files[] )
        throws TaskException
    {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom( "*.java" );
        m.setTo( "*.class" );
        SourceFileScanner sfs = new SourceFileScanner( this );
        File[] newFiles = sfs.restrictAsFiles( files, srcDir, destDir, m );

        if( newFiles.length > 0 )
        {
            File[] newCompileList = new File[ compileList.length +
                newFiles.length ];
            System.arraycopy( compileList, 0, newCompileList, 0,
                              compileList.length );
            System.arraycopy( newFiles, 0, newCompileList,
                              compileList.length, newFiles.length );
            compileList = newCompileList;
        }
    }

    private String determineCompiler()
    {
        Object compiler = getProperty( "build.compiler" );

        if( !"false".equals( fork ) )
        {
            if( compiler != null )
            {
                if( isJdkCompiler( compiler.toString() ) )
                {
                    final String message = "Since fork is true, ignoring build.compiler setting.";
                    getLogger().warn( message );
                    compiler = "extJavac";
                }
                else
                {
                    getLogger().warn( "Since build.compiler setting isn't classic or modern, ignoring fork setting." );
                }
            }
            else
            {
                compiler = "extJavac";
            }
        }

        if( compiler == null )
        {
            if( JavaVersion.JAVA1_2 != JavaVersion.getCurrentJavaVersion() )
            {
                compiler = "modern";
            }
            else
            {
                compiler = "classic";
            }
        }
        return compiler.toString();
    }

    /**
     * Adds an "implementation" attribute to Commandline$Attribute used to
     * filter command line attributes based on the current implementation.
     *
     * @author RT
     */
    public class ImplementationSpecificArgument
        extends Argument
    {

        private String impl;

        public void setImplementation( String impl )
        {
            this.impl = impl;
        }

        public String[] getParts()
        {
            if( impl == null || impl.equals( determineCompiler() ) )
            {
                return super.getParts();
            }
            else
            {
                return new String[ 0 ];
            }
        }
    }

}
