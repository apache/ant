/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import org.apache.avalon.util.StringUtil;

public class DefaultProjectListener
    implements ProjectListener
{
    protected String        m_prefix;

    public void projectStarted( final String projectName )
    {
        output( "Starting project " + projectName + "\n" );
    }

    public void projectFinished()
    {
    }
    
    public void targetStarted( final String targetName )
    {
        output( targetName + ":\n" );
    }

    public void targetFinished()
    {
    }
    
    public void taskletStarted( final String taskletName )
    {
        m_prefix = taskletName;
    }

    public void taskletFinished()
    {
        m_prefix = null;
    }

    public void log( String message )
    {
        output( message );
    }

    public void log( String message, Throwable throwable )
    {
        output( message + "\n" + StringUtil.printStackTrace( throwable, 5, true ) );
    }

    protected void output( final String data )
    {
        if( null != m_prefix ) System.out.println( "\t[" + m_prefix + "] " + data );
        else System.out.println( data );
    }
}
