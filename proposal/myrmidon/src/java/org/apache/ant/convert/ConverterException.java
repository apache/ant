/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

import org.apache.myrmidon.AntException;

/**
 * ConverterException thrown when a problem occurs during convertion etc.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class ConverterException
    extends AntException
{
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
        super( message, throwable );
    }
}

