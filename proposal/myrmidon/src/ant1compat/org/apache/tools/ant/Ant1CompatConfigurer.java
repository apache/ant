/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.util.Locale;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * A helper class which uses reflection to configure any Object,
 * with the help of the Ant1 IntrospectionHelper.
 * This aims to mimic (to some extent) the Ant1-style configuration rules
 * implemented by ProjectHelperImpl.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
class Ant1CompatConfigurer
{
    private final Object m_configuredObject;
    private Configuration m_configuration;
    private final Project m_project;
    private final IntrospectionHelper m_helper;

    private Object[] m_childObjects;
    private Ant1CompatConfigurer[] m_childConfigurers;
    private String[] m_childNames;

    Ant1CompatConfigurer( Object configuredObject,
                          Configuration config,
                          Project project )
    {
        m_configuredObject = configuredObject;
        m_configuration = config;
        m_project = project;
        m_helper = IntrospectionHelper.getHelper( m_configuredObject.getClass() );
    }

    /**
     * Create all child elements, recursively.
     */
    void createChildren() throws ConfigurationException
    {
        Configuration[] childConfigs = m_configuration.getChildren();

        m_childObjects = new Object[ childConfigs.length ];
        m_childConfigurers = new Ant1CompatConfigurer[ childConfigs.length ];
        m_childNames = new String[ childConfigs.length ];

        for( int i = 0; i < childConfigs.length; i++ )
        {
            Configuration childConfig = childConfigs[ i ];
            String name = childConfig.getName();
            Object childObject =
                m_helper.createElement( m_project, m_configuredObject, name );
            Ant1CompatConfigurer childConfigurer =
                new Ant1CompatConfigurer( childObject, childConfig, m_project );

            m_childObjects[ i ] = childObject;
            m_childNames[ i ] = name;
            m_childConfigurers[ i ] = childConfigurer;

            // Recursively create children
            childConfigurer.createChildren();
        }
    }

    /**
     * Configure attributes and text, recursively.
     */
    void configure() throws ConfigurationException
    {
        // Configure the attributes.
        final String[] attribs = m_configuration.getAttributeNames();
        for( int i = 0; i < attribs.length; i++ )
        {
            final String name = attribs[ i ];
            final String value =
                m_project.replaceProperties( m_configuration.getAttribute( name ) );
            try
            {
                m_helper.setAttribute( m_project, m_configuredObject,
                                       name.toLowerCase( Locale.US ), value );
            }
            catch( BuildException be )
            {
                // id attribute must be set externally
                if( !name.equals( "id" ) )
                {
                    throw be;
                }
            }
        }

        // Configure the text content.
        String text = m_configuration.getValue( null );
        if( text != null )
        {
            m_helper.addText( m_project, m_configuredObject, text );
        }

        // Configure and add all children
        for( int i = 0; i < m_childConfigurers.length; i++ )
        {
            m_childConfigurers[ i ].configure();

            // Store child if neccessary (addConfigured)
            m_helper.storeElement( m_project, m_configuredObject,
                                   m_childObjects[ i ], m_childNames[ i ] );
        }

        // Set the reference, if id was specified.
        String id = m_configuration.getAttribute( "id", null );
        if( id != null )
        {
            m_project.addReference( id, m_configuredObject );
        }

    }
}
