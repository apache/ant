/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.exec;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Properties;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Argument;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.EnvironmentData;
import org.apache.tools.ant.types.EnvironmentVariable;

/**
 * Executes a given command if the os platform is appropriate.
 *
 * @author duncan@x180.com
 * @author rubys@us.ibm.com
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:mariusz@rakiura.org">Mariusz Nowostawski</a>
 */
public class ExecTask
    extends Task
{
    private boolean m_newEnvironment;
    private Integer m_timeout;
    private EnvironmentData m_env = new EnvironmentData();
    private Commandline m_command = new Commandline();
    private FileOutputStream m_ouput;
    private ByteArrayOutputStream m_byteArrayOutput;

    /**
     * Controls whether the VM (1.3 and above) is used to execute the command
     */
    private boolean m_useVMLauncher = true;
    private File m_workingDirectory;

    private String m_os;
    private File m_outputFile;
    private String m_outputProperty;
    private String m_resultProperty;

    /**
     * The working directory of the process
     *
     * @param d The new Dir value
     */
    public void setDir( final File dir )
        throws TaskException
    {
        m_workingDirectory = dir;
    }

    /**
     * The command to execute.
     */
    public void setExecutable( final String value )
        throws TaskException
    {
        m_command.setExecutable( value );
    }

    /**
     * Use a completely new environment
     */
    public void setNewenvironment( final boolean newEnvironment )
    {
        m_newEnvironment = newEnvironment;
    }

    /**
     * Only execute the process if <code>os.name</code> is included in this
     * string.
     *
     * @param os The new Os value
     */
    public void setOs( final String os )
    {
        m_os = os;
    }

    /**
     * File the output of the process is redirected to.
     *
     * @param out The new Output value
     */
    public void setOutput( final File outputFile )
    {
        m_outputFile = outputFile;
    }

    /**
     * Property name whose value should be set to the output of the process
     *
     * @param outputprop The new Outputproperty value
     */
    public void setOutputproperty( final String outputprop )
    {
        m_outputProperty = outputprop;
    }

    /**
     * fill a property in with a result. when no property is defined: failure to
     * execute
     *
     * @param resultProperty The new ResultProperty value
     * @since 1.5
     */
    public void setResultProperty( final String resultProperty )
    {
        m_resultProperty = resultProperty;
    }

    /**
     * Timeout in milliseconds after which the process will be killed.
     *
     * @param value The new Timeout value
     */
    public void setTimeout( final Integer timeout )
    {
        m_timeout = timeout;
    }

    /**
     * Control whether the VM is used to launch the new process or whether the
     * OS's shell is used.
     *
     * @param vmLauncher The new VMLauncher value
     */
    public void setVMLauncher( final boolean vmLauncher )
    {
        m_useVMLauncher = vmLauncher;
    }

    /**
     * Add a nested env element - an environment variable.
     *
     * @param var The feature to be added to the Env attribute
     */
    public void addEnv( final EnvironmentVariable var )
    {
        m_env.addVariable( var );
    }

    /**
     * Add a nested arg element - a command line argument.
     */
    public void addArg( final Argument argument )
    {
        m_command.addArgument( argument );
    }

    /**
     * Do the work.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        validate();
        if( isValidOs() )
        {
            runExec( prepareExec() );
        }
    }

    /**
     * Is this the OS the user wanted?
     *
     * @return The ValidOs value
     */
    protected boolean isValidOs()
    {
        // test if os match
        final String os = System.getProperty( "os.name" );

        getLogger().debug( "Current OS is " + os );
        if( ( m_os != null ) && ( m_os.indexOf( os ) < 0 ) )
        {
            // this command will be executed only on the specified OS
            getLogger().debug( "This OS, " + os + " was not found in the specified list of valid OSes: " + m_os );
            return false;
        }
        return true;
    }

    /**
     * A Utility method for this classes and subclasses to run an Execute
     * instance (an external command).
     *
     * @param exe Description of Parameter
     * @exception IOException Description of Exception
     */
    protected final void runExecute( final Execute exe )
        throws IOException, TaskException
    {
        final int err = exe.execute();

        //test for and handle a forced process death
        maybeSetResultPropertyValue( err );
        if( 0 != err )
        {
            throw new TaskException( getName() + " returned: " + err );
        }

        if( null != m_byteArrayOutput )
        {
            writeResultToProperty();
        }
    }

    private void writeResultToProperty() throws IOException, TaskException
    {
        final BufferedReader input =
            new BufferedReader( new StringReader( m_byteArrayOutput.toString() ) );
        String line = null;
        StringBuffer val = new StringBuffer();
        while( ( line = input.readLine() ) != null )
        {
            if( val.length() != 0 )
            {
                val.append( StringUtil.LINE_SEPARATOR );
            }
            val.append( line );
        }
        setProperty( m_outputProperty, val.toString() );
    }

    /**
     * Has the user set all necessary attributes?
     *
     * @exception TaskException Description of Exception
     */
    protected void validate()
        throws TaskException
    {
        if( m_command.getExecutable() == null )
        {
            throw new TaskException( "no executable specified" );
        }

        if( m_workingDirectory != null && !m_workingDirectory.exists() )
        {
            throw new TaskException( "The directory you specified does not exist" );
        }

        if( m_workingDirectory != null && !m_workingDirectory.isDirectory() )
        {
            throw new TaskException( "The directory you specified is not a directory" );
        }
    }

    /**
     * Create the StreamHandler to use with our Execute instance.
     *
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    private void setupOutput( final Execute exe )
        throws TaskException
    {
        if( m_outputFile != null )
        {
            try
            {
                m_ouput = new FileOutputStream( m_outputFile );
                getLogger().debug( "Output redirected to " + m_outputFile );
                exe.setOutput( m_ouput );
                exe.setError( m_ouput );
            }
            catch( FileNotFoundException fne )
            {
                throw new TaskException( "Cannot write to " + m_outputFile, fne );
            }
            catch( IOException ioe )
            {
                throw new TaskException( "Cannot write to " + m_outputFile, ioe );
            }
        }
        else if( m_outputProperty != null )
        {
            m_byteArrayOutput = new ByteArrayOutputStream();
            getLogger().debug( "Output redirected to ByteArray" );
            exe.setOutput( m_byteArrayOutput );
            exe.setError( m_byteArrayOutput );
        }
        else
        {
            exe.setOutput( new LogOutputStream( getLogger(), false ) );
            exe.setError( new LogOutputStream( getLogger(), true ) );
        }
    }

    /**
     * Flush the output stream - if there is one.
     */
    protected void logFlush()
    {
        try
        {
            if( m_ouput != null ) {
                m_ouput.close();
            }
            if( m_byteArrayOutput != null ) {
                m_byteArrayOutput.close();
            }
        }
        catch( IOException io )
        {
        }
    }

    /**
     * helper method to set result property to the passed in value if
     * appropriate
     */
    protected void maybeSetResultPropertyValue( int result )
        throws TaskException
    {
        String res = Integer.toString( result );
        if( m_resultProperty != null )
        {
            setProperty( m_resultProperty, res );
        }
    }

    /**
     * Create an Execute instance with the correct working directory set.
     */
    protected Execute prepareExec()
        throws TaskException
    {
        // default directory to the project's base directory
        if( m_workingDirectory == null ) {
          m_workingDirectory = getBaseDirectory();
        }
        // show the command
        getLogger().debug( m_command.toString() );

        final Execute exe = new Execute();
        setupOutput( exe );
        if( null != m_timeout )
        {
            exe.setTimeout( m_timeout.intValue() );
        }
        exe.setWorkingDirectory( m_workingDirectory );
        exe.setVMLauncher( m_useVMLauncher );
        exe.setNewenvironment( m_newEnvironment );

        final Properties environment = m_env.getVariables();
        final Iterator keys = environment.keySet().iterator();
        while( keys.hasNext() )
        {
            final String key = (String)keys.next();
            final String value = environment.getProperty( key );
            getLogger().debug( "Setting environment variable: " + key + "=" + value );
        }
        exe.setEnvironment( environment );
        return exe;
    }

    /**
     * Run the command using the given Execute instance. This may be overidden
     * by subclasses
     *
     * @param exe Description of Parameter
     * @exception TaskException Description of Exception
     */
    protected void runExec( final Execute exe )
        throws TaskException
    {
        exe.setCommandline( m_command.getCommandline() );
        try
        {
            runExecute( exe );
        }
        catch( IOException e )
        {
            throw new TaskException( "Execute failed: " + e.toString(), e );
        }
        finally
        {
            // close the output file if required
            logFlush();
        }
    }

    protected final Commandline getCommand()
    {
        return m_command;
    }

}
