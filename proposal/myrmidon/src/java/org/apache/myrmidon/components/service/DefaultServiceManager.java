/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.myrmidon.interfaces.service.ServiceException;
import org.apache.myrmidon.interfaces.service.ServiceFactory;
import org.apache.myrmidon.interfaces.service.ServiceManager;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * A service manager implementation.  This implementation takes care of
 * creating service instances, using a {@link ServiceFactory}, and running the
 * service instances through the service lifecycle.  Service creation happens
 * on demand.
 *
 * <p>This implementation uses a TypeManager to locate the service factories.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class DefaultServiceManager
    implements ServiceManager, Composable, Disposable
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( DefaultServiceManager.class );

    /** Map from service class -> service object. */
    private Map m_services = new HashMap();

    private TypeFactory m_typeFactory;

    /**
     * Locate the components used by this service manager.
     */
    public void compose( final ComponentManager componentManager ) throws ComponentException
    {
        final TypeManager typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
        try
        {
            m_typeFactory = typeManager.getFactory( ServiceFactory.class );
        }
        catch( final TypeException e )
        {
            throw new ComponentException( e.getMessage(), e );
        }
    }

    /**
     * Disposes this service manager, and all services created by it.
     */
    public void dispose()
    {
        // Dispose the services
        for( Iterator iterator = m_services.values().iterator(); iterator.hasNext(); )
        {
            final Object object = iterator.next();
            if( object instanceof Disposable )
            {
                ( (Disposable)object ).dispose();
            }
        }

        // Ditch state
        m_services = null;
        m_typeFactory = null;
    }

    /**
     * Determines if this service manager contains a particular service.
     */
    public boolean hasService( Class serviceType )
    {
        // If we have already instantiated the service, or if we know how
        // to instantiate it, then return true
        if( m_services.containsKey( serviceType ) )
        {
            return true;
        }
        if( m_typeFactory.canCreate( serviceType.getName() ) )
        {
            return true;
        }

        return false;
    }

    /**
     * Locates a service instance.
     */
    public Object getService( Class serviceType )
        throws ServiceException
    {
        Object service = m_services.get( serviceType );
        if( service == null )
        {
            // Create the service
            service = createService( serviceType );
            m_services.put( serviceType, service );
        }

        return service;
    }

    /**
     * Creates the service object for a service class.
     */
    private Object createService( Class serviceType ) throws ServiceException
    {
        try
        {
            final ServiceFactory factory = (ServiceFactory)m_typeFactory.create( serviceType.getName() );

            // Create the service
            final Object service = factory.createService();
            if( ! serviceType.isInstance( service ) )
            {
                final String message = REZ.getString( "mismatched-service-type.error", serviceType.getName(), service.getClass().getName() );
                throw new ServiceException( message );
            }
            return service;
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "create-service.error", serviceType.getName() );
            throw new ServiceException( message, e );
        }
    }
}
