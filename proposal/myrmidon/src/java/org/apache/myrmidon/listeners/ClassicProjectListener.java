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
 * Classic listener that emulates the default ant1.x listener notifications.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public final class ClassicProjectListener
    extends AbstractProjectListener
{
    /**
     * Notify listener of targetStarted event.
     *
     * @param target the name of target
     */
    public void targetStarted( final String target )
    {
        output( target + ":\n" );
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
    private void output( final String data )
    {
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
