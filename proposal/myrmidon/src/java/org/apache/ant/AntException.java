/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant;

import org.apache.avalon.CascadingRuntimeException;

public class AntException
    extends CascadingRuntimeException
{
    public AntException( final String message )
    {
        this( message, null );
    }

    public AntException( final String message, final Throwable throwable )
    {
        super( message, throwable );
    }
}

