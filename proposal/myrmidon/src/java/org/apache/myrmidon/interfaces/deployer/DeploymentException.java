/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.deployer;

import org.apache.avalon.framework.CascadingException;

/**
 * Exception to indicate error deploying.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public final class DeploymentException
    extends CascadingException
{
    /**
     * Construct a new <code>DeploymentException</code> instance.
     *
     * @param message The detail message for this exception.
     */
    public DeploymentException( final String message )
    {
        this( message, null );
    }

    /**
     * Construct a new <code>DeploymentException</code> instance.
     *
     * @param message The detail message for this exception.
     * @param throwable the root cause of the exception
     */
    public DeploymentException( final String message, final Throwable throwable )
    {
        super( message, throwable );
    }
}
