/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.aspects;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.log.Logger;
import org.apache.myrmidon.api.Task;
import org.apache.myrmidon.api.TaskException;

/**
 * AspectHandler is the interface through which aspects are handled.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public abstract class AbstractAspectHandler
    implements AspectHandler
{
    public Configuration preCreate( final Configuration configuration )
        throws TaskException
    {
        return configuration;
    }

    public void postCreate( final Task task )
        throws TaskException
    {
    }

    public void preLoggable( final Logger logger )
        throws TaskException
    {
    }

    public void preConfigure()
        throws TaskException
    {
    }

    public void preExecute()
        throws TaskException
    {
    }

    public void preDestroy()
        throws TaskException
    {
    }

    public boolean error( final TaskException te )
        throws TaskException
    {
        return false;
    }
}
