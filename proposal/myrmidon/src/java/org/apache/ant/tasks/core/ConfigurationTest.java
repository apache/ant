/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasks.core;

import org.apache.ant.AntException;
import org.apache.ant.configuration.Configurable;
import org.apache.ant.configuration.Configuration;
import org.apache.ant.tasklet.AbstractTasklet;
import org.apache.avalon.ConfigurationException;

/**
 * This is abstract base class for tasklets.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class ConfigurationTest 
    extends AbstractTasklet
    implements Configurable
{
    protected String              m_message;

    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        String message = configuration.getAttribute( "message" );
        final Object object = getContext().resolveValue( message );
        if( object instanceof String )
        {
            m_message = (String)object;
        }
        else
        {
            m_message = object.toString();
        }
    }

    public void run()
        throws AntException
    {
        getLogger().warn( m_message );
    }
}
