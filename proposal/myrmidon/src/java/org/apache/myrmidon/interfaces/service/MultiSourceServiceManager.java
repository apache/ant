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

/**
 * A service manager that aggregates services from several
 * {@link AntServiceManager} objects.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class MultiSourceServiceManager
    implements AntServiceManager
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( MultiSourceServiceManager.class );

    /** The source service managers, in order. */
    private final ArrayList m_sources = new ArrayList();

    /**
     * Adds a service manager to the end of the source list.
     */
    public void add( final AntServiceManager mgr )
    {
        m_sources.add( mgr );
    }

    /**
     * Determines if this service manager contains a particular service.
     *
     * @param serviceType The service interface.
     */
    public boolean hasService( final Class serviceType )
    {
        final int size = m_sources.size();
        for( int i = 0; i < size; i++ )
        {
            final AntServiceManager serviceManager = (AntServiceManager)m_sources.get( i );
            if( serviceManager.hasService( serviceType ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Locates a service instance.
     *
     * @param serviceType The service interface.
     * @return The service instance.  The returned object is guaranteed to
     *         implement the service interface.
     * @throws AntServiceException If the service does not exist.
     */
    public Object getService( final Class serviceType )
        throws AntServiceException
    {
        final int size = m_sources.size();
        for( int i = 0; i < size; i++ )
        {
            final AntServiceManager serviceManager = (AntServiceManager)m_sources.get( i );
            if( serviceManager.hasService( serviceType ) )
            {
                return serviceManager.getService( serviceType );
            }
        }

        final String message = REZ.getString( "unknown-service.error", serviceType.getName() );
        throw new AntServiceException( message );
    }
}
