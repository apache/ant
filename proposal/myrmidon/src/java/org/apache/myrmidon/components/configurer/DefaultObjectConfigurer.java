/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * An object configurer which uses reflection to determine the properties
 * of a class.
 *
 * @author <a href="mailto:adammurdoch_ml@yahoo.com">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class DefaultObjectConfigurer
    implements ObjectConfigurer
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultObjectConfigurer.class );

    private final Class m_class;

    /**
     * Map from lowercase property name -> PropertyConfigurer.
     */
    private final Map m_props = new HashMap();

    /**
     * All property configurers.
     */
    private final List m_allProps = new ArrayList();

    /**
     * The typed property configurer.
     */
    private PropertyConfigurer m_typedPropConfigurer;

    /**
     * Content configurer.
     */
    private PropertyConfigurer m_contentConfigurer;

    /**
     * Creates an object configurer for a particular class.  The newly
     * created configurer will not handle any attributes, elements, or content.
     * Use the various <code>enable</code> methods to enable handling of these.
     */
    public DefaultObjectConfigurer( final Class classInfo )
    {
        m_class = classInfo;
    }

    /**
     * Enables all properties and content handling.
     */
    public void enableAll()
        throws ConfigurationException
    {
        enableProperties();
        enableContent();
    }

    /**
     * Enables all adders.
     */
    private void enableProperties()
        throws ConfigurationException
    {
        final Map adders = findAdders();

        // Add the elements

        final Iterator iterator = adders.keySet().iterator();
        while( iterator.hasNext() )
        {
            final String propName = (String)iterator.next();
            final Method addMethod = (Method)adders.get( propName );

            // Determine and check the return type
            final Class type = addMethod.getParameterTypes()[ 0 ];
            final boolean isTypedProp = ( propName.length() == 0 );
            if( isTypedProp && !type.isInterface() )
            {
                final String message =
                    REZ.getString( "typed-adder-non-interface.error",
                                   m_class.getName(),
                                   type.getName() );
                throw new ConfigurationException( message );
            }

            // Determine the max count for the property
            int maxCount = Integer.MAX_VALUE;
            if( addMethod != null && addMethod.getName().startsWith( "set" ) )
            {
                maxCount = 1;
            }

            final DefaultPropertyConfigurer configurer =
                new DefaultPropertyConfigurer( m_allProps.size(),
                                               type,
                                               addMethod,
                                               maxCount );
            m_allProps.add( configurer );
            if( isTypedProp )
            {
                m_typedPropConfigurer = configurer;
            }
            else
            {
                m_props.put( propName, configurer );
            }
        }
    }

    /**
     * Locate all 'add' and 'set' methods which return void, and take a
     * single parameter.
     */
    private Map findAdders()
        throws ConfigurationException
    {
        final Map adders = new HashMap();
        final List methodSet = new ArrayList();
        findMethodsWithPrefix( "add", methodSet );
        findMethodsWithPrefix( "set", methodSet );

        final Iterator iterator = methodSet.iterator();
        while( iterator.hasNext() )
        {
            final Method method = (Method)iterator.next();
            final String methodName = method.getName();
            if( Void.TYPE != method.getReturnType() ||
                1 != method.getParameterTypes().length )
            {
                continue;
            }

            // Skip the text content method
            if( methodName.equals( "addContent" ) )
            {
                continue;
            }

            // Extract property name
            final String propName = extractName( 3, methodName );
            final Class type = method.getParameterTypes()[ 0 ];

            // Add to the adders map
            if( adders.containsKey( propName ) )
            {
                final Method candidate = (Method)adders.get( propName );
                final Class currentType = candidate.getParameterTypes()[ 0 ];

                // Ditch the string version, if any
                if( currentType != String.class && type == String.class )
                {
                    // New type is string, and current type is not.  Ignore
                    // the new method
                    continue;
                }
                else if( currentType != String.class || type == String.class )
                {
                    // Both are string, or both are not string
                    final String message =
                        REZ.getString( "multiple-adder-methods-for-element.error",
                                       m_class.getName(),
                                       propName );
                    throw new ConfigurationException( message );
                }

                // Else, current type is string, and new type is not, so
                // continue below, and overwrite the current method
            }

            adders.put( propName, method );
        }
        return adders;
    }

    /**
     * Enables content.
     */
    private void enableContent()
        throws ConfigurationException
    {
        // TODO - should be using 'setContent', rather than 'addContent',
        // to better match the call-at-most-once semantics of the other
        // setter methods

        // Locate any 'addContent' methods, which return void, and take
        // a single parameter.
        final Method[] methods = m_class.getMethods();
        for( int i = 0; i < methods.length; i++ )
        {
            final Method method = methods[ i ];
            final String methodName = method.getName();
            if( Modifier.isStatic( method.getModifiers() ) ||
                !methodName.equals( "addContent" ) ||
                method.getReturnType() != Void.TYPE ||
                method.getParameterTypes().length != 1 )
            {
                continue;
            }

            // Check for multiple content setters
            if( null != m_contentConfigurer )
            {
                final String message =
                    REZ.getString( "multiple-content-setter-methods.error", m_class.getName() );
                throw new ConfigurationException( message );
            }

            final Class type = method.getParameterTypes()[ 0 ];
            m_contentConfigurer =
                new DefaultPropertyConfigurer( m_allProps.size(),
                                               type,
                                               method,
                                               1 );
            m_allProps.add( m_contentConfigurer );
        }
    }

    /**
     * Locates the configurer for a particular class.
     */
    public static ObjectConfigurer getConfigurer( final Class classInfo )
        throws ConfigurationException
    {
        final DefaultObjectConfigurer configurer = new DefaultObjectConfigurer( classInfo );
        configurer.enableAll();
        return configurer;
    }

    /**
     * Starts the configuration of an object.
     */
    public ConfigurationState startConfiguration( Object object )
        throws ConfigurationException
    {
        return new ConfigurationState( this, object, m_allProps.size() );
    }

    /**
     * Finishes the configuration of an object, performing any final
     * validation and type conversion.
     */
    public Object finishConfiguration( final ConfigurationState state )
        throws ConfigurationException
    {
        // Make sure there are no pending created objects
        final ConfigurationState defState = (ConfigurationState)state;
        return defState.getObject();
    }

    /**
     * Returns a configurer for an element of this class.
     */
    public PropertyConfigurer getProperty( final String name )
    {
        return (PropertyConfigurer)m_props.get( name );
    }

    /**
     * Returns a configurer for the typed property of this class.
     */
    public PropertyConfigurer getTypedProperty()
    {
        return m_typedPropConfigurer;
    }

    /**
     * Returns a configurer for the content of this class.
     */
    public PropertyConfigurer getContentConfigurer()
    {
        return m_contentConfigurer;
    }

    /**
     * Extracts a property name from a Java method name.
     *
     * <p>Removes the prefix, inserts '-' before each uppercase character
     * (except the first), then converts all to lowercase.
     */
    private String extractName( final int prefixLen, final String methodName )
    {
        final StringBuffer sb = new StringBuffer( methodName );
        sb.delete( 0, prefixLen );

        //Contains the index that we are up to in string buffer.
        //May not be equal to i as length of string buffer may change
        int index = 0;

        final int size = sb.length();
        for( int i = 0; i < size; i++ )
        {
            char ch = sb.charAt( index );
            if( Character.isUpperCase( ch ) )
            {
                if( index > 0 )
                {
                    sb.insert( index, '-' );
                    index++;
                }
                sb.setCharAt( index, Character.toLowerCase( ch ) );
            }
            index++;
        }
        return sb.toString();
    }

    /**
     * Locates all non-static methods whose name starts with a particular
     * prefix.
     */
    private void findMethodsWithPrefix( final String prefix, final Collection matches )
    {
        final int prefixLen = prefix.length();
        final Method[] methods = m_class.getMethods();
        for( int i = 0; i < methods.length; i++ )
        {
            final Method method = methods[ i ];
            final String methodName = method.getName();
            if( Modifier.isStatic( method.getModifiers() ) ||
                methodName.length() < prefixLen ||
                !methodName.startsWith( prefix ) )
            {
                continue;
            }

            matches.add( method );
        }
    }
}
