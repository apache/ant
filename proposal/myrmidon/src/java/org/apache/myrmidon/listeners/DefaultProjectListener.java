/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.listeners;

import org.apache.avalon.framework.ExceptionUtil;

/**
 * Default listener that emulates the old ant listener notifications.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public final class DefaultProjectListener
    extends AbstractProjectListener
{
    private boolean m_targetOutput;

    /**
     * Notify listener of targetStarted event.
     *
     * @param target the name of target
     */
    public void targetStarted( final String target )
    {
        super.targetStarted( target );
        m_targetOutput = false;
    }

    /**
     * Notify listener of log message event.
     *
     * @param message the message
     */
    public void log( final String message )
    {
        output( message );
    }

    /**
     * Notify listener of log message event.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void log( final String message, final Throwable throwable )
    {
        output( message + "\n" + ExceptionUtil.printStackTrace( throwable, 5, true ) );
    }

    /**
     * Utility class to output data.
     * Overide in sub-classes to direct to a different destination.
     *
     * @param data the data
     */
    private void output( final String data )
    {
        if( !m_targetOutput )
        {
            System.out.println( getTarget() + ":\n" );
            m_targetOutput = true;
        }

        final String task = getTask();
        if( null != task )
        {
            System.out.println( "\t[" + task + "] " + data );
        }
        else
        {
            System.out.println( data );
        }
    }
}
