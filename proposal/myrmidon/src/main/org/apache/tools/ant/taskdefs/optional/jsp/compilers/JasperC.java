/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.jsp.compilers;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.optional.jsp.JspC;
import org.apache.tools.ant.types.Commandline;

/**
 * The implementation of the jasper compiler. This is a cut-and-paste of the
 * original Jspc task.
 *
 * @author Matthew Watson <a href="mailto:mattw@i3sp.com">mattw@i3sp.com</a>
 */
public class JasperC extends DefaultCompilerAdapter
{
    /*
     * ------------------------------------------------------------
     */
    public boolean execute()
        throws BuildException
    {
        getJspc().log( "Using jasper compiler", Project.MSG_VERBOSE );
        Commandline cmd = setupJasperCommand();

        try
        {
            // Create an instance of the compiler, redirecting output to
            // the project log
            Java java = ( Java )( getJspc().getProject() ).createTask( "java" );
            if( getJspc().getClasspath() != null )
                java.setClasspath( getJspc().getClasspath() );
            java.setClassname( "org.apache.jasper.JspC" );
            String args[] = cmd.getArguments();
            for( int i = 0; i < args.length; i++ )
                java.createArg().setValue( args[i] );
            java.setFailonerror( true );
            java.execute();
            return true;
        }
        catch( Exception ex )
        {
            if( ex instanceof BuildException )
            {
                throw ( BuildException )ex;
            }
            else
            {
                throw new BuildException( "Error running jsp compiler: ",
                    ex, getJspc().getLocation() );
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
            cmd.createArgument().setValue( "-d" );
            cmd.createArgument().setFile( jspc.getDestdir() );
        }
        if( jspc.getPackage() != null )
        {
            cmd.createArgument().setValue( "-p" );
            cmd.createArgument().setValue( jspc.getPackage() );
        }
        if( jspc.getVerbose() != 0 )
        {
            cmd.createArgument().setValue( "-v" + jspc.getVerbose() );
        }
        if( jspc.isMapped() )
        {
            cmd.createArgument().setValue( "-mapped" );
        }
        if( jspc.getIeplugin() != null )
        {
            cmd.createArgument().setValue( "-ieplugin" );
            cmd.createArgument().setValue( jspc.getIeplugin() );
        }
        if( jspc.getUriroot() != null )
        {
            cmd.createArgument().setValue( "-uriroot" );
            cmd.createArgument().setValue( jspc.getUriroot().toString() );
        }
        if( jspc.getUribase() != null )
        {
            cmd.createArgument().setValue( "-uribase" );
            cmd.createArgument().setValue( jspc.getUribase().toString() );
        }
        logAndAddFilesToCompile( getJspc(), getJspc().getCompileList(), cmd );
        return cmd;
    }
    /*
     * ------------------------------------------------------------
     */
}
