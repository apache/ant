/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon;

import java.util.ArrayList;
import java.util.List;
import org.apache.myrmidon.listeners.LogEvent;

/**
 * Asserts that log messages are delivered in the correct order.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class LogMessageTracker
    extends TrackingProjectListener
{
    private List m_targets = new ArrayList();
    private List m_messages = new ArrayList();

    /**
     * Handles a log message.
     */
    public void log( final LogEvent event )
    {
        super.log( event );

        // Pop the next expected message off the list, and make sure it
        // matches the message in the event
        assertTrue( "Unexpected log message", m_targets.size() > 0 && m_messages.size() > 0 );
        assertEquals( "Unexpected log message", m_targets.remove( 0 ), event.getTargetName() );
        assertEquals( "Unexpected log message", m_messages.remove( 0 ), event.getMessage() );
    }

    /**
     * Asserts that all the log messages were delivered.
     */
    public void assertComplete()
    {
        super.assertComplete();

        // Make sure that all log messages were delivered
        assertTrue( "Log message not delivered", m_targets.size() == 0 && m_messages.size() == 0 );
    }

    /**
     * Adds an expected log message.
     */
    public void addExpectedMessage( String target, String message )
    {
        m_targets.add( target );
        m_messages.add( message );
    }
}
