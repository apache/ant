/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.jsp.compilers;

import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.java.ExecuteJava;
import org.apache.tools.todo.taskdefs.jsp.JspC;
import org.apache.myrmidon.framework.nativelib.Commandline;
import org.apache.myrmidon.framework.nativelib.ArgumentList;

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

        final ExecuteJava exe = new ExecuteJava();
        exe.setClassName( "org.apache.jasper.JspC" );
        if( getJspc().getClasspath() != null )
        {
            exe.getClassPath().add( getJspc().getClasspath() );
        }

        setupJasperCommand( exe.getArguments() );

        // Create an instance of the compiler, redirecting output to
        // the project log
        exe.execute( getTaskContext() );
        return true;
    }

    /*
     * ------------------------------------------------------------
     */
    private void setupJasperCommand( final ArgumentList cmd )
        throws TaskException
    {
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
    }
}
