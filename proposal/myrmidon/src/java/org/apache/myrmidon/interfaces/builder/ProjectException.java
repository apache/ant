/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.builder;

import org.apache.myrmidon.interfaces.ComponentException;

/**
 * A cascading exception thrown on a problem constructing a Project model.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class ProjectException
    extends ComponentException
{
    /**
     * Constructs a non-cascaded exception.
     *
     * @param message The detail message for this exception.
     */
    public ProjectException( final String message )
    {
        super( message );
    }

    /**
     * Constructs a cascaded exception.
     *
     * @param message The detail message for this exception.
     * @param throwable the root cause of the exception
     */
    public ProjectException( final String message, final Throwable throwable )
    {
        super( message, throwable );
    }
}
