/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.exec.Execute2;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * BorlandGenerateClient is dedicated to the Borland Application Server 4.5 This
 * task generates the client jar using as input the ejb jar file. Two mode are
 * available: java mode (default) and fork mode. With the fork mode, it is
 * impossible to add classpath to the commmand line.
 *
 * @author <a href="mailto:benoit.moussaud@criltelecom.com">Benoit Moussaud</a>
 */
public class BorlandGenerateClient extends Task
{
    final static String JAVA_MODE = "java";
    final static String FORK_MODE = "fork";

    /**
     * debug the generateclient task
     */
    boolean debug = false;

    /**
     * hold the ejbjar file name
     */
    File ejbjarfile = null;

    /**
     * hold the client jar file name
     */
    File clientjarfile = null;

    /**
     * hold the mode (java|fork)
     */
    String mode = JAVA_MODE;

    /**
     * hold the classpath
     */
    Path classpath;

    public void setClasspath( Path classpath )
        throws TaskException
    {
        if( this.classpath == null )
        {
            this.classpath = classpath;
        }
        else
        {
            this.classpath.append( classpath );
        }
    }

    public void setClientjar( File clientjar )
    {
        clientjarfile = clientjar;
    }

    public void setDebug( boolean debug )
    {
        this.debug = debug;
    }

    public void setEjbjar( File ejbfile )
    {
        ejbjarfile = ejbfile;
    }

    public void setMode( String s )
    {
        mode = s;
    }

    public Path createClasspath()
        throws TaskException
    {
        if( this.classpath == null )
        {
            this.classpath = new Path();
        }
        Path path1 = this.classpath;
        final Path path = new Path();
        path1.addPath( path );
        return path;
    }

    /**
     * Do the work. The work is actually done by creating a separate JVM to run
     * a java task.
     *
     * @exception TaskException if someting goes wrong with the build
     */
    public void execute()
        throws TaskException
    {
        if( ejbjarfile == null ||
            ejbjarfile.isDirectory() )
        {
            throw new TaskException( "invalid ejb jar file." );
        }// end of if ()

        if( clientjarfile == null ||
            clientjarfile.isDirectory() )
        {
            getLogger().debug( "invalid or missing client jar file." );
            String ejbjarname = ejbjarfile.getAbsolutePath();
            //clientname = ejbjarfile+client.jar
            String clientname = ejbjarname.substring( 0, ejbjarname.lastIndexOf( "." ) );
            clientname = clientname + "client.jar";
            clientjarfile = new File( clientname );

        }// end of if ()

        if( mode == null )
        {
            getLogger().info( "mode is null default mode  is java" );
            setMode( JAVA_MODE );
        }// end of if ()

        getLogger().info( "client jar file is " + clientjarfile );

        if( mode.equalsIgnoreCase( FORK_MODE ) )
        {
            executeFork();
        }// end of if ()
        else
        {
            executeJava();
        }// end of else
    }

    /**
     * launch the generate client using system api
     *
     * @exception TaskException Description of Exception
     */
    protected void executeFork()
        throws TaskException
    {
        try
        {

            final Commandline cmd = buildCommand();

            getLogger().info( "mode : fork" );
            getLogger().debug( "Calling java2iiop" );

            final Execute2 exe = new Execute2();
            setupLogger( exe );
            exe.setWorkingDirectory( new File( "." ) );
            exe.setCommandline( cmd.getCommandline() );
            exe.execute();
        }
        catch( Exception e )
        {
            // Have to catch this because of the semantics of calling main()
            String msg = "Exception while calling generateclient Details: " + e.toString();
            throw new TaskException( msg, e );
        }

    }

    private Commandline buildCommand()
    {
        final Commandline cmd = new Commandline();
        cmd.setExecutable( "iastool" );
        cmd.addArgument( "generateclient" );
        if( debug )
        {
            cmd.addArgument( "-trace" );
        }

        cmd.addArgument( "-short" );
        cmd.addArgument( "-jarfile" );
        // ejb jar file
        cmd.addArgument( ejbjarfile.getAbsolutePath() );
        //client jar file
        cmd.addArgument( "-single" );
        cmd.addArgument( "-clientjarfile" );
        cmd.addArgument( clientjarfile.getAbsolutePath() );
        return cmd;
    }

    /**
     * launch the generate client using java api
     *
     * @exception TaskException Description of Exception
     */
    protected void executeJava()
        throws TaskException
    {
        try
        {
            getLogger().info( "mode : java" );

            org.apache.tools.ant.taskdefs.Java execTask = null;
            execTask = null;//(Java)getProject().createTask( "java" );

            execTask.setDir( new File( "." ) );
            execTask.setClassname( "com.inprise.server.commandline.EJBUtilities" );
            //classpath
            //add at the end of the classpath
            //the system classpath in order to find the tools.jar file
            // TODO - make sure tools.jar is in the classpath
            //execTask.addClasspath( classpath.concatSystemClasspath( "last" ) );

            execTask.setFork( true );
            execTask.createArg().setValue( "generateclient" );
            if( debug )
            {
                execTask.createArg().setValue( "-trace" );
            }// end of if ()

            //
            execTask.createArg().setValue( "-short" );
            execTask.createArg().setValue( "-jarfile" );
            // ejb jar file
            execTask.createArg().setValue( ejbjarfile.getAbsolutePath() );
            //client jar file
            execTask.createArg().setValue( "-single" );
            execTask.createArg().setValue( "-clientjarfile" );
            execTask.createArg().setValue( clientjarfile.getAbsolutePath() );

            getLogger().debug( "Calling EJBUtilities" );
            execTask.execute();

        }
        catch( Exception e )
        {
            // Have to catch this because of the semantics of calling main()
            String msg = "Exception while calling generateclient Details: " + e.toString();
            throw new TaskException( msg, e );
        }
    }
}
