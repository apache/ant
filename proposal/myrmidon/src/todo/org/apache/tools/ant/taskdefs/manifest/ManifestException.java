/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.manifest;

import org.apache.avalon.framework.CascadingException;

/**
 * ManifestException is thrown when there is a problem parsing, generating or
 * handling a Manifest.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ManifestException
    extends CascadingException
{
    /**
     * Basic constructor for exception that does not specify a message
     */
    public ManifestException()
    {
        this( "", null );
    }

    /**
     * Basic constructor with a message
     *
     * @param message the message
     */
    public ManifestException( final String message )
    {
        this( message, null );
    }

    /**
     * Constructor that builds cascade so that other exception information can be retained.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public ManifestException( final String message, final Throwable throwable )
    {
        super( message, throwable );
    }
}
