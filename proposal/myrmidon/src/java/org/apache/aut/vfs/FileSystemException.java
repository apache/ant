/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

/**
 * Thrown for file system errors.
 *
 * @author Adam Murdoch
 */
public class FileSystemException
    extends Exception
{
    /**
     * The Throwable that caused this exception to be thrown.
     */
    private final Throwable m_throwable;

    /**
     * Constructs exception with the specified detail message.
     *
     * @param   message   the detail message.
     */
    public FileSystemException( final String message )
    {
        this( message, null );
    }

    /**
     * Constructs exception with the specified detail message.
     *
     * @param   message   the detail message.
     * @param   throwable the cause.
     */
    public FileSystemException( final String message,
                                final Throwable throwable )
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
