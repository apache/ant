/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

/**
 * Exception thrown when evaluating a property.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class PropertyException
    extends Exception
{
    /**
     * Basic constructor for exception that does not specify a message
     */
    public PropertyException()
    {
        this( "" );
    }

    /**
     * Basic constructor with a message
     *
     * @param message the message
     */
    public PropertyException( final String message )
    {
        super( message );
    }
}

