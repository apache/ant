/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.exec;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Os;
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
    /**
     * Invalid exit code.
     */
    public final static int INVALID = Integer.MAX_VALUE;

    protected static String c_antWorkingDirectory = System.getProperty( "user.dir" );
    private static CommandLauncher c_vmLauncher;
    private static CommandLauncher c_shellLauncher;
    private static Vector c_procEnvironment;

    /**
     * Used to destroy processes when the VM exits.
     */
    private static ProcessDestroyer c_processDestroyer = new ProcessDestroyer();

    private String[] m_command;
    private String[] m_environment;
    private int m_exitValue = INVALID;
    private File m_workingDirectory;
    private Project m_project;
    private boolean m_newEnvironment;

    /**
     * Controls whether the VM is used to launch commands, where possible
     */
    private boolean m_useVMLauncher = true;
    private ExecuteStreamHandler m_streamHandler;
    private ExecuteWatchdog m_watchdog;

    /**
     * Builds a command launcher for the OS and JVM we are running under
     */
    static
    {

        try
        {
            // Try using a JDK 1.3 launcher
            try
            {
                c_vmLauncher = new Java13CommandLauncher();
            }
            catch( NoSuchMethodException exc )
            {
                // Ignore and keep try
            }

            if( Os.isFamily( "mac" ) )
            {
                // Mac
                c_shellLauncher = new MacCommandLauncher( new CommandLauncher() );
            }
            else if( Os.isFamily( "os/2" ) )
            {
                // OS/2 - use same mechanism as Windows 2000
                c_shellLauncher = new WinNTCommandLauncher( new CommandLauncher() );
            }
            else if( Os.isFamily( "windows" ) )
            {
                // Windows.  Need to determine which JDK we're running in

                CommandLauncher baseLauncher;
                if( System.getProperty( "java.version" ).startsWith( "1.1" ) )
                {
                    // JDK 1.1
                    baseLauncher = new Java11CommandLauncher();
                }
                else
                {
                    // JDK 1.2
                    baseLauncher = new CommandLauncher();
                }

                // Determine if we're running under 2000/NT or 98/95
                String osname =
                    System.getProperty( "os.name" ).toLowerCase( Locale.US );

                if( osname.indexOf( "nt" ) >= 0 || osname.indexOf( "2000" ) >= 0 )
                {
                    // Windows 2000/NT
                    c_shellLauncher = new WinNTCommandLauncher( baseLauncher );
                }
                else
                {
                    // Windows 98/95 - need to use an auxiliary script
                    c_shellLauncher = new ScriptCommandLauncher( "bin/antRun.bat", baseLauncher );
                }
            }
            else if( (new Os( "netware" )).eval() )
            {
                // NetWare.  Need to determine which JDK we're running in
                CommandLauncher baseLauncher;
                if( System.getProperty( "java.version" ).startsWith( "1.1" ) )
                {
                    // JDK 1.1
                    baseLauncher = new Java11CommandLauncher();
                }
                else
                {
                    // JDK 1.2
                    baseLauncher = new CommandLauncher();
                }

                c_shellLauncher = new PerlScriptCommandLauncher( "bin/antRun.pl", baseLauncher );
            }
            else
            {
                // Generic
                c_shellLauncher = new ScriptCommandLauncher( "bin/antRun", new CommandLauncher() );
            }
        }
        catch( TaskException e )
        {
            e.printStackTrace();
        }
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
        this.m_streamHandler = streamHandler;
        this.m_watchdog = watchdog;
    }

    /**
     * Find the list of environment variables for this process.
     *
     * @return The ProcEnvironment value
     */
    public static synchronized Vector getProcEnvironment()
        throws TaskException
    {
        if( c_procEnvironment != null )
            return c_procEnvironment;

        c_procEnvironment = new Vector();
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Execute exe = new Execute( new PumpStreamHandler( out ) );
            exe.setCommandline( getProcEnvCommand() );
            // Make sure we do not recurse forever
            exe.setNewenvironment( true );
            int retval = exe.execute();
            if( retval != 0 )
            {
                // Just try to use what we got
            }

            BufferedReader in =
                new BufferedReader( new StringReader( out.toString() ) );
            String var = null;
            String line;
            String lineSep = System.getProperty( "line.separator" );
            while( (line = in.readLine()) != null )
            {
                if( line.indexOf( '=' ) == -1 )
                {
                    // Chunk part of previous env var (UNIX env vars can
                    // contain embedded new lines).
                    if( var == null )
                    {
                        var = lineSep + line;
                    }
                    else
                    {
                        var += lineSep + line;
                    }
                }
                else
                {
                    // New env var...append the previous one if we have it.
                    if( var != null )
                    {
                        c_procEnvironment.addElement( var );
                    }
                    var = line;
                }
            }
            // Since we "look ahead" before adding, there's one last env var.
            c_procEnvironment.addElement( var );
        }
        catch( IOException exc )
        {
            exc.printStackTrace();
            // Just try to see how much we got
        }
        return c_procEnvironment;
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
            exe.setAntRun( task.getProject() );
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

    private static String[] getProcEnvCommand()
    {
        if( Os.isFamily( "os/2" ) )
        {
            // OS/2 - use same mechanism as Windows 2000
            // Not sure
            String[] cmd = {"cmd", "/c", "set"};
            return cmd;
        }
        else if( Os.isFamily( "windows" ) )
        {
            String osname =
                System.getProperty( "os.name" ).toLowerCase( Locale.US );
            // Determine if we're running under 2000/NT or 98/95
            if( osname.indexOf( "nt" ) >= 0 || osname.indexOf( "2000" ) >= 0 )
            {
                // Windows 2000/NT
                String[] cmd = {"cmd", "/c", "set"};
                return cmd;
            }
            else
            {
                // Windows 98/95 - need to use an auxiliary script
                String[] cmd = {"command.com", "/c", "set"};
                return cmd;
            }
        }
        else if( Os.isFamily( "unix" ) )
        {
            // Generic UNIX
            // Alternatively one could use: /bin/sh -c env
            String[] cmd = {"/usr/bin/env"};
            return cmd;
        }
        else if( Os.isFamily( "netware" ) )
        {
            String[] cmd = {"env"};
            return cmd;
        }
        else
        {
            // MAC OS 9 and previous
            // TODO: I have no idea how to get it, someone must fix it
            String[] cmd = null;
            return cmd;
        }
    }

    /**
     * Set the name of the antRun script using the project's value.
     *
     * @param project the current project.
     * @exception TaskException Description of Exception
     */
    public void setAntRun( Project project )
        throws TaskException
    {
        this.m_project = project;
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
     * @param env The new Environment value
     */
    public void setEnvironment( String[] env )
    {
        this.m_environment = env;
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
     * This is emulated using the antRun scripts unless the OS is Windows NT in
     * which case a cmd.exe is spawned, or MRJ and setting user.dir works, or
     * JDK 1.3 and there is official support in java.lang.Runtime.
     *
     * @param wd the working directory of the process.
     */
    public void setWorkingDirectory( File wd )
    {
        if( wd == null || wd.getAbsolutePath().equals( c_antWorkingDirectory ) )
            m_workingDirectory = null;
        else
            m_workingDirectory = wd;
    }

    /**
     * Returns the commandline used to create a subprocess.
     *
     * @return the commandline used to create a subprocess
     */
    public String[] getCommandline()
    {
        return m_command;
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
            return m_environment;
        return patchEnvironment();
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
        CommandLauncher launcher = c_vmLauncher != null ? c_vmLauncher : c_shellLauncher;
        if( !m_useVMLauncher )
        {
            launcher = c_shellLauncher;
        }

        final Process process = launcher.exec( m_project, getCommandline(), getEnvironment(), m_workingDirectory );
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

    /**
     * Patch the current environment with the new values from the user.
     *
     * @return the patched environment
     */
    private String[] patchEnvironment()
        throws TaskException
    {
        Vector osEnv = (Vector)getProcEnvironment().clone();
        for( int i = 0; i < m_environment.length; i++ )
        {
            int pos = m_environment[ i ].indexOf( '=' );
            // Get key including "="
            String key = m_environment[ i ].substring( 0, pos + 1 );
            int size = osEnv.size();
            for( int j = 0; j < size; j++ )
            {
                if( ((String)osEnv.elementAt( j )).startsWith( key ) )
                {
                    osEnv.removeElementAt( j );
                    break;
                }
            }
            osEnv.addElement( m_environment[ i ] );
        }
        String[] result = new String[ osEnv.size() ];
        osEnv.copyInto( result );
        return result;
    }

}
