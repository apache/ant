/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.jsp.compilers;

import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.tools.todo.taskdefs.Java;
import org.apache.tools.todo.taskdefs.jsp.JspC;
import org.apache.tools.todo.types.Argument;
import org.apache.tools.todo.types.Commandline;

/**
 * The implementation of the jasper compiler. This is a cut-and-paste of the
 * original Jspc task.
 *
 * @author Matthew Watson <a href="mailto:mattw@i3sp.com">mattw@i3sp.com</a>
 */
public class JasperC
    extends DefaultCompilerAdapter
{
    /*
     * ------------------------------------------------------------
     */
    public boolean execute()
        throws TaskException
    {
        getTaskContext().debug( "Using jasper compiler" );
        Commandline cmd = setupJasperCommand();

        try
        {
            // Create an instance of the compiler, redirecting output to
            // the project log
            //FIXME
            Java java = null;//(Java)( getJspc().getProject() ).createTask( "java" );
            if( getJspc().getClasspath() != null )
            {
                java.addClasspath( getJspc().getClasspath() );
            }
            java.setClassname( "org.apache.jasper.JspC" );
            String args[] = cmd.getArguments();
            for( int i = 0; i < args.length; i++ )
            {
                java.addArg( new Argument( args[ i ] ) );
            }
            java.execute();
            return true;
        }
        catch( Exception ex )
        {
            if( ex instanceof TaskException )
            {
                throw (TaskException)ex;
            }
            else
            {
                throw new TaskException( "Error running jsp compiler: ",
                                         ex );
            }
        }
    }

    /*
     * ------------------------------------------------------------
     */
    private Commandline setupJasperCommand()
    {
        Commandline cmd = new Commandline();
        JspC jspc = getJspc();
        if( jspc.getDestdir() != null )
        {
            cmd.addArgument( "-d" );
            cmd.addArgument( jspc.getDestdir() );
        }
        if( jspc.getPackage() != null )
        {
            cmd.addArgument( "-p" );
            cmd.addArgument( jspc.getPackage() );
        }
        if( jspc.getVerbose() != 0 )
        {
            cmd.addArgument( "-v" + jspc.getVerbose() );
        }
        if( jspc.isMapped() )
        {
            cmd.addArgument( "-mapped" );
        }
        if( jspc.getIeplugin() != null )
        {
            cmd.addArgument( "-ieplugin" );
            cmd.addArgument( jspc.getIeplugin() );
        }
        if( jspc.getUriroot() != null )
        {
            cmd.addArgument( "-uriroot" );
            cmd.addArgument( jspc.getUriroot().toString() );
        }
        if( jspc.getUribase() != null )
        {
            cmd.addArgument( "-uribase" );
            cmd.addArgument( jspc.getUribase().toString() );
        }
        logAndAddFilesToCompile( getJspc(), getJspc().getCompileList(), cmd );
        return cmd;
    }
    /*
     * ------------------------------------------------------------
     */
}
