/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.taskdefs.exec.LogOutputStream;
import org.apache.tools.ant.types.Argument;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.EnvironmentVariable;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * This task acts as a loader for java applications but allows to use the same
 * JVM for the called application thus resulting in much faster operation.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Java extends Task
{

    private CommandlineJava cmdl = new CommandlineJava();
    private boolean fork = false;
    private File dir = null;
    private PrintStream outStream = null;
    private boolean failOnError = false;
    private File out;

    /**
     * Set the class name.
     *
     * @param s The new Classname value
     * @exception TaskException Description of Exception
     */
    public void setClassname( String s )
        throws TaskException
    {
        if( cmdl.getJar() != null )
        {
            throw new TaskException( "Cannot use 'jar' and 'classname' attributes in same command" );
        }
        cmdl.setClassname( s );
    }

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param s The new Classpath value
     */
    public void setClasspath( Path s )
        throws TaskException
    {
        createClasspath().append( s );
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     *
     * @param r The new ClasspathRef value
     */
    public void setClasspathRef( Reference r )
        throws TaskException
    {
        createClasspath().setRefid( r );
    }

    /**
     * The working directory of the process
     *
     * @param d The new Dir value
     */
    public void setDir( File d )
    {
        this.dir = d;
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
     * Set the forking flag.
     *
     * @param s The new Fork value
     */
    public void setFork( boolean s )
    {
        this.fork = s;
    }

    /**
     * set the jar name...
     *
     * @param jarfile The new Jar value
     * @exception TaskException Description of Exception
     */
    public void setJar( File jarfile )
        throws TaskException
    {
        if( cmdl.getClassname() != null )
        {
            throw new TaskException( "Cannot use 'jar' and 'classname' attributes in same command." );
        }
        cmdl.setJar( jarfile.getAbsolutePath() );
    }

    /**
     * Set the command used to start the VM (only if fork==false).
     *
     * @param s The new Jvm value
     */
    public void setJvm( String s )
    {
        cmdl.setVm( s );
    }

    /**
     * -mx or -Xmx depending on VM version
     *
     * @param max The new Maxmemory value
     */
    public void setMaxmemory( String max )
    {
        cmdl.setMaxmemory( max );
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
     * Add a nested sysproperty element.
     *
     * @param sysp The feature to be added to the Sysproperty attribute
     */
    public void addSysproperty( EnvironmentVariable sysp )
    {
        cmdl.addSysproperty( sysp );
    }

    /**
     * Clear out the arguments to this java task.
     */
    public void clearArgs()
    {
        cmdl.clearJavaArgs();
    }

    /**
     * Creates a nested arg element.
     *
     * @return Description of the Returned Value
     */
    public Argument createArg()
    {
        return cmdl.createArgument();
    }

    /**
     * Creates a nested classpath element
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
        throws TaskException
    {
        return cmdl.createClasspath( getProject() ).createPath();
    }

    /**
     * Creates a nested jvmarg element.
     *
     * @return Description of the Returned Value
     */
    public Argument createJvmarg()
    {
        return cmdl.createVmArgument();
    }

    /**
     * Do the execution.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        int err = -1;
        if( ( err = executeJava() ) != 0 )
        {
            if( failOnError )
            {
                throw new TaskException( "Java returned: " + err );
            }
            else
            {
                getLogger().error( "Java Result: " + err );
            }
        }
    }

    /**
     * Do the execution and return a return code.
     *
     * @return the return code from the execute java class if it was executed in
     *      a separate VM (fork = "yes").
     * @exception TaskException Description of Exception
     */
    public int executeJava()
        throws TaskException
    {
        String classname = cmdl.getClassname();
        if( classname == null && cmdl.getJar() == null )
        {
            throw new TaskException( "Classname must not be null." );
        }
        if( !fork && cmdl.getJar() != null )
        {
            throw new TaskException( "Cannot execute a jar in non-forked mode. Please set fork='true'. " );
        }

        if( fork )
        {
            getLogger().debug( "Forking " + cmdl.toString() );

            return run( cmdl.getCommandline() );
        }
        else
        {
            if( cmdl.getVmCommand().size() > 1 )
            {
                getLogger().warn( "JVM args ignored when same JVM is used." );
            }
            if( dir != null )
            {
                getLogger().warn( "Working directory ignored when same JVM is used." );
            }

            getLogger().debug( "Running in same VM " + cmdl.getJavaCommand().toString() );
            run( cmdl );
            return 0;
        }
    }

    protected void handleErrorOutput( String line )
    {
        if( outStream != null )
        {
            outStream.println( line );
        }
        else
        {
            super.handleErrorOutput( line );
        }
    }

    protected void handleOutput( String line )
    {
        if( outStream != null )
        {
            outStream.println( line );
        }
        else
        {
            super.handleOutput( line );
        }
    }

    /**
     * Executes the given classname with the given arguments as it was a command
     * line application.
     *
     * @param classname Description of Parameter
     * @param args Description of Parameter
     * @exception TaskException Description of Exception
     */
    protected void run( String classname, ArrayList args )
        throws TaskException
    {
        CommandlineJava cmdj = new CommandlineJava();
        cmdj.setClassname( classname );
        for( int i = 0; i < args.size(); i++ )
        {
            cmdj.createArgument().setValue( (String)args.get( i ) );
        }
        run( cmdj );
    }

    /**
     * Executes the given classname with the given arguments as it was a command
     * line application.
     *
     * @param command Description of Parameter
     * @exception TaskException Description of Exception
     */
    private void run( CommandlineJava command )
        throws TaskException
    {
        ExecuteJava exe = new ExecuteJava();
        exe.setJavaCommand( command.getJavaCommand() );
        exe.setClasspath( command.getClasspath() );
        exe.setSystemProperties( command.getSystemProperties() );
        if( out != null )
        {
            try
            {
                outStream = new PrintStream( new FileOutputStream( out ) );
                exe.execute( getProject() );
            }
            catch( IOException io )
            {
                throw new TaskException( "Error", io );
            }
            finally
            {
                if( outStream != null )
                {
                    outStream.close();
                }
            }
        }
        else
        {
            exe.execute( getProject() );
        }
    }

    /**
     * Executes the given classname with the given arguments in a separate VM.
     *
     * @param command Description of Parameter
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    private int run( String[] command )
        throws TaskException
    {
        FileOutputStream fos = null;
        try
        {
            Execute exe = null;
            if( out == null )
            {
                exe = new Execute();
                exe.setOutput( new LogOutputStream( getLogger(), false ) );
                exe.setError( new LogOutputStream( getLogger(), true ) );

            }
            else
            {
                fos = new FileOutputStream( out );
                exe = new Execute();
                exe.setOutput( fos );
                exe.setError( fos );
            }

            if( dir == null )
            {
                dir = getBaseDirectory();
            }
            else if( !dir.exists() || !dir.isDirectory() )
            {
                throw new TaskException( dir.getAbsolutePath() + " is not a valid directory" );
            }

            exe.setWorkingDirectory( dir );

            exe.setCommandline( command );
            try
            {
                return exe.execute();
            }
            catch( IOException e )
            {
                throw new TaskException( "Error", e );
            }
        }
        catch( IOException io )
        {
            throw new TaskException( "Error", io );
        }
        finally
        {
            if( fos != null )
            {
                try
                {
                    fos.close();
                }
                catch( IOException io )
                {
                }
            }
        }
    }
}
