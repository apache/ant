/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.builder;

/**
 * A cascading exception thrown on a problem constructing a Project model.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class ProjectException
    extends Exception
{
    /**
     * If this exception is cascaded, the cause of this exception.
     */
    private final Throwable m_throwable;

    /**
     * Constructs an non-cascaded exception with a message
     *
     * @param message the message
     */
    public ProjectException( final String message )
    {
        this( message, null );
    }

    /**
     * Constructs a cascaded exception with the supplied message, which links the
     * Throwable provided.
     *
     * @param message the message
     * @param throwable the throwable that caused this exception
     */
    public ProjectException( final String message, final Throwable throwable )
    {
        super( message );
        m_throwable = throwable;
    }

    /**
     * Retrieve root cause of the exception.
     *
     * @return the root cause
     */
    public final Throwable getCause()
    {
        return m_throwable;
    }
}
