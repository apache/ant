/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.myrmidon.api.TaskException;

/**
 * Abstract task used to write tasks that delegate to facades
 * such as Javac, Jspc and so forth.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractFacadeTask
    extends AbstractContainerTask
    implements Configurable
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( AbstractFacadeTask.class );

    /**
     * The name of the attribute used to select specific
     * implementation of facade.
     */
    private final String m_selector;

    /**
     * The Class type for the facade (also used as the role
     * when looking up TypeManager).
     */
    private final Class m_facadeType;

    /**
     * The default name of implementation if none are specified.
     */
    private final String m_defaultName;

    /**
     * The configuration used to configure the facade implementation.
     */
    private Configuration m_configuration;

    /**
     * Create the facade task that works with specified facade class,
     * using the selector attribute to find implementation or using
     * defaultName if selector attribute not specified.
     */
    protected AbstractFacadeTask( final String selector,
                                  final Class facadeType,
                                  final String defaultName )
    {
        m_selector = selector;
        m_facadeType = facadeType;
        m_defaultName = defaultName;
    }

    /**
     * Supply the configuration for this task.
     */
    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        m_configuration = configuration;
    }

    /**
     * Utility method to create and configure the facade
     * implementation.
     */
    protected Object prepareFacade()
        throws TaskException
    {
        final Object facade = createFacade();
        configureFacade( facade );
        return facade;
    }

    /**
     * Utility method to configure the specified facade.
     * It will be configured according to normal resolution
     * rules using the configuration data supplied to task
     * minus the selector attribute if present.
     */
    protected void configureFacade( final Object facade )
        throws TaskException
    {
        Configuration configuration = m_configuration;

        if( null != m_selector &&
            null != m_configuration.getAttribute( m_selector, null ) )
        {
            configuration = rebuildConfiguration( m_configuration, m_selector );
        }

        try
        {
            configureElement( facade, m_facadeType, configuration );
        }
        catch( final ConfigurationException ce )
        {
            throw new TaskException( ce.getMessage(), ce );
        }
    }

    /**
     * Rebuild the configuration tree with the attribute with specified
     * name removed from top-level element.
     */
    private Configuration rebuildConfiguration( final Configuration configuration,
                                                final String attribute )
    {
        final DefaultConfiguration newConfiguration =
            new DefaultConfiguration( configuration.getName(),
                                      configuration.getLocation() );

        //Add all the attributes from old configuration except the one
        //that was used to select the particular implementation
        final String[] names = configuration.getAttributeNames();
        for( int i = 0; i < names.length; i++ )
        {
            final String name = names[ i ];
            if( !name.equals( attribute ) )
            {
                final String value = configuration.getAttribute( name, null );
                newConfiguration.setAttribute( name, value );
            }
        }

        //Add all elements to new configuration element in the
        //correct order
        final Configuration[] children = configuration.getChildren();
        for( int i = 0; i < children.length; i++ )
        {
            newConfiguration.addChild( children[ i ] );
        }

        return newConfiguration;
    }

    /**
     * Create the instance of the facade. It looks up the name
     * of the implementation via the <code>getImplementation()</code>
     * method and then creates a new instance from a TypeFactory
     * using that name and the facadeType (specified in the
     * constructor).
     */
    protected Object createFacade()
        throws TaskException
    {
        final String implementation = getImplementation();
        if( null == implementation )
        {
            final String message =
                REZ.getString( "facade.missing-impl.error", getContext().getName() );
            throw new TaskException( message );
        }
        return newInstance( m_facadeType, implementation );
    }

    /**
     * Get the shortname of the implementation
     * to use. It assumes that the implementation is registered in
     * the TypeFactory under this shortname.
     */
    protected String getImplementation()
    {
        if( null != m_selector )
        {
            return m_configuration.getAttribute( m_selector, m_defaultName );
        }
        else
        {
            return m_defaultName;
        }
    }
}
