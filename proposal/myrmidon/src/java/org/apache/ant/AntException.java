/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant;

import org.apache.avalon.framework.CascadingRuntimeException;

/**
 * AntException thrown when a problem with tasks etc.
 * It is cascading so that further embedded information can be contained. 
 * ie ANtException was caused by IOException etc.
 * It is RuntimeException as it has to pass through a number of Java-defined
 * interfaces - ala Runnable and also to aid in ease of indicating an error.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class AntException
    extends CascadingRuntimeException
{
    /**
     * Basic constructor with a message
     *
     * @param message the message 
     */
    public AntException( final String message )
    {
        this( message, null );
    }

    /**
     * Constructor that builds cascade so that other exception information can be retained.
     *
     * @param message the message 
     * @param throwable the throwable
     */
    public AntException( final String message, final Throwable throwable )
    {
        super( message, throwable );
    }
}

