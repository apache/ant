/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces;

/**
 * An exception thrown by a Myrmidon component.  This is a convenience
 * class, which can be sub-classes to create exceptions for specific components.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ComponentException
    extends Exception
{
    /**
     * The Throwable that caused this exception to be thrown.
     */
    private final Throwable m_throwable;

    /**
     * Constructs a non-cascaded exception.
     *
     * @param message The detail message for this exception.
     */
    public ComponentException( final String message )
    {
        this( message, null );
    }

    /**
     * Constructs a cascaded exception.
     *
     * @param message The detail message for this exception.
     * @param throwable the root cause of the exception
     */
    public ComponentException( final String message, final Throwable throwable )
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
