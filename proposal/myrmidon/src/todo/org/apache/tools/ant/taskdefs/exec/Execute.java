/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.exec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Os;
import org.apache.myrmidon.framework.exec.CommandLauncher;
import org.apache.myrmidon.framework.exec.Environment;
import org.apache.myrmidon.framework.exec.ExecMetaData;
import org.apache.myrmidon.framework.exec.launchers.DefaultCommandLauncher;
import org.apache.myrmidon.framework.exec.launchers.MacCommandLauncher;
import org.apache.myrmidon.framework.exec.launchers.ScriptCommandLauncher;
import org.apache.myrmidon.framework.exec.launchers.WinNTCommandLauncher;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;

/**
 * Runs an external program.
 *
 * @author thomas.haas@softwired-inc.com
 */
public class Execute
{
    private static final CommandLauncher c_launcher = new DefaultCommandLauncher();
    private static final CommandLauncher c_shellLauncher = createShellLauncher();

    /**
     * Used to destroy processes when the VM exits.
     */
    private static ProcessDestroyer c_processDestroyer = new ProcessDestroyer();

    private ExecMetaData m_metaData;
    private String[] m_command;
    private Properties m_environment;
    private int m_exitValue;
    private File m_workingDirectory = new File( "." );
    private Project m_project;
    private boolean m_newEnvironment;

    /**
     * Controls whether the VM is used to launch commands, where possible
     */
    private boolean m_useVMLauncher = true;
    private ExecuteStreamHandler m_streamHandler;
    private ExecuteWatchdog m_watchdog;

    private static CommandLauncher createShellLauncher()
    {
        CommandLauncher launcher = null;
        try
        {
            if( Os.isFamily( "mac" ) )
            {
                // Mac
                launcher = new MacCommandLauncher();
            }
            else if( Os.isFamily( "os/2" ) )
            {
                // OS/2 - use same mechanism as Windows 2000
                launcher = new WinNTCommandLauncher();
            }
            else if( Os.isFamily( "windows" ) )
            {
                // Windows.  Need to determine which JDK we're running in

                // Determine if we're running under 2000/NT or 98/95
                final String osname =
                    System.getProperty( "os.name" ).toLowerCase( Locale.US );

                if( osname.indexOf( "nt" ) >= 0 || osname.indexOf( "2000" ) >= 0 )
                {
                    // Windows 2000/NT
                    launcher = new WinNTCommandLauncher();
                }
                else
                {
                    // Windows 98/95 - need to use an auxiliary script
                    final String script = resolveCommand( "bin/antRun.bat" );
                    launcher = new ScriptCommandLauncher( script );
                }
            }
            else if( ( new Os( "netware" ) ).eval() )
            {
                // NetWare.  Need to determine which JDK we're running in
                final String perlScript = resolveCommand( "bin/antRun.pl" );
                final String[] script = new String[]{"perl", perlScript};
                launcher = new ScriptCommandLauncher( script );
            }
            else
            {
                // Generic
                final String script = resolveCommand( "bin/antRun" );
                launcher = new ScriptCommandLauncher( script );
            }
        }
        catch( final TaskException te )
        {
            te.printStackTrace();
        }
        return launcher;
    }

    private static String resolveCommand( final String command )
    {
        final File homeDir = getAntHomeDirectory();
        final String script =
            FileUtil.resolveFile( homeDir, command ).toString();
        return script;
    }

    /**
     * Retrieve the directory in which Myrmidon is installed.
     * This is used to determine the locaiton of scripts in various launchers.
     */
    protected static File getAntHomeDirectory()
    {
        final String antHome = System.getProperty( "ant.home" );
        if( null == antHome )
        {
            final String message =
                "Cannot locate antRun script: Property 'ant.home' not specified";
            throw new IllegalStateException( message );
        }

        return new File( antHome );
    }

    /**
     * Creates a new execute object using <code>PumpStreamHandler</code> for
     * stream handling.
     */
    public Execute()
    {
        this( new PumpStreamHandler(), null );
    }

    /**
     * Creates a new execute object.
     *
     * @param streamHandler the stream handler used to handle the input and
     *      output streams of the subprocess.
     */
    public Execute( ExecuteStreamHandler streamHandler )
    {
        this( streamHandler, null );
    }

    /**
     * Creates a new execute object.
     *
     * @param streamHandler the stream handler used to handle the input and
     *      output streams of the subprocess.
     * @param watchdog a watchdog for the subprocess or <code>null</code> to to
     *      disable a timeout for the subprocess.
     */
    public Execute( ExecuteStreamHandler streamHandler, ExecuteWatchdog watchdog )
    {
        m_streamHandler = streamHandler;
        m_watchdog = watchdog;
    }

    /**
     * A utility method that runs an external command. Writes the output and
     * error streams of the command to the project log.
     *
     * @param task The task that the command is part of. Used for logging
     * @param cmdline The command to execute.
     * @throws TaskException if the command does not return 0.
     */
    public static void runCommand( Task task, String[] cmdline )
        throws TaskException
    {
        try
        {
            task.log( Commandline.toString( cmdline ), Project.MSG_VERBOSE );
            Execute exe = new Execute( new LogStreamHandler( task,
                                                             Project.MSG_INFO,
                                                             Project.MSG_ERR ) );
            exe.setCommandline( cmdline );
            int retval = exe.execute();
            if( retval != 0 )
            {
                throw new TaskException( cmdline[ 0 ] + " failed with return code " + retval );
            }
        }
        catch( IOException exc )
        {
            throw new TaskException( "Could not launch " + cmdline[ 0 ] + ": " + exc );
        }
    }

    /**
     * Sets the commandline of the subprocess to launch.
     *
     * @param commandline the commandline of the subprocess to launch
     */
    public void setCommandline( String[] commandline )
    {
        m_command = commandline;
    }

    /**
     * Sets the environment variables for the subprocess to launch.
     *
     * @param env The new EnvironmentData value
     */
    public void setEnvironment( String[] env )
        throws TaskException
    {
        setEnvironment( Environment.createEnvVars( env ) );
    }

    public void setEnvironment( final Properties environment )
    {
        m_environment = environment;
    }

    /**
     * Set whether to propagate the default environment or not.
     *
     * @param newenv whether to propagate the process environment.
     */
    public void setNewenvironment( boolean newenv )
    {
        m_newEnvironment = newenv;
    }

    /**
     * Launch this execution through the VM, where possible, rather than through
     * the OS's shell. In some cases and operating systems using the shell will
     * allow the shell to perform additional processing such as associating an
     * executable with a script, etc
     *
     * @param useVMLauncher The new VMLauncher value
     */
    public void setVMLauncher( boolean useVMLauncher )
    {
        this.m_useVMLauncher = useVMLauncher;
    }

    /**
     * Sets the working directory of the process to execute. <p>
     *
     * @param workingDirectory the working directory of the process.
     */
    public void setWorkingDirectory( final File workingDirectory )
    {
        m_workingDirectory = workingDirectory;
    }

    /**
     * Returns the environment used to create a subprocess.
     *
     * @return the environment used to create a subprocess
     */
    public String[] getEnvironment()
        throws TaskException
    {
        if( m_environment == null || m_newEnvironment )
        {
            return Environment.toNativeFormat( m_environment );
        }
        else
        {
            try
            {
                Environment.addNativeEnvironment( m_environment );
                return Environment.toNativeFormat( m_environment );
            }
            catch( final IOException ioe )
            {
                throw new TaskException( ioe.getMessage(), ioe );
            }
        }
    }

    /**
     * query the exit value of the process.
     *
     * @return the exit value, 1 if the process was killed, or Project.INVALID
     *      if no exit value has been received
     */
    public int getExitValue()
    {
        return m_exitValue;
    }

    /**
     * Runs a process defined by the command line and returns its exit status.
     *
     * @return the exit status of the subprocess or <code>INVALID</code>
     * @exception IOException Description of Exception
     */
    public int execute()
        throws IOException, TaskException
    {
        CommandLauncher launcher = c_launcher != null ? c_launcher : c_shellLauncher;
        if( !m_useVMLauncher )
        {
            launcher = c_shellLauncher;
        }

        final ExecMetaData metaData =
            new ExecMetaData( m_command, getEnvironment(),
                              m_workingDirectory, false );
        final Process process = launcher.exec( metaData );
        try
        {
            m_streamHandler.setProcessInputStream( process.getOutputStream() );
            m_streamHandler.setProcessOutputStream( process.getInputStream() );
            m_streamHandler.setProcessErrorStream( process.getErrorStream() );
        }
        catch( IOException e )
        {
            process.destroy();
            throw e;
        }
        m_streamHandler.start();

        // add the process to the list of those to destroy if the VM exits
        //
        c_processDestroyer.add( process );

        if( m_watchdog != null )
            m_watchdog.start( process );
        waitFor( process );

        // remove the process to the list of those to destroy if the VM exits
        //
        c_processDestroyer.remove( process );

        if( m_watchdog != null )
            m_watchdog.stop();
        m_streamHandler.stop();
        if( m_watchdog != null )
            m_watchdog.checkException();
        return getExitValue();
    }

    /**
     * test for an untimely death of the process
     *
     * @return true iff a watchdog had to kill the process
     * @since 1.5
     */
    public boolean killedProcess()
    {
        return m_watchdog != null && m_watchdog.killedProcess();
    }

    private void setExitValue( final int value )
    {
        m_exitValue = value;
    }

    protected void waitFor( Process process )
    {
        try
        {
            process.waitFor();
            setExitValue( process.exitValue() );
        }
        catch( InterruptedException e )
        {
        }
    }
}
