/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.service;

import org.apache.avalon.framework.CascadingException;

/**
 * ServiceException thrown when a service can not be created for
 * some reason.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ServiceException
    extends CascadingException
{
    /**
     * Basic constructor for exception that does not specify a message
     */
    public ServiceException()
    {
        this( "", null );
    }

    /**
     * Basic constructor with a message
     *
     * @param message the message
     */
    public ServiceException( final String message )
    {
        this( message, null );
    }

    /**
     * Constructor that builds cascade so that other exception information can be retained.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public ServiceException( final String message, final Throwable throwable )
    {
        super( message, throwable );
    }
}

