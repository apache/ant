/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.service;

/**
 * ServiceException thrown when a service can not be created for
 * some reason.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class AntServiceException
    extends Exception
{
    /**
     * The Throwable that caused this exception to be thrown.
     */
    private final Throwable m_throwable;

    /**
     * Basic constructor for exception that does not specify a message
     */
    public AntServiceException()
    {
        this( "", null );
    }

    /**
     * Basic constructor with a message
     *
     * @param message the message
     */
    public AntServiceException( final String message )
    {
        this( message, null );
    }

    /**
     * Constructor that builds cascade so that other exception information can be retained.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public AntServiceException( final String message, final Throwable throwable )
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

