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
import org.apache.myrmidon.api.TaskException;
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
public abstract class DefaultCompilerAdapter implements CompilerAdapter
{
    protected static String lSep = System.getProperty( "line.separator" );
    protected boolean debug = false;
    protected boolean optimize = false;
    protected boolean deprecation = false;
    protected boolean depend = false;
    protected boolean verbose = false;

    protected Javac attributes;
    protected Path bootclasspath;
    protected Path compileClasspath;

    protected File[] compileList;
    protected File destDir;
    protected String encoding;
    protected Path extdirs;
    protected boolean includeAntRuntime;
    protected boolean includeJavaRuntime;
    protected String memoryInitialSize;
    protected String memoryMaximumSize;
    protected Project project;

    /*
     * jdg - TODO - all these attributes are currently protected, but they
     * should probably be private in the near future.
     */
    protected Path src;
    protected String target;

    public void setJavac( Javac attributes )
    {
        this.attributes = attributes;
        src = attributes.getSrcdir();
        destDir = attributes.getDestdir();
        encoding = attributes.getEncoding();
        debug = attributes.getDebug();
        optimize = attributes.getOptimize();
        deprecation = attributes.getDeprecation();
        depend = attributes.getDepend();
        verbose = attributes.getVerbose();
        target = attributes.getTarget();
        bootclasspath = attributes.getBootclasspath();
        extdirs = attributes.getExtdirs();
        compileList = attributes.getFileList();
        compileClasspath = attributes.getClasspath();
        project = attributes.getProject();
        includeAntRuntime = attributes.getIncludeantruntime();
        includeJavaRuntime = attributes.getIncludejavaruntime();
        memoryInitialSize = attributes.getMemoryInitialSize();
        memoryMaximumSize = attributes.getMemoryMaximumSize();
    }

    public Javac getJavac()
    {
        return attributes;
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

        // we cannot be using Java 1.0 when forking, so we only have to
        // distinguish between Java 1.1, and Java 1.2 and higher, as Java 1.1
        // has its own parameter format
        boolean usingJava1_1 = Project.getJavaVersion().equals( Project.JAVA_1_1 );
        String memoryParameterPrefix = usingJava1_1 ? "-J-" : "-J-X";
        if( memoryInitialSize != null )
        {
            if( !attributes.isForkedJavac() )
            {
                attributes.log( "Since fork is false, ignoring memoryInitialSize setting.",
                                Project.MSG_WARN );
            }
            else
            {
                cmd.createArgument().setValue( memoryParameterPrefix + "ms" + memoryInitialSize );
            }
        }

        if( memoryMaximumSize != null )
        {
            if( !attributes.isForkedJavac() )
            {
                attributes.log( "Since fork is false, ignoring memoryMaximumSize setting.",
                                Project.MSG_WARN );
            }
            else
            {
                cmd.createArgument().setValue( memoryParameterPrefix + "mx" + memoryMaximumSize );
            }
        }

        if( attributes.getNowarn() )
        {
            cmd.createArgument().setValue( "-nowarn" );
        }

        if( deprecation == true )
        {
            cmd.createArgument().setValue( "-deprecation" );
        }

        if( destDir != null )
        {
            cmd.createArgument().setValue( "-d" );
            cmd.createArgument().setFile( destDir );
        }

        cmd.createArgument().setValue( "-classpath" );

        // Just add "sourcepath" to classpath ( for JDK1.1 )
        // as well as "bootclasspath" and "extdirs"
        if( Project.getJavaVersion().startsWith( "1.1" ) )
        {
            Path cp = new Path( project );
            /*
             * XXX - This doesn't mix very well with build.systemclasspath,
             */
            if( bootclasspath != null )
            {
                cp.append( bootclasspath );
            }
            if( extdirs != null )
            {
                cp.addExtdirs( extdirs );
            }
            cp.append( classpath );
            cp.append( src );
            cmd.createArgument().setPath( cp );
        }
        else
        {
            cmd.createArgument().setPath( classpath );
            cmd.createArgument().setValue( "-sourcepath" );
            cmd.createArgument().setPath( src );
            if( target != null )
            {
                cmd.createArgument().setValue( "-target" );
                cmd.createArgument().setValue( target );
            }
            if( bootclasspath != null )
            {
                cmd.createArgument().setValue( "-bootclasspath" );
                cmd.createArgument().setPath( bootclasspath );
            }
            if( extdirs != null )
            {
                cmd.createArgument().setValue( "-extdirs" );
                cmd.createArgument().setPath( extdirs );
            }
        }

        if( encoding != null )
        {
            cmd.createArgument().setValue( "-encoding" );
            cmd.createArgument().setValue( encoding );
        }
        if( debug )
        {
            if( useDebugLevel
                && Project.getJavaVersion() != Project.JAVA_1_0
                && Project.getJavaVersion() != Project.JAVA_1_1 )
            {

                String debugLevel = attributes.getDebugLevel();
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
        else if( Project.getJavaVersion() != Project.JAVA_1_0 &&
            Project.getJavaVersion() != Project.JAVA_1_1 )
        {
            cmd.createArgument().setValue( "-g:none" );
        }
        if( optimize )
        {
            cmd.createArgument().setValue( "-O" );
        }

        if( depend )
        {
            if( Project.getJavaVersion().startsWith( "1.1" ) )
            {
                cmd.createArgument().setValue( "-depend" );
            }
            else if( Project.getJavaVersion().startsWith( "1.2" ) )
            {
                cmd.createArgument().setValue( "-Xdepend" );
            }
            else
            {
                attributes.log( "depend attribute is not supported by the modern compiler",
                                Project.MSG_WARN );
            }
        }

        if( verbose )
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
        if( attributes.getSource() != null )
        {
            cmd.createArgument().setValue( "-source" );
            cmd.createArgument().setValue( attributes.getSource() );
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
        Path classpath = new Path( project );

        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath

        if( destDir != null )
        {
            classpath.setLocation( destDir );
        }

        // Combine the build classpath with the system classpath, in an
        // order determined by the value of build.classpath

        if( compileClasspath == null )
        {
            if( includeAntRuntime )
            {
                classpath.addExisting( Path.systemClasspath );
            }
        }
        else
        {
            if( includeAntRuntime )
            {
                classpath.addExisting( compileClasspath.concatSystemClasspath( "last" ) );
            }
            else
            {
                classpath.addExisting( compileClasspath.concatSystemClasspath( "ignore" ) );
            }
        }

        if( includeJavaRuntime )
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
                exe.setOutput( new LogOutputStream( attributes, Project.MSG_INFO ) );
                exe.setError( new LogOutputStream( attributes, Project.MSG_WARN ) );
                exe.setWorkingDirectory( project.getBaseDir() );
                exe.setCommandline( commandArray );
                return exe.execute();
            }
            catch( IOException e )
            {
                throw new TaskException( "Error running " + args[ 0 ]
                                         + " compiler", e );
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
        attributes.log( "Compilation args: " + cmd.toString(),
                        Project.MSG_VERBOSE );

        StringBuffer niceSourceList = new StringBuffer( "File" );
        if( compileList.length != 1 )
        {
            niceSourceList.append( "s" );
        }
        niceSourceList.append( " to be compiled:" );

        niceSourceList.append( lSep );

        for( int i = 0; i < compileList.length; i++ )
        {
            String arg = compileList[ i ].getAbsolutePath();
            cmd.createArgument().setValue( arg );
            niceSourceList.append( "    " + arg + lSep );
        }

        attributes.log( niceSourceList.toString(), Project.MSG_VERBOSE );
    }

}

