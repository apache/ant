/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.compilers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.JavaVersion;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.taskdefs.exec.LogOutputStream;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

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
    extends AbstractLogEnabled
    implements CompilerAdapter
{
    protected static String LINE_SEP = System.getProperty( "line.separator" );
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
    protected Project m_project;

    /*
     * jdg - TODO - all these attributes are currently protected, but they
     * should probably be private in the near future.
     */
    protected Path src;
    protected String target;

    public void setJavac( Javac attributes )
    {
        this.m_attributes = attributes;
        src = attributes.getSrcdir();
        m_destDir = attributes.getDestdir();
        m_encoding = attributes.getEncoding();
        m_debug = attributes.getDebug();
        m_optimize = attributes.getOptimize();
        m_deprecation = attributes.getDeprecation();
        m_depend = attributes.getDepend();
        m_verbose = attributes.getVerbose();
        target = attributes.getTarget();
        m_bootclasspath = attributes.getBootclasspath();
        m_extdirs = attributes.getExtdirs();
        m_compileList = attributes.getFileList();
        m_compileClasspath = attributes.getClasspath();
        m_project = attributes.getProject();
        m_includeAntRuntime = attributes.getIncludeantruntime();
        m_includeJavaRuntime = attributes.getIncludejavaruntime();
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
        logAndAddFilesToCompile( cmd );
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
        Path classpath = getCompileClasspath();
        String memoryParameterPrefix = "-J-X";
        if( m_memoryInitialSize != null )
        {
            if( !m_attributes.isForkedJavac() )
            {
                final String message = "Since fork is false, ignoring memoryInitialSize setting.";
                getLogger().warn( message );
            }
            else
            {
                cmd.createArgument().setValue( memoryParameterPrefix + "ms" + m_memoryInitialSize );
            }
        }

        if( m_memoryMaximumSize != null )
        {
            if( !m_attributes.isForkedJavac() )
            {
                final String message = "Since fork is false, ignoring memoryMaximumSize setting.";
                getLogger().warn( message );
            }
            else
            {
                cmd.createArgument().setValue( memoryParameterPrefix + "mx" + m_memoryMaximumSize );
            }
        }

        if( m_attributes.getNowarn() )
        {
            cmd.createArgument().setValue( "-nowarn" );
        }

        if( m_deprecation == true )
        {
            cmd.createArgument().setValue( "-deprecation" );
        }

        if( m_destDir != null )
        {
            cmd.createArgument().setValue( "-d" );
            cmd.createArgument().setFile( m_destDir );
        }

        cmd.createArgument().setValue( "-classpath" );
        cmd.createArgument().setPath( classpath );

        cmd.createArgument().setValue( "-sourcepath" );
        cmd.createArgument().setPath( src );

        if( target != null )
        {
            cmd.createArgument().setValue( "-target" );
            cmd.createArgument().setValue( target );
        }

        if( m_bootclasspath != null )
        {
            cmd.createArgument().setValue( "-bootclasspath" );
            cmd.createArgument().setPath( m_bootclasspath );
        }

        if( m_extdirs != null )
        {
            cmd.createArgument().setValue( "-extdirs" );
            cmd.createArgument().setPath( m_extdirs );
        }

        if( m_encoding != null )
        {
            cmd.createArgument().setValue( "-encoding" );
            cmd.createArgument().setValue( m_encoding );
        }
        if( m_debug )
        {
            if( useDebugLevel )
            {
                String debugLevel = m_attributes.getDebugLevel();
                if( debugLevel != null )
                {
                    cmd.createArgument().setValue( "-g:" + debugLevel );
                }
                else
                {
                    cmd.createArgument().setValue( "-g" );
                }
            }
            else
            {
                cmd.createArgument().setValue( "-g" );
            }
        }
        else
        {
            cmd.createArgument().setValue( "-g:none" );
        }
        if( m_optimize )
        {
            cmd.createArgument().setValue( "-O" );
        }

        if( m_verbose )
        {
            cmd.createArgument().setValue( "-verbose" );
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

        logAndAddFilesToCompile( cmd );
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
            cmd.createArgument().setValue( "-source" );
            cmd.createArgument().setValue( m_attributes.getSource() );
        }
        return cmd;
    }

    /**
     * Builds the compilation classpath.
     *
     * @return The CompileClasspath value
     */
    protected Path getCompileClasspath()
        throws TaskException
    {
        Path classpath = new Path();

        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath

        if( m_destDir != null )
        {
            classpath.setLocation( m_destDir );
        }

        // Combine the build classpath with the system classpath, in an
        // order determined by the value of build.classpath

        if( m_compileClasspath == null )
        {
            if( m_includeAntRuntime )
            {
                classpath.addExisting( Path.systemClasspath );
            }
        }
        else
        {
            if( m_includeAntRuntime )
            {
                classpath.addExisting( m_compileClasspath.concatSystemClasspath( "last" ) );
            }
            else
            {
                classpath.addExisting( m_compileClasspath.concatSystemClasspath( "ignore" ) );
            }
        }

        if( m_includeJavaRuntime )
        {
            classpath.addJavaRuntime();
        }

        return classpath;
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
     * @param args - arguments to pass to process on command line
     * @param firstFileName - index of the first source file in args
     * @return Description of the Returned Value
     */
    protected int executeExternalCompile( String[] args, int firstFileName )
        throws TaskException
    {
        String[] commandArray = null;
        File tmpFile = null;

        try
        {
            /*
             * Many system have been reported to get into trouble with
             * long command lines - no, not only Windows ;-).
             *
             * POSIX seems to define a lower limit of 4k, so use a temporary
             * file if the total length of the command line exceeds this limit.
             */
            if( Commandline.toString( args ).length() > 4096 )
            {
                PrintWriter out = null;
                try
                {
                    tmpFile = File.createTempFile( "jikes", "", new File( "." ) );
                    out = new PrintWriter( new FileWriter( tmpFile ) );
                    for( int i = firstFileName; i < args.length; i++ )
                    {
                        out.println( args[ i ] );
                    }
                    out.flush();
                    commandArray = new String[ firstFileName + 1 ];
                    System.arraycopy( args, 0, commandArray, 0, firstFileName );
                    commandArray[ firstFileName ] = "@" + tmpFile.getAbsolutePath();
                }
                catch( IOException e )
                {
                    throw new TaskException( "Error creating temporary file", e );
                }
                finally
                {
                    if( out != null )
                    {
                        try
                        {
                            out.close();
                        }
                        catch( Throwable t )
                        {
                        }
                    }
                }
            }
            else
            {
                commandArray = args;
            }

            try
            {
                final Execute exe = new Execute();
                exe.setOutput( new LogOutputStream( m_attributes.hackGetLogger(), false ) );
                exe.setError( new LogOutputStream( m_attributes.hackGetLogger(), true ) );
                exe.setWorkingDirectory( m_project.getBaseDir() );
                exe.setCommandline( commandArray );
                return exe.execute();
            }
            catch( IOException e )
            {
                throw new TaskException( "Error running " + args[ 0 ] + " compiler", e );
            }
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
    protected void logAndAddFilesToCompile( Commandline cmd )
    {
        getLogger().debug( "Compilation args: " + cmd.toString() );

        StringBuffer niceSourceList = new StringBuffer( "File" );
        if( m_compileList.length != 1 )
        {
            niceSourceList.append( "s" );
        }
        niceSourceList.append( " to be compiled:" );

        niceSourceList.append( LINE_SEP );

        for( int i = 0; i < m_compileList.length; i++ )
        {
            String arg = m_compileList[ i ].getAbsolutePath();
            cmd.createArgument().setValue( arg );
            niceSourceList.append( "    " + arg + LINE_SEP );
        }

        getLogger().debug( niceSourceList.toString() );
    }
}

