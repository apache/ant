/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

import org.apache.avalon.framework.CascadingException;

/**
 * Thrown for file system errors.
 *
 * @author Adam Murdoch
 */
public class FileSystemException extends CascadingException
{
    private Throwable m_cause;

    /**
     * Constructs exception with the specified detail message.
     *
     * @param   msg   the detail message.
     */
    public FileSystemException( String msg )
    {
        super( msg );
    }

    /**
     * Constructs exception with the specified detail message.
     *
     * @param   msg   the detail message.
     * @param   cause the cause.
     */
    public FileSystemException( String msg, Throwable cause )
    {
        super( msg, cause );
    }
}
