/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;

/**
 * Executes a given command if the os platform is appropriate.
 *
 * @author duncan@x180.com
 * @author rubys@us.ibm.com
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:mariusz@rakiura.org">Mariusz Nowostawski</a>
 */
public class ExecTask extends Task
{

    private static String lSep = System.getProperty( "line.separator" );
    protected boolean failOnError = false;
    protected boolean newEnvironment = false;
    private Integer timeout = null;
    private Environment env = new Environment();
    protected Commandline cmdl = new Commandline();
    private FileOutputStream fos = null;
    private ByteArrayOutputStream baos = null;
    private boolean failIfExecFails = true;

    /**
     * Controls whether the VM (1.3 and above) is used to execute the command
     */
    private boolean vmLauncher = true;
    private File dir;

    private String os;
    private File out;
    private String outputprop;
    private String resultProperty;

    /**
     * The working directory of the process
     *
     * @param d The new Dir value
     */
    public void setDir( File d )
        throws TaskException
    {
        this.dir = d;
    }

    /**
     * The command to execute.
     *
     * @param value The new Executable value
     */
    public void setExecutable( String value )
        throws TaskException
    {
        cmdl.setExecutable( value );
    }

    /**
     * ant attribute
     *
     * @param flag The new FailIfExecutionFails value
     */
    public void setFailIfExecutionFails( boolean flag )
    {
        failIfExecFails = flag;
    }

    /**
     * Throw a TaskException if process returns non 0.
     *
     * @param fail The new Failonerror value
     */
    public void setFailonerror( boolean fail )
    {
        failOnError = fail;
    }

    /**
     * Use a completely new environment
     *
     * @param newenv The new Newenvironment value
     */
    public void setNewenvironment( boolean newenv )
    {
        newEnvironment = newenv;
    }

    /**
     * Only execute the process if <code>os.name</code> is included in this
     * string.
     *
     * @param os The new Os value
     */
    public void setOs( String os )
    {
        this.os = os;
    }

    /**
     * File the output of the process is redirected to.
     *
     * @param out The new Output value
     */
    public void setOutput( File out )
    {
        this.out = out;
    }

    /**
     * Property name whose value should be set to the output of the process
     *
     * @param outputprop The new Outputproperty value
     */
    public void setOutputproperty( String outputprop )
    {
        this.outputprop = outputprop;
    }

    /**
     * fill a property in with a result. when no property is defined: failure to
     * execute
     *
     * @param resultProperty The new ResultProperty value
     * @since 1.5
     */
    public void setResultProperty( String resultProperty )
    {
        this.resultProperty = resultProperty;
    }

    /**
     * Timeout in milliseconds after which the process will be killed.
     *
     * @param value The new Timeout value
     */
    public void setTimeout( Integer value )
    {
        timeout = value;
    }

    /**
     * Control whether the VM is used to launch the new process or whether the
     * OS's shell is used.
     *
     * @param vmLauncher The new VMLauncher value
     */
    public void setVMLauncher( boolean vmLauncher )
    {
        this.vmLauncher = vmLauncher;
    }

    /**
     * Add a nested env element - an environment variable.
     *
     * @param var The feature to be added to the Env attribute
     */
    public void addEnv( Environment.Variable var )
    {
        env.addVariable( var );
    }

    /**
     * Add a nested arg element - a command line argument.
     *
     * @return Description of the Returned Value
     */
    public Commandline.Argument createArg()
    {
        return cmdl.createArgument();
    }

    /**
     * Do the work.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        checkConfiguration();
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
        String myos = System.getProperty( "os.name" );
        log( "Current OS is " + myos, Project.MSG_VERBOSE );
        if( ( os != null ) && ( os.indexOf( myos ) < 0 ) )
        {
            // this command will be executed only on the specified OS
            log( "This OS, " + myos + " was not found in the specified list of valid OSes: " + os, Project.MSG_VERBOSE );
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
    protected final void runExecute( Execute exe )
        throws IOException, TaskException
    {
        int err = -1;// assume the worst

        err = exe.execute();
        //test for and handle a forced process death
        if( exe.killedProcess() )
        {
            log( "Timeout: killed the sub-process", Project.MSG_WARN );
        }
        maybeSetResultPropertyValue( err );
        if( err != 0 )
        {
            if( failOnError )
            {
                throw new TaskException( getName() + " returned: " + err );
            }
            else
            {
                log( "Result: " + err, Project.MSG_ERR );
            }
        }
        if( baos != null )
        {
            BufferedReader in =
                new BufferedReader( new StringReader( baos.toString() ) );
            String line = null;
            StringBuffer val = new StringBuffer();
            while( ( line = in.readLine() ) != null )
            {
                if( val.length() != 0 )
                {
                    val.append( lSep );
                }
                val.append( line );
            }
            project.setNewProperty( outputprop, val.toString() );
        }
    }

    /**
     * Has the user set all necessary attributes?
     *
     * @exception TaskException Description of Exception
     */
    protected void checkConfiguration()
        throws TaskException
    {
        if( cmdl.getExecutable() == null )
        {
            throw new TaskException( "no executable specified" );
        }
        if( dir != null && !dir.exists() )
        {
            throw new TaskException( "The directory you specified does not exist" );
        }
        if( dir != null && !dir.isDirectory() )
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
    protected ExecuteStreamHandler createHandler()
        throws TaskException
    {
        if( out != null )
        {
            try
            {
                fos = new FileOutputStream( out );
                log( "Output redirected to " + out, Project.MSG_VERBOSE );
                return new PumpStreamHandler( fos );
            }
            catch( FileNotFoundException fne )
            {
                throw new TaskException( "Cannot write to " + out, fne );
            }
            catch( IOException ioe )
            {
                throw new TaskException( "Cannot write to " + out, ioe );
            }
        }
        else if( outputprop != null )
        {
            baos = new ByteArrayOutputStream();
            log( "Output redirected to ByteArray", Project.MSG_VERBOSE );
            return new PumpStreamHandler( baos );
        }
        else
        {
            return new LogStreamHandler( this,
                                         Project.MSG_INFO, Project.MSG_WARN );
        }
    }

    /**
     * Create the Watchdog to kill a runaway process.
     *
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    protected ExecuteWatchdog createWatchdog()
        throws TaskException
    {
        if( timeout == null )
            return null;
        return new ExecuteWatchdog( timeout.intValue() );
    }

    /**
     * Flush the output stream - if there is one.
     */
    protected void logFlush()
    {
        try
        {
            if( fos != null )
                fos.close();
            if( baos != null )
                baos.close();
        }
        catch( IOException io )
        {
        }
    }

    /**
     * helper method to set result property to the passed in value if
     * appropriate
     *
     * @param result Description of Parameter
     */
    protected void maybeSetResultPropertyValue( int result )
    {
        String res = Integer.toString( result );
        if( resultProperty != null )
        {
            project.setNewProperty( resultProperty, res );
        }
    }

    /**
     * Create an Execute instance with the correct working directory set.
     *
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    protected Execute prepareExec()
        throws TaskException
    {
        // default directory to the project's base directory
        if( dir == null )
            dir = project.getBaseDir();
        // show the command
        log( cmdl.toString(), Project.MSG_VERBOSE );

        Execute exe = new Execute( createHandler(), createWatchdog() );
        exe.setAntRun( project );
        exe.setWorkingDirectory( dir );
        exe.setVMLauncher( vmLauncher );
        String[] environment = env.getVariables();
        if( environment != null )
        {
            for( int i = 0; i < environment.length; i++ )
            {
                log( "Setting environment variable: " + environment[ i ],
                     Project.MSG_VERBOSE );
            }
        }
        exe.setNewenvironment( newEnvironment );
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
    protected void runExec( Execute exe )
        throws TaskException
    {
        exe.setCommandline( cmdl.getCommandline() );
        try
        {
            runExecute( exe );
        }
        catch( IOException e )
        {
            if( failIfExecFails )
            {
                throw new TaskException( "Execute failed: " + e.toString(), e );
            }
            else
            {
                log( "Execute failed: " + e.toString(), Project.MSG_ERR );
            }
        }
        finally
        {
            // close the output file if required
            logFlush();
        }
    }

}
