/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.libs.selftest;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.AbstractContainerTask;

/**
 * This is to test self interpretation of configuration.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class ConfigurationTest
    extends AbstractContainerTask
    implements Configurable
{
    private String              m_message;

    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        final String message = configuration.getAttribute( "message" );
        final Object object = resolve( message );

        if( object instanceof String )
        {
            m_message = (String)object;
        }
        else
        {
            m_message = object.toString();
        }
    }

    public void execute()
        throws TaskException
    {
        getLogger().warn( m_message );
    }
}
