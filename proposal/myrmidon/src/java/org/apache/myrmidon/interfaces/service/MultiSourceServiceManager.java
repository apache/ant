/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.service;

import java.util.ArrayList;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;

/**
 * A service manager that aggregates services from several
 * {@link ServiceManager} objects.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class MultiSourceServiceManager
    implements ServiceManager
{
    private static final Resources REZ
        = ResourceManager.getPackageResources( MultiSourceServiceManager.class );

    /** The source service managers, in order. */
    private final ArrayList m_sources = new ArrayList();

    /**
     * Adds a service manager to the end of the source list.
     * @param mgr The ServiceManager to add.
     */
    public void add( final ServiceManager mgr )
    {
        m_sources.add( mgr );
    }

    /**
     * Determines if this service manager contains a particular service.
     * @param serviceRole The name of the service to check for.
     * @return <code>true</code> if this service manager contains
     *         the named service.
     */
    public boolean hasService( final String serviceRole )
    {
        final int size = m_sources.size();
        for( int i = 0; i < size; i++ )
        {
            final ServiceManager serviceManager = (ServiceManager)m_sources.get( i );
            if( serviceManager.hasService( serviceRole ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Locates a service instance.
     *
     * @param serviceRole The service interface.
     * @return The service instance.  The returned object is guaranteed to
     *         implement the service interface.
     * @throws ServiceException If the service does not exist.
     */
    public Object lookup( final String serviceRole )
        throws ServiceException
    {
        final int size = m_sources.size();
        for( int i = 0; i < size; i++ )
        {
            final ServiceManager serviceManager = (ServiceManager)m_sources.get( i );
            if( serviceManager.hasService( serviceRole ) )
            {
                return serviceManager.lookup( serviceRole );
            }
        }

        final String message = REZ.getString( "unknown-service.error", serviceRole );
        throw new ServiceException( message );
    }

    /**
     * Releases a service.
     * @param service The service to release.
     */
    public void release( final Object service )
    {
    }
}
