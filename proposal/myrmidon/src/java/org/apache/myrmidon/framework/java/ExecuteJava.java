/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.java;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.aut.nativelib.Os;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Execute;
import org.apache.myrmidon.framework.file.Path;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.types.EnvironmentData;
import org.apache.tools.todo.types.PathUtil;
import org.apache.tools.todo.types.SysProperties;
import org.apache.tools.todo.util.FileUtils;

/**
 * A utility class that takes care of executing a Java application.  This
 * class can execute Java apps in the current JVM, or a forked JVM.
 *
 * <p>To execute a Java application, create an instance of this class,
 * configure it, and call one of the following methods:
 * <ul>
 * <li>{@link #execute}
 * <li>{@link #executeForked}
 * <li>{@link #executeNonForked}
 * </ul>
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class ExecuteJava
{
    private static final Resources REZ
        = ResourceManager.getPackageResources( ExecuteJava.class );

    private final Path m_classPath = new Path();
    private final EnvironmentData m_sysProperties = new EnvironmentData();
    private final Commandline m_args = new Commandline();
    private final Commandline m_vmArgs = new Commandline();
    private boolean m_fork;
    private File m_workingDirectory;
    private File m_jar;
    private String m_jvm;
    private String m_className;
    private String m_maxMemory;
    private boolean m_ignoreReturnCode;

    /**
     * Sets the main class of the application.
     */
    public void setClassName( final String className )
    {
        m_className = className;
    }

    /**
     * Sets the executable jar file to use to execute the application.
     * Can only be used in forked mode.
     */
    public void setJar( final File jar )
    {
        m_jar = jar;
    }

    /**
     * Enables forked mode.
     */
    public void setFork( final boolean fork )
    {
        m_fork = fork;
    }

    /**
     * Sets the max memory allocation pool size to use when running the
     * application.  Only used in forked mode.
     *
     * @param maxMemory the maximum memory pool size, or null for the default.
     */
    public void setMaxMemory( final String maxMemory )
    {
        m_maxMemory = maxMemory;
    }

    /**
     * Sets the working directory for the application.  Only used in forked mode.
     */
    public void setWorkingDirectory( final File workingDirectory )
    {
        m_workingDirectory = workingDirectory;
    }

    /**
     * Disables checking of the application's return code.  Only used in forked
     * mode.
     *
     * @param ignore If true, the return code of the application is ignored.
     *               If false, an exception is thrown if the application does
     *               no exit with a 0 return code.
     */
    public void setIgnoreReturnCode( boolean ignore )
    {
        m_ignoreReturnCode = ignore;
    }

    /**
     * Sets the JVM executable to use to run the application in a forked JVM.
     *
     * @param jvm the path to the JVM executable, or null to use the default
     *            JVM executable.
     */
    public void setJvm( final String jvm )
    {
        m_jvm = jvm;
    }

    /**
     * Returns the classpath that will be used to execute the application.
     *
     * @return the application's classpath.  This path can be modified.
     */
    public Path getClassPath()
    {
        return m_classPath;
    }

    /**
     * Returns the system properties that will be used for the application.
     * Only used in forked mode.
     *
     * @return the application's system properties.  Can be modified.
     */
    public EnvironmentData getSysProperties()
    {
        return m_sysProperties;
    }

    /**
     * Returns the arguments that will be used for the application.
     *
     * @return the application's arguments.  Can be modified.
     */
    public Commandline getArguments()
    {
        return m_args;
    }

    /**
     * Returns the JVM arguments that will be used to execute the application.
     * Only used in forked mode.
     *
     * @return the JVM aguments.  Can be modified.
     */
    public Commandline getVmArguments()
    {
        return m_vmArgs;
    }

    /**
     * Executes the application.
     */
    public void execute( final TaskContext context )
        throws TaskException
    {
        if( m_fork )
        {
            executeForked( context );
        }
        else
        {
            executeNonForked( context );
        }
    }

    /**
     * Executes the application in this JVM.
     */
    public void executeNonForked( final TaskContext context )
        throws TaskException
    {
        if( m_className == null )
        {
            final String message = REZ.getString( "executejava.no-classname.error" );
            throw new TaskException( message );
        }
        if( m_jar != null )
        {
            final String message = REZ.getString( "executejava.jar-no-fork.error" );
            throw new TaskException( message );
        }
        if( m_vmArgs.size() > 0 )
        {
            final String message = REZ.getString( "executejava.ignore-jvm-args.notice" );
            context.warn( message );
        }
        if( m_workingDirectory != null )
        {
            final String message = REZ.getString( "executejava.ignore-dir.notice" );
            context.warn( message );
        }
        if( m_maxMemory != null )
        {
            final String message = REZ.getString( "executejava.ignore-maxmem.notice" );
            context.warn( message );
        }
        if( m_sysProperties.size() > 0 )
        {
            final String message = REZ.getString( "executejava.ignore-sys-props.notice" );
            context.warn( message );
        }

        final String[] args = m_args.getArguments();

        // Log message
        if( context.isVerboseEnabled() )
        {
            final String debugMessage
                = REZ.getString( "executejava.exec-in-jvm.notice",
                                 m_className,
                                 FileUtils.formatCommandLine( args ) );
            context.verbose( debugMessage );
        }

        // Locate the class
        final Class target;
        try
        {
            final ClassLoader classLoader = PathUtil.createClassLoader( m_classPath, context );
            target = classLoader.loadClass( m_className );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "executejava.find-class.error", m_className );
            throw new TaskException( message, e );
        }

        // Call the main method
        try
        {
            final Class[] params = { args.getClass() };
            final Method main = target.getMethod( "main", params );
            main.invoke( null, new Object[] { args } );
        }
        catch( final InvocationTargetException e )
        {
            final Throwable t = e.getTargetException();
            final String message = REZ.getString( "executejava.execute-app.error", m_className );
            throw new TaskException( message, t );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "executejava.execute-app.error", m_className );
            throw new TaskException( message, e );
        }
    }

    /**
     * Executes the application in a separate JVM.
     */
    public int executeForked( final TaskContext context )
        throws TaskException
    {
        // Validate
        if( m_className != null && m_jar != null )
        {
            final String message = REZ.getString( "executejava.class-and-jar.error" );
            throw new TaskException( message );
        }
        if( m_className == null && m_jar == null )
        {
            final String message = REZ.getString( "executejava.no-classname.error" );
            throw new TaskException( message );
        }

        final Execute exe = new Execute();
        exe.setWorkingDirectory( m_workingDirectory );
        exe.setIgnoreReturnCode( m_ignoreReturnCode );

        // Setup the command line
        final Commandline command = exe.getCommandline();

        // Executable name
        if( m_jvm != null )
        {
            command.setExecutable( m_jvm );
        }
        else
        {
            command.setExecutable( getJavaExecutableName() );
        }

        // JVM arguments
        command.addArguments( m_vmArgs );

        // Max memory size
        if( m_maxMemory != null )
        {
            command.addArgument( "-Xmx" + m_maxMemory );
        }

        // System properties
        final String[] props = SysProperties.getJavaVariables( m_sysProperties );
        command.addArguments( props );

        // Classpath
        final String[] classpath = m_classPath.listFiles( context );
        if( classpath.length > 0 )
        {
            command.addArgument( "-classpath" );
            command.addArgument( PathUtil.formatPath( classpath ) );
        }

        // What to execute
        if( m_jar != null )
        {
            command.addArgument( "-jar" );
            command.addArgument( m_jar );
        }
        else
        {
            command.addArgument( m_className );
        }

        // Java app arguments
        command.addArguments( m_args );

        // Execute
        return exe.execute( context );
    }

    /**
     * Determines the executable name for the java command for this JVM.
     *
     * @todo Move this to somewhere in AUT.
     */
    public static String getJavaExecutableName()
    {
        if( Os.isFamily( Os.OS_FAMILY_NETWARE ) )
        {
            // NetWare may have a "java" in the JRE directory, but 99% of
            // the time, you don't want to execute it -- Jeff Tulley
            // <JTULLEY@novell.com>
            return "java";
        }

        // Figure out the basename
        final String baseName;
        if( Os.isFamily( Os.OS_FAMILY_WINDOWS) || Os.isFamily( Os.OS_FAMILY_DOS ) )
        {
            baseName = "java.exe";
        }
        else
        {
            baseName = "java";
        }

        // Look for java in the ${java.home{/../bin directory.  Unfortunately
        // on Windows java.home doesn't always refer to the correct location,
        // so we need to fall back to assuming java is somewhere on the
        // PATH.
        File javaExe =
            new File( System.getProperty( "java.home" ) + "/../bin/" + baseName );

        if( javaExe.exists() )
        {
            return javaExe.getAbsolutePath();
        }
        else
        {
            return "java";
        }
    }
}
