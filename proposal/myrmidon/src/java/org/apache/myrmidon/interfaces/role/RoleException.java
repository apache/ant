/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.role;

import org.apache.avalon.framework.CascadingException;

/**
 * An exception thrown by the RoleManager.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class RoleException
    extends CascadingException
{
    public RoleException( String s )
    {
        super( s );
    }

    public RoleException( String s, Throwable throwable )
    {
        super( s, throwable );
    }
}
