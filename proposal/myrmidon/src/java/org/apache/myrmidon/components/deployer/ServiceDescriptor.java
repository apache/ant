/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

import java.util.ArrayList;
import java.util.List;

/**
 * A typelib service descriptor, which defines a set of services.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class ServiceDescriptor
    extends TypelibDescriptor
{
    private final List m_services = new ArrayList();

    public ServiceDescriptor( final String url )
    {
        super( url );
    }

    public ServiceDefinition[] getDefinitions()
    {
        return (ServiceDefinition[])m_services.toArray
            ( new ServiceDefinition[ m_services.size() ] );
    }

    public void addDefinition( final ServiceDefinition definition )
    {
        m_services.add( definition );
    }
}
