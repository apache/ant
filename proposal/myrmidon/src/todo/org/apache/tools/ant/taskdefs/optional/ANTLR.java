/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.taskdefs.ExecuteJava;
import org.apache.tools.ant.taskdefs.exec.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;

/**
 * ANTLR task.
 *
 * @author <a href="mailto:emeade@geekfarm.org">Erik Meade</a>
 * @author <a href="mailto:sbailliez@apache.org>Stephane Bailliez</a>
 */
public class ANTLR extends Task
{

    private CommandlineJava commandline = new CommandlineJava();

    /**
     * should fork ?
     */
    private boolean fork = false;

    /**
     * working directory
     */
    private File workingdir = null;

    /**
     * where to output the result
     */
    private File outputDirectory;

    /**
     * the file to process
     */
    private File target;

    public ANTLR()
    {
        commandline.setVm( "java" );
        commandline.setClassname( "antlr.Tool" );
    }

    /**
     * The working directory of the process
     *
     * @param d The new Dir value
     */
    public void setDir( File d )
    {
        this.workingdir = d;
    }

    public void setFork( boolean s )
    {
        this.fork = s;
    }

    public void setOutputdirectory( File outputDirectory )
    {
        log( "Setting output directory to: " + outputDirectory.toString(), Project.MSG_VERBOSE );
        this.outputDirectory = outputDirectory;
    }

    public void setTarget( File target )
    {
        log( "Setting target to: " + target.toString(), Project.MSG_VERBOSE );
        this.target = target;
    }

    /**
     * <code>&lt;classpath&gt;</code> allows classpath to be set because a
     * directory might be given for Antlr debug...
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
    {
        return commandline.createClasspath( project ).createPath();
    }

    /**
     * Create a new JVM argument. Ignored if no JVM is forked.
     *
     * @return create a new JVM argument so that any argument can be passed to
     *      the JVM.
     * @see #setFork(boolean)
     */
    public Commandline.Argument createJvmarg()
    {
        return commandline.createVmArgument();
    }

    public void execute()
        throws TaskException
    {
        //Adds the jars or directories containing Antlr this should make the forked
        //JVM work without having to specify it directly.
        addClasspathEntry( "/antlr/Tool.class" );

        validateAttributes();

        //TODO: use ANTLR to parse the grammer file to do this.
        if( target.lastModified() > getGeneratedFile().lastModified() )
        {
            commandline.createArgument().setValue( "-o" );
            commandline.createArgument().setValue( outputDirectory.toString() );
            commandline.createArgument().setValue( target.toString() );

            if( fork )
            {
                log( "Forking " + commandline.toString(), Project.MSG_VERBOSE );
                int err = run( commandline.getCommandline() );
                if( err == 1 )
                {
                    throw new TaskException( "ANTLR returned: " + err );
                }
            }
            else
            {
                ExecuteJava exe = new ExecuteJava();
                exe.setJavaCommand( commandline.getJavaCommand() );
                exe.setClasspath( commandline.getClasspath() );
                exe.execute( project );
            }
        }
    }

    /**
     * Search for the given resource and add the directory or archive that
     * contains it to the classpath. <p>
     *
     * Doesn't work for archives in JDK 1.1 as the URL returned by getResource
     * doesn't contain the name of the archive.</p>
     *
     * @param resource The feature to be added to the ClasspathEntry attribute
     */
    protected void addClasspathEntry( String resource )
    {
        URL url = getClass().getResource( resource );
        if( url != null )
        {
            String u = url.toString();
            if( u.startsWith( "jar:file:" ) )
            {
                int pling = u.indexOf( "!" );
                String jarName = u.substring( 9, pling );
                log( "Implicitly adding " + jarName + " to classpath",
                     Project.MSG_DEBUG );
                createClasspath().setLocation( new File( ( new File( jarName ) ).getAbsolutePath() ) );
            }
            else if( u.startsWith( "file:" ) )
            {
                int tail = u.indexOf( resource );
                String dirName = u.substring( 5, tail );
                log( "Implicitly adding " + dirName + " to classpath",
                     Project.MSG_DEBUG );
                createClasspath().setLocation( new File( ( new File( dirName ) ).getAbsolutePath() ) );
            }
            else
            {
                log( "Don\'t know how to handle resource URL " + u,
                     Project.MSG_DEBUG );
            }
        }
        else
        {
            log( "Couldn\'t find " + resource, Project.MSG_DEBUG );
        }
    }

    private File getGeneratedFile()
        throws TaskException
    {
        String generatedFileName = null;
        try
        {
            BufferedReader in = new BufferedReader( new FileReader( target ) );
            String line;
            while( ( line = in.readLine() ) != null )
            {
                int extendsIndex = line.indexOf( " extends " );
                if( line.startsWith( "class " ) && extendsIndex > -1 )
                {
                    generatedFileName = line.substring( 6, extendsIndex ).trim();
                    break;
                }
            }
            in.close();
        }
        catch( Exception e )
        {
            throw new TaskException( "Unable to determine generated class", e );
        }
        if( generatedFileName == null )
        {
            throw new TaskException( "Unable to determine generated class" );
        }
        return new File( outputDirectory, generatedFileName + ".java" );
    }

    /**
     * execute in a forked VM
     *
     * @param command Description of Parameter
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    private int run( String[] command )
        throws TaskException
    {
        Execute exe = new Execute( new LogStreamHandler( this, Project.MSG_INFO,
                                                         Project.MSG_WARN ), null );
        exe.setAntRun( project );
        if( workingdir != null )
        {
            exe.setWorkingDirectory( workingdir );
        }
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

    private void validateAttributes()
        throws TaskException
    {
        if( target == null || !target.isFile() )
        {
            throw new TaskException( "Invalid target: " + target );
        }

        // if no output directory is specified, used the target's directory
        if( outputDirectory == null )
        {
            String fileName = target.toString();
            setOutputdirectory( new File( target.getParent() ) );
        }
        if( !outputDirectory.isDirectory() )
        {
            throw new TaskException( "Invalid output directory: " + outputDirectory );
        }
    }
}
