/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Condition;

/**
 * This is a task used to throw a TaskException.
 * Useful for forcing a build to fail on a certain condition.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant:task name="fail"
 */
public class Fail
    extends AbstractTask
{
    private String m_message;
    private Condition m_condition;

    public void setMessage( final String message )
    {
        checkNullMessage();
        m_message = message;
    }

    public void addContent( final String message )
    {
        System.out.println( message );
        checkNullMessage();
        m_message = message;
    }

    public void setIf( final String ifCondition )
    {
        checkNullCondition();
        m_condition = new Condition( true, ifCondition );
    }

    public void setUnless( final String unlessCondition )
    {
        checkNullCondition();
        m_condition = new Condition( false, unlessCondition );
    }

    public void execute()
        throws TaskException
    {
        boolean failed = true;

        if( null != m_condition )
        {
            failed = m_condition.evaluate( getContext() );
        }

        if( failed )
        {
            if( null != m_message )
            {
                throw new TaskException( m_message );
            }
            else
            {
                throw new TaskException();
            }
        }
    }

    private void checkNullMessage()
    {
        if( null != m_message )
        {
            final String message = "Message can only be set once by " +
                "either nested content or the message attribute";
            throw new IllegalStateException( message );
        }
    }

    private void checkNullCondition()
    {
        if( null != m_condition )
        {
            throw new IllegalStateException( "Condition already set!" );
        }
    }
}
