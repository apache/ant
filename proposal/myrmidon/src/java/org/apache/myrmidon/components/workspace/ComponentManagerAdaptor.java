/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.myrmidon.interfaces.service.ServiceException;
import org.apache.myrmidon.interfaces.service.ServiceManager;

/**
 * An adaptor from {@link ComponentManager} to {@link ServiceManager}.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class ComponentManagerAdaptor
    implements ServiceManager
{
    private final ComponentManager m_componentManager;

    public ComponentManagerAdaptor( final ComponentManager componentManager )
    {
        m_componentManager = componentManager;
    }

    /**
     * Determines if this service manager contains a particular service.
     */
    public boolean hasService( Class serviceType )
    {
        return m_componentManager.hasComponent( serviceType.getName() );
    }

    /**
     * Locates a service instance.
     */
    public Object getService( Class serviceType )
        throws ServiceException
    {
        try
        {
            return m_componentManager.lookup( serviceType.getName() );
        }
        catch( ComponentException e )
        {
            throw new ServiceException( e.getMessage(), e );
        }
    }
}
