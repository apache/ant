/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.converter;

/**
 * ConverterException thrown when a problem occurs during convertion etc.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ConverterException
    extends Exception
{
    /**
     * The Throwable that caused this exception to be thrown.
     */
    private final Throwable m_throwable;

    /**
     * Basic constructor with a message
     *
     * @param message the message
     */
    public ConverterException( final String message )
    {
        this( message, null );
    }

    /**
     * Constructor that builds cascade so that other exception information can be retained.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public ConverterException( final String message, final Throwable throwable )
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

