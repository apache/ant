/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import org.apache.myrmidon.interfaces.service.AntServiceException;
import org.apache.myrmidon.interfaces.service.AntServiceManager;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;

/**
 * An adaptor from {@link ServiceManager} to {@link AntServiceManager}.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class ServiceManagerAdaptor
    implements AntServiceManager
{
    private final ServiceManager m_serviceManager;

    public ServiceManagerAdaptor( final ServiceManager componentManager )
    {
        m_serviceManager = componentManager;
    }

    /**
     * Determines if this service manager contains a particular service.
     */
    public boolean hasService( Class serviceType )
    {
        return m_serviceManager.hasService( serviceType.getName() );
    }

    /**
     * Locates a service instance.
     */
    public Object getService( Class serviceType )
        throws AntServiceException
    {
        try
        {
            return m_serviceManager.lookup( serviceType.getName() );
        }
        catch( final ServiceException se )
        {
            throw new AntServiceException( se.getMessage(), se );
        }
    }
}
