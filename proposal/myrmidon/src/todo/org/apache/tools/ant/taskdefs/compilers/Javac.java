/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.compilers;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.aut.nativelib.Os;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.JavaVersion;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.SourceFileScanner;
import org.apache.tools.ant.util.mappers.GlobPatternMapper;

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
public class Javac
    extends MatchingTask
{
    private final static String FAIL_MSG
        = "Compile failed, messages should have been provided.";

    private boolean m_debug;
    private boolean m_optimize;
    private boolean m_deprecation;
    private boolean m_depend;
    private boolean m_verbose;
    private boolean m_includeAntRuntime = true;
    private boolean m_includeJavaRuntime;
    private boolean m_fork;
    private String m_forkedExecutable;
    private boolean m_nowarn;
    private ArrayList m_implementationSpecificArgs = new ArrayList();

    protected File[] m_compileList = new File[ 0 ];
    private Path m_bootclasspath;
    private Path m_compileClasspath;
    private String m_debugLevel;
    private File m_destDir;
    private String m_encoding;
    private Path m_extdirs;
    private String m_memoryInitialSize;
    private String m_memoryMaximumSize;
    private String m_source;
    private Path m_src;
    private String m_target;

    /**
     * Adds an element to the bootclasspath that will be used to compile the
     * classes against.
     */
    public void addBootclasspath( Path bootclasspath )
    {
        if( m_bootclasspath == null )
        {
            m_bootclasspath = bootclasspath;
        }
        else
        {
            m_bootclasspath.addPath( bootclasspath );
        }
    }

    /**
     * Adds an element to the classpath to be used for this compilation.
     */
    public void addClasspath( Path classpath )
    {
        if( m_compileClasspath == null )
        {
            m_compileClasspath = classpath;
        }
        else
        {
            m_compileClasspath.addPath( classpath );
        }
    }

    /**
     * Set the debug flag.
     */
    public void setDebug( final boolean debug )
    {
        m_debug = debug;
    }

    /**
     * Set the value of debugLevel.
     *
     * @param v Value to assign to debugLevel.
     */
    public void setDebugLevel( String v )
    {
        m_debugLevel = v;
    }

    /**
     * Set the depend flag.
     *
     * @param depend The new Depend value
     */
    public void setDepend( boolean depend )
    {
        m_depend = depend;
    }

    /**
     * Set the deprecation flag.
     *
     * @param deprecation The new Deprecation value
     */
    public void setDeprecation( boolean deprecation )
    {
        m_deprecation = deprecation;
    }

    /**
     * Set the destination directory into which the Java source files should be
     * compiled.
     *
     * @param destDir The new Destdir value
     */
    public void setDestdir( File destDir )
    {
        m_destDir = destDir;
    }

    /**
     * Set the Java source file encoding name.
     *
     * @param encoding The new Encoding value
     */
    public void setEncoding( String encoding )
    {
        m_encoding = encoding;
    }

    /**
     * Adds an element to the extension directories that will be used during
     * the compilation.
     *
     * @param extdirs The new Extdirs value
     */
    public void addExtdirs( Path extdirs )
        throws TaskException
    {
        if( m_extdirs == null )
        {
            m_extdirs = extdirs;
        }
        else
        {
            m_extdirs.addPath( extdirs );
        }
    }

    /**
     * Sets whether to fork the javac compiler.
     */
    public void setFork( final boolean fork )
    {
        m_fork = fork;
        if( fork )
        {
            m_forkedExecutable = getSystemJavac();
        }
    }

    /**
     * Include ant's own classpath in this task's classpath?
     *
     * @param include The new Includeantruntime value
     */
    public void setIncludeantruntime( boolean include )
    {
        m_includeAntRuntime = include;
    }

    /**
     * Sets whether or not to include the java runtime libraries to this task's
     * classpath.
     *
     * @param include The new Includejavaruntime value
     */
    public void setIncludejavaruntime( boolean include )
    {
        m_includeJavaRuntime = include;
    }

    /**
     * Set the memoryInitialSize flag.
     *
     * @param memoryInitialSize The new MemoryInitialSize value
     */
    public void setMemoryInitialSize( String memoryInitialSize )
    {
        m_memoryInitialSize = memoryInitialSize;
    }

    /**
     * Set the memoryMaximumSize flag.
     *
     * @param memoryMaximumSize The new MemoryMaximumSize value
     */
    public void setMemoryMaximumSize( String memoryMaximumSize )
    {
        m_memoryMaximumSize = memoryMaximumSize;
    }

    /**
     * Sets whether the -nowarn option should be used.
     *
     * @param flag The new Nowarn value
     */
    public void setNowarn( boolean flag )
    {
        m_nowarn = flag;
    }

    /**
     * Set the optimize flag.
     *
     * @param optimize The new Optimize value
     */
    public void setOptimize( boolean optimize )
    {
        m_optimize = optimize;
    }

    /**
     * Set the value of source.
     *
     * @param v Value to assign to source.
     */
    public void setSource( String v )
    {
        m_source = v;
    }

    /**
     * Adds an element to the source dirs to find the source Java files.
     *
     * @param srcDir The new Srcdir value
     */
    public void addSrcdir( Path srcDir )
        throws TaskException
    {
        if( m_src == null )
        {
            m_src = srcDir;
        }
        else
        {
            m_src.addPath( srcDir );
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
        m_target = target;
    }

    /**
     * Set the verbose flag.
     *
     * @param verbose The new Verbose value
     */
    public void setVerbose( boolean verbose )
    {
        m_verbose = verbose;
    }

    /**
     * Gets the bootclasspath that will be used to compile the classes against.
     *
     * @return The Bootclasspath value
     */
    public Path getBootclasspath()
    {
        return m_bootclasspath;
    }

    /**
     * Gets the classpath to be used for this compilation.
     *
     * @return The Classpath value
     */
    public Path getClasspath()
    {
        return m_compileClasspath;
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
        for( Iterator enum = m_implementationSpecificArgs.iterator();
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
        return m_debug;
    }

    /**
     * Get the value of debugLevel.
     *
     * @return value of debugLevel.
     */
    public String getDebugLevel()
    {
        return m_debugLevel;
    }

    /**
     * Gets the depend flag.
     *
     * @return The Depend value
     */
    public boolean getDepend()
    {
        return m_depend;
    }

    /**
     * Gets the deprecation flag.
     *
     * @return The Deprecation value
     */
    public boolean getDeprecation()
    {
        return m_deprecation;
    }

    /**
     * Gets the destination directory into which the java source files should be
     * compiled.
     *
     * @return The Destdir value
     */
    public File getDestdir()
    {
        return m_destDir;
    }

    /**
     * Gets the java source file encoding name.
     *
     * @return The Encoding value
     */
    public String getEncoding()
    {
        return m_encoding;
    }

    /**
     * Gets the extension directories that will be used during the compilation.
     *
     * @return The Extdirs value
     */
    public Path getExtdirs()
    {
        return m_extdirs;
    }

    /**
     * Gets the list of files to be compiled.
     *
     * @return The FileList value
     */
    public File[] getFileList()
    {
        return m_compileList;
    }

    /**
     * Gets whether or not the ant classpath is to be included in the task's
     * classpath.
     *
     * @return The Includeantruntime value
     */
    public boolean getIncludeantruntime()
    {
        return m_includeAntRuntime;
    }

    /**
     * Gets whether or not the java runtime should be included in this task's
     * classpath.
     *
     * @return The Includejavaruntime value
     */
    public boolean getIncludejavaruntime()
    {
        return m_includeJavaRuntime;
    }

    /**
     * The name of the javac executable to use in fork-mode.
     *
     * @return The JavacExecutable value
     */
    public String getJavacExecutable()
    {
        if( m_forkedExecutable == null && isForkedJavac() )
        {
            m_forkedExecutable = getSystemJavac();
        }
        else if( m_forkedExecutable != null && !isForkedJavac() )
        {
            m_forkedExecutable = null;
        }
        return m_forkedExecutable;
    }

    /**
     * Gets the memoryInitialSize flag.
     *
     * @return The MemoryInitialSize value
     */
    public String getMemoryInitialSize()
    {
        return m_memoryInitialSize;
    }

    /**
     * Gets the memoryMaximumSize flag.
     *
     * @return The MemoryMaximumSize value
     */
    public String getMemoryMaximumSize()
    {
        return m_memoryMaximumSize;
    }

    /**
     * Should the -nowarn option be used.
     *
     * @return The Nowarn value
     */
    public boolean getNowarn()
    {
        return m_nowarn;
    }

    /**
     * Gets the optimize flag.
     *
     * @return The Optimize value
     */
    public boolean isOptimize()
    {
        return m_optimize;
    }

    /**
     * Get the value of source.
     *
     * @return value of source.
     */
    public String getSource()
    {
        return m_source;
    }

    /**
     * Gets the source dirs to find the source java files.
     *
     * @return The Srcdir value
     */
    public Path getSrcdir()
    {
        return m_src;
    }

    /**
     * Gets the target VM that the classes will be compiled for.
     *
     * @return The Target value
     */
    public String getTarget()
    {
        return m_target;
    }

    /**
     * Gets the verbose flag.
     *
     * @return The Verbose value
     */
    public boolean getVerbose()
    {
        return m_verbose;
    }

    /**
     * Is this a forked invocation of JDK's javac?
     *
     * @return The ForkedJavac value
     */
    public boolean isForkedJavac()
    {
        return m_fork;
    }

    /**
     * Adds an implementation specific command line argument.
     */
    public ImplementationSpecificArgument createCompilerArg()
    {
        final ImplementationSpecificArgument arg = new ImplementationSpecificArgument( this );
        m_implementationSpecificArgs.add( arg );
        return arg;
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

        if( m_src == null )
        {
            throw new TaskException( "srcdir attribute must be set!" );
        }
        String[] list = m_src.list();
        if( list.length == 0 )
        {
            throw new TaskException( "srcdir attribute must be set!" );
        }

        if( m_destDir != null && !m_destDir.isDirectory() )
        {
            throw new TaskException( "destination directory \"" + m_destDir + "\" does not exist or is not a directory" );
        }

        // scan source directories and dest directory to build up
        // compile lists
        resetFileLists();
        for( int i = 0; i < list.length; i++ )
        {
            final String filename = list[ i ];
            File srcDir = (File)getContext().resolveFile( filename );
            if( !srcDir.exists() )
            {
                throw new TaskException( "srcdir \"" + srcDir.getPath() + "\" does not exist!" );
            }

            DirectoryScanner ds = getDirectoryScanner( srcDir );

            String[] files = ds.getIncludedFiles();

            scanDir( srcDir, m_destDir != null ? m_destDir : srcDir, files );
        }

        // compile the source files

        String compiler = determineCompiler();

        if( m_compileList.length > 0 )
        {

            CompilerAdapter adapter =
                CompilerAdapterFactory.getCompiler( compiler, getContext() );
            final String message = "Compiling " + m_compileList.length + " source file" +
                ( m_compileList.length == 1 ? "" : "s" ) +
                ( m_destDir != null ? " to " + m_destDir : "" );
            getLogger().info( message );

            // now we need to populate the compiler adapter
            adapter.setJavac( this );

            // finally, lets execute the compiler!!
            if( !adapter.execute() )
            {
                throw new TaskException( FAIL_MSG );
            }
        }
    }

    protected String getSystemJavac()
    {
        // This is the most common extension case - exe for windows and OS/2,
        // nothing for *nix.
        String extension = Os.isFamily( Os.OS_FAMILY_DOS ) ? ".exe" : "";

        // Look for java in the java.home/../bin directory.  Unfortunately
        // on Windows java.home doesn't always refer to the correct location,
        // so we need to fall back to assuming java is somewhere on the
        // PATH.
        File jExecutable =
            new File( System.getProperty( "java.home" ) +
                              "/../bin/javac" + extension );

        if( jExecutable.exists() && !Os.isFamily( Os.OS_FAMILY_NETWARE ) )
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
        m_compileList = new File[ 0 ];
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
        SourceFileScanner sfs = new SourceFileScanner();
        setupLogger( sfs );
        File[] newFiles = sfs.restrictAsFiles( files, srcDir, destDir, m, getContext() );

        if( newFiles.length > 0 )
        {
            File[] newCompileList = new File[ m_compileList.length +
                newFiles.length ];
            System.arraycopy( m_compileList, 0, newCompileList, 0,
                              m_compileList.length );
            System.arraycopy( newFiles, 0, newCompileList,
                              m_compileList.length, newFiles.length );
            m_compileList = newCompileList;
        }
    }

    protected String determineCompiler()
    {
        Object compiler = getContext().getProperty( "build.compiler" );
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
}
