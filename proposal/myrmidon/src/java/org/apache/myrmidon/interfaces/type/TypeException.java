/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.type;

/**
 * Exception to indicate problem with type instantiating.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public final class TypeException
    extends Exception
{
    /**
     * The Throwable that caused this exception to be thrown.
     */
    private final Throwable m_throwable;

    /**
     * Construct a new <code>TypeException</code> instance.
     *
     * @param message The detail message for this exception.
     */
    public TypeException( final String message )
    {
        this( message, null );
    }

    /**
     * Construct a new <code>TypeException</code> instance.
     *
     * @param message The detail message for this exception.
     * @param throwable the root cause of the exception
     */
    public TypeException( final String message, final Throwable throwable )
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
