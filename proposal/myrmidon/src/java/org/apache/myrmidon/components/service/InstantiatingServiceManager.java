/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.myrmidon.interfaces.role.RoleInfo;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.service.ServiceFactory;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * A service manager implementation, which creates service instances on demand.
 *
 * <p>This manager creates service instances, using a {@link ServiceFactory},
 * and running the service instances through the service lifecycle:
 * <ul>
 * <li>log enable
 * <li>contextualise
 * <li>service
 * <li>parameterise
 * <li>initialise
 * <li>use
 * <li>dispose
 * </ul>
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class InstantiatingServiceManager
    extends AbstractLogEnabled
    implements ServiceManager, Contextualizable, Parameterizable, Serviceable, Disposable
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( InstantiatingServiceManager.class );

    /** Map from service class -> service object. */
    private Map m_services = new HashMap();

    /** The objects (services and factories) that have been created by this mgr. */
    private List m_objects = new ArrayList();

    /** Other services used by this service manager. */
    private TypeFactory m_typeFactory;
    private RoleManager m_roleManager;
    private ServiceManager m_serviceManager;
    private Parameters m_parameters;
    private TypeManager m_typeManager;
    private Context m_context;

    public void contextualize( final Context context ) throws ContextException
    {
        m_context = context;
    }

    public void parameterize( final Parameters parameters ) throws ParameterException
    {
        m_parameters = parameters;
    }

    /**
     * Pass the <code>ServiceManager</code> to the <code>servicable</code>.
     * The <code>Servicable</code> implementation should use the specified
     * <code>ServiceManager</code> to acquire the components it needs for
     * execution.
     *
     * @param manager The <code>ServiceManager</code> which this
     *                <code>Servicable</code> uses.
     */
    public void service( final ServiceManager manager )
        throws ServiceException
    {
        m_serviceManager = manager;
        m_roleManager = (RoleManager)manager.lookup( RoleManager.ROLE );
        m_typeManager = (TypeManager)manager.lookup( TypeManager.ROLE );
    }

    /**
     * Disposes this service manager, and all services created by it.
     */
    public void dispose()
    {
        // Dispose the services
        for( Iterator iterator = m_objects.iterator(); iterator.hasNext(); )
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
        m_objects = null;
        m_parameters = null;
        m_roleManager = null;
        m_serviceManager = null;
    }

    /**
     * Determines if this service manager contains a particular service.
     */
    public boolean hasService( final String serviceRole )
    {
        // If we have already instantiated the service, or if we know how
        // to instantiate it, then return true
        if( m_services.containsKey( serviceRole ) )
        {
            return true;
        }
        try
        {
            return getFactory().canCreate( serviceRole );
        }
        catch( TypeException e )
        {
            // Throw away exception - yuck
        }
        return false;
    }

    /**
     * Locates the type factory to use to instantiate service factories.
     */
    private TypeFactory getFactory() throws TypeException
    {
        if( m_typeFactory == null )
        {
            m_typeFactory = m_typeManager.getFactory( ServiceFactory.ROLE );
        }
        return m_typeFactory;
    }

    /**
     * Locates a service instance.
     */
    public Object lookup( final String serviceRole )
        throws ServiceException
    {
        Object service = m_services.get( serviceRole );
        if( service == null )
        {
            // Create the service
            service = createService( serviceRole );
            m_services.put( serviceRole, service );
        }

        return service;
    }

    /**
     * Releases a service.
     */
    public void release( final Object service )
    {
    }

    /**
     * Creates the service object for a service role.
     */
    private Object createService( final String serviceRole ) throws ServiceException
    {
        try
        {
            // Create the factory
            final ServiceFactory factory = (ServiceFactory)getFactory().create( serviceRole );
            setupObject( factory );

            // Create the service
            final Object service = factory.createService();

            // Check the service is assignable to the role type
            final RoleInfo roleInfo = m_roleManager.getRole( serviceRole );
            final Class serviceType = roleInfo.getType();
            if( serviceType != null && !serviceType.isInstance( service ) )
            {
                final String message = REZ.getString( "mismatched-service-type.error",
                                                      serviceRole, service.getClass().getName() );
                throw new ServiceException( message );
            }

            setupObject( service );
            return service;
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "create-service.error", serviceRole );
            throw new ServiceException( message, e );
        }
    }

    /**
     * Sets-up an object, taking it through the lifecycle steps.
     */
    private void setupObject( final Object object )
        throws Exception
    {
        setupLogger( object );

        if( m_context != null && object instanceof Contextualizable )
        {
            ( (Contextualizable)object ).contextualize( m_context );
        }

        if( object instanceof Serviceable )
        {
            ( (Serviceable)object ).service( m_serviceManager );
        }

        if( m_parameters != null && object instanceof Parameterizable )
        {
            ( (Parameterizable)object ).parameterize( m_parameters );
        }

        if( object instanceof Initializable )
        {
            ( (Initializable)object ).initialize();
        }

        m_objects.add( object );
    }
}
