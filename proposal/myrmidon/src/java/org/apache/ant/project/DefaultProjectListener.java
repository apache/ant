/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import org.apache.avalon.framework.ExceptionUtil;

/**
 * Default listener that emulates the old ant listener notifications.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultProjectListener
    implements ProjectListener
{
    protected String        m_prefix;

    /**
     * Notify listener of projectStarted event.
     *
     * @param projectName the projectName
     */
    public void projectStarted( final String projectName )
    {
        output( "Starting project " + projectName + "\n" );
    }
    
    /**
     * Notify listener of projectFinished event.
     */
    public void projectFinished()
    {
    }
    
    /**
     * Notify listener of targetStarted event.
     *
     * @param targetName the name of target
     */
    public void targetStarted( final String targetName )
    {
        output( targetName + ":\n" );
    }
    
    /**
     * Notify listener of targetFinished event.
     */
    public void targetFinished()
    {
    }
    
    /**
     * Notify listener of taskletStarted event.
     *
     * @param taskletName the name of tasklet
     */
    public void taskletStarted( final String taskletName )
    {
        m_prefix = taskletName;
    }
    
    /**
     * Notify listener of taskletFinished event.
     */
    public void taskletFinished()
    {
        m_prefix = null;
    }
    
    /**
     * Notify listener of log message event.
     *
     * @param message the message
     */
    public void log( String message )
    {
        output( message );
    }
    
    /**
     * Notify listener of log message event.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void log( String message, Throwable throwable )
    {
        output( message + "\n" + ExceptionUtil.printStackTrace( throwable, 5, true ) );
    }

    /**
     * Utility class to output data.
     * Overide in sub-classes to direct to a different destination.
     *
     * @param data the data
     */
    protected void output( final String data )
    {
        if( null != m_prefix ) System.out.println( "\t[" + m_prefix + "] " + data );
        else System.out.println( data );
    }
}
