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
 * Default listener that emulates the Ant 1.x no banner listener.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultProjectListener
    extends ClassicProjectListener
{
    private boolean m_targetOutput;

    /**
     * Notify listener of targetStarted event.
     */
    public void targetStarted( final TargetEvent target )
    {
        m_targetOutput = false;
    }

    /**
     * Notify listener of targetFinished event.
     */
    public void targetFinished( final TargetEvent event )
    {
        if( m_targetOutput )
        {
            getWriter().println();
        }
    }

    /**
     * Notify listener of log message event.
     */
    public void log( final LogEvent event )
    {
        // Write the target header, if necessary
        final String target = event.getTargetName();
        if( target != null && !m_targetOutput )
        {
            writeTargetHeader( event );
            m_targetOutput = true;
        }

        // Write the message
        super.log( event );
    }
}
