/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.javac;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Execute;
import org.apache.myrmidon.framework.FileSet;
import org.apache.tools.todo.types.Commandline;
import org.apache.myrmidon.framework.file.Path;
import org.apache.myrmidon.framework.file.FileListUtil;
import org.apache.tools.todo.util.FileUtils;
import org.apache.aut.nativelib.PathUtil;

/**
 * This is the default implementation for the CompilerAdapter interface.
 * Currently, this is a cut-and-paste of the original javac task.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com
 *      </a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 */
public abstract class DefaultCompilerAdapter
    implements CompilerAdapter
{
    protected boolean m_debug;
    protected boolean m_optimize;
    protected boolean m_deprecation;
    protected boolean m_depend;
    protected boolean m_verbose;

    protected Javac m_attributes;
    protected Path m_bootclasspath;
    protected Path m_compileClasspath;

    protected File[] m_compileList;
    protected File m_destDir;
    protected String m_encoding;
    protected Path m_extdirs;
    protected boolean m_includeAntRuntime;
    protected boolean m_includeJavaRuntime;
    protected String m_memoryInitialSize;
    protected String m_memoryMaximumSize;

    /*
     * jdg - TODO - all these attributes are currently protected, but they
     * should probably be private in the near future.
     */
    protected Path src;
    protected String target;

    private TaskContext m_taskContext;

    public void setTaskContext( final TaskContext context )
    {
        m_taskContext = context;
    }

    protected final TaskContext getTaskContext()
    {
        return m_taskContext;
    }

    public void setJavac( Javac attributes )
    {
        m_attributes = attributes;
        src = attributes.getSrcdir();
        m_destDir = attributes.getDestdir();
        m_encoding = attributes.getEncoding();
        m_debug = attributes.getDebug();
        m_optimize = attributes.isOptimize();
        m_deprecation = attributes.getDeprecation();
        m_depend = attributes.getDepend();
        m_verbose = attributes.getVerbose();
        target = attributes.getTarget();
        m_bootclasspath = attributes.getBootclasspath();
        m_extdirs = attributes.getExtdirs();
        m_compileList = attributes.getFileList();
        m_compileClasspath = attributes.getClasspath();
        m_memoryInitialSize = attributes.getMemoryInitialSize();
        m_memoryMaximumSize = attributes.getMemoryMaximumSize();
    }

    public Javac getJavac()
    {
        return m_attributes;
    }

    protected Commandline setupJavacCommand()
        throws TaskException
    {
        return setupJavacCommand( false );
    }

    /**
     * Does the command line argument processing for classic and adds the files
     * to compile as well.
     *
     * @param debugLevelCheck Description of Parameter
     * @return Description of the Returned Value
     */
    protected Commandline setupJavacCommand( boolean debugLevelCheck )
        throws TaskException
    {
        Commandline cmd = new Commandline();
        setupJavacCommandlineSwitches( cmd, debugLevelCheck );
        logFilesToCompile( cmd );
        addFilesToCompile( cmd );
        return cmd;
    }

    /**
     * Does the command line argument processing common to classic and modern.
     * Doesn't add the files to compile.
     *
     * @param cmd Description of Parameter
     * @param useDebugLevel Description of Parameter
     * @return Description of the Returned Value
     */
    protected Commandline setupJavacCommandlineSwitches( Commandline cmd,
                                                         boolean useDebugLevel )
        throws TaskException
    {
        Path classpath = new Path();
        addCompileClasspath( classpath );
        String memoryParameterPrefix = "-J-X";
        if( m_memoryInitialSize != null )
        {
            if( !m_attributes.isForkedJavac() )
            {
                final String message = "Since fork is false, ignoring memoryInitialSize setting.";
                getTaskContext().warn( message );
            }
            else
            {
                cmd.addArgument( memoryParameterPrefix + "ms" + m_memoryInitialSize );
            }
        }

        if( m_memoryMaximumSize != null )
        {
            if( !m_attributes.isForkedJavac() )
            {
                final String message = "Since fork is false, ignoring memoryMaximumSize setting.";
                getTaskContext().warn( message );
            }
            else
            {
                cmd.addArgument( memoryParameterPrefix + "mx" + m_memoryMaximumSize );
            }
        }

        if( m_attributes.getNowarn() )
        {
            cmd.addArgument( "-nowarn" );
        }

        if( m_deprecation == true )
        {
            cmd.addArgument( "-deprecation" );
        }

        if( m_destDir != null )
        {
            cmd.addArgument( "-d" );
            cmd.addArgument( m_destDir );
        }

        cmd.addArgument( "-classpath" );
        cmd.addArgument( FileListUtil.formatPath( classpath, getTaskContext() ) );

        cmd.addArgument( "-sourcepath" );
        cmd.addArgument( FileListUtil.formatPath( src, getTaskContext() ) );

        if( target != null )
        {
            cmd.addArgument( "-target" );
            cmd.addArgument( target );
        }

        final String[] bootclasspath = m_bootclasspath.listFiles( getTaskContext() );
        if( bootclasspath.length > 0 )
        {
            cmd.addArgument( "-bootclasspath" );
            cmd.addArgument( PathUtil.formatPath( bootclasspath ) );
        }

        if( m_extdirs != null )
        {
            cmd.addArgument( "-extdirs" );
            cmd.addArgument( FileListUtil.formatPath( m_extdirs, getTaskContext() ) );
        }

        if( m_encoding != null )
        {
            cmd.addArgument( "-encoding" );
            cmd.addArgument( m_encoding );
        }
        if( m_debug )
        {
            if( useDebugLevel )
            {
                String debugLevel = m_attributes.getDebugLevel();
                if( debugLevel != null )
                {
                    cmd.addArgument( "-g:" + debugLevel );
                }
                else
                {
                    cmd.addArgument( "-g" );
                }
            }
            else
            {
                cmd.addArgument( "-g" );
            }
        }
        else
        {
            cmd.addArgument( "-g:none" );
        }
        if( m_optimize )
        {
            cmd.addArgument( "-O" );
        }

        if( m_verbose )
        {
            cmd.addArgument( "-verbose" );
        }

        addCurrentCompilerArgs( cmd );

        return cmd;
    }

    /**
     * Does the command line argument processing for modern and adds the files
     * to compile as well.
     *
     * @return Description of the Returned Value
     */
    protected Commandline setupModernJavacCommand()
        throws TaskException
    {
        Commandline cmd = new Commandline();
        setupModernJavacCommandlineSwitches( cmd );

        logFilesToCompile( cmd );
        addFilesToCompile( cmd );
        return cmd;
    }

    /**
     * Does the command line argument processing for modern. Doesn't add the
     * files to compile.
     *
     * @param cmd Description of Parameter
     * @return Description of the Returned Value
     */
    protected Commandline setupModernJavacCommandlineSwitches( Commandline cmd )
        throws TaskException
    {
        setupJavacCommandlineSwitches( cmd, true );
        if( m_attributes.getSource() != null )
        {
            cmd.addArgument( "-source" );
            cmd.addArgument( m_attributes.getSource() );
        }
        return cmd;
    }

    /**
     * Adds the compilation classpath to a path.
     */
    protected void addCompileClasspath( final Path classpath )
        throws TaskException
    {
        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath

        if( m_destDir != null )
        {
            classpath.addLocation( m_destDir );
        }

        // add the classpath
        if( m_compileClasspath != null )
        {
            classpath.add( m_compileClasspath );
        }
    }

    /**
     * Adds the command line arguments specifc to the current implementation.
     *
     * @param cmd The feature to be added to the CurrentCompilerArgs attribute
     */
    protected void addCurrentCompilerArgs( Commandline cmd )
    {
        cmd.addArguments( getJavac().getCurrentCompilerArgs() );
    }

    /**
     * Do the compile with the specified arguments.
     *
     * @param cmd - the command line, to which the names of the files to
     *        compile are added.
     */
    protected boolean executeExternalCompile( final Commandline cmd )
        throws TaskException
    {
        logFilesToCompile( cmd );

        File tmpFile = null;

        try
        {
            /*
             * Many system have been reported to get into trouble with
             * long command lines - no, not only Windows ;-).
             *
             * POSIX seems to define a lower limit of 4k, so use a temporary
             * file.
             */
            try
            {
                tmpFile = File.createTempFile( "javac", "", new File( "." ) );
                final FileWriter fout = new FileWriter( tmpFile );
                try
                {
                    final PrintWriter out = new PrintWriter( fout );
                    for( int i = 0; i < m_compileList.length; i++ )
                    {
                        File file = m_compileList[i ];
                        out.println( file.getAbsolutePath() );
                    }
                    out.close();
                }
                finally
                {
                    IOUtil.shutdownWriter( fout );
                }
            }
            catch( final IOException ioe )
            {
                throw new TaskException( "Error creating temporary file", ioe );
            }

            cmd.addArgument( "@" + tmpFile.getAbsolutePath() );

            final Execute exe = new Execute();
            exe.setIgnoreReturnCode( true );
            exe.setCommandline( cmd );
            return exe.execute( getTaskContext() ) == 0;
        }
        finally
        {
            if( tmpFile != null )
            {
                tmpFile.delete();
            }
        }
    }

    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &qout;niceSourceList&quot;
     *
     * @param cmd Description of Parameter
     */
    protected void logFilesToCompile( final Commandline cmd )
        throws TaskException
    {
        final String[] cmdline = cmd.getArguments();
        getTaskContext().debug( "Compilation args: " + FileUtils.formatCommandLine( cmdline ) );

        StringBuffer niceSourceList = new StringBuffer( "File" );
        if( m_compileList.length != 1 )
        {
            niceSourceList.append( "s" );
        }
        niceSourceList.append( " to be compiled:" );

        niceSourceList.append( StringUtil.LINE_SEPARATOR );

        for( int i = 0; i < m_compileList.length; i++ )
        {
            String arg = m_compileList[ i ].getAbsolutePath();
            niceSourceList.append( "    " + arg + StringUtil.LINE_SEPARATOR );
        }

        getTaskContext().debug( niceSourceList.toString() );
    }

    /**
     * Adds the files to compile to a command-line
     */
    protected void addFilesToCompile( final Commandline cmd )
    {
        for( int i = 0; i < m_compileList.length; i++ )
        {
            File file = m_compileList[i ];
            cmd.addArgument( file );
        }
    }

    /**
     * Emulation of extdirs feature in java >= 1.2. This method adds all files
     * in the given directories (but not in sub-directories!) to the classpath,
     * so that you don't have to specify them all one by one.
     */
    protected void addExtdirs( Path path )
        throws TaskException
    {
        if( m_extdirs == null )
        {
            String extProp = System.getProperty( "java.ext.dirs" );
            if( extProp != null )
            {
                m_extdirs = new Path( extProp );
            }
            else
            {
                return;
            }
        }

        addExtdirs( path, m_extdirs, getTaskContext() );
    }

    /**
     * Adds the contents of a set of directories to a path.
     */
    public static void addExtdirs( final Path toPath,
                                   final Path extDirs,
                                   final TaskContext context )
        throws TaskException
    {
        final String[] dirs = extDirs.listFiles( context );
        for( int i = 0; i < dirs.length; i++ )
        {
            final File dir = new File( dirs[ i ] );
            if( dir.exists() && dir.isDirectory() )
            {
                final FileSet fileSet = new FileSet();
                fileSet.setDir( dir );
                fileSet.setIncludes( "*" );
                toPath.addFileset( fileSet );
            }
        }
    }
}

