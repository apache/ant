/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.ConfigurationException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An object configurer which uses reflection to determine the properties
 * of a class.
 *
 * @author <a href="mailto:adammurdoch_ml@yahoo.com">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class DefaultObjectConfigurer
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
        // TODO - get rid of creators, and either setter or adders
        enableAdders();
        enableContent();
    }

    /**
     * Enables all creators + adders.
     */
    public void enableAdders()
        throws ConfigurationException
    {
        final Map creators = findCreators();
        final Map adders = findAdders();

        // Add the elements
        final Set elemNames = new HashSet();
        elemNames.addAll( creators.keySet() );
        elemNames.addAll( adders.keySet() );

        final Iterator iterator = elemNames.iterator();
        while( iterator.hasNext() )
        {
            final String propName = (String)iterator.next();
            final Method createMethod = (Method)creators.get( propName );
            final Method addMethod = (Method)adders.get( propName );

            // Determine and check the return type
            Class type;
            if( createMethod != null && addMethod != null )
            {
                // Make sure the add method is more general than the create
                // method
                type = createMethod.getReturnType();
                final Class addType = addMethod.getParameterTypes()[ 0 ];
                if( !addType.isAssignableFrom( type ) )
                {
                    final String message =
                        REZ.getString( "incompatible-element-types.error",
                                       propName,
                                       m_class.getName() );
                    throw new ConfigurationException( message );
                }
            }
            else if( createMethod != null )
            {
                type = createMethod.getReturnType();
            }
            else
            {
                type = addMethod.getParameterTypes()[ 0 ];
            }

            final DefaultPropertyConfigurer configurer =
                new DefaultPropertyConfigurer( type, createMethod, addMethod );
            m_props.put( propName, configurer );
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
            if( method.getReturnType() != Void.TYPE ||
                method.getParameterTypes().length != 1 )
            {
                continue;
            }

            // TODO - un-hard-code this
            if( methodName.equals( "addContent" ) )
            {
                continue;
            }

            // Extract element name
            final String elemName = extractName( 3, methodName );

            // Add to the adders map
            if( adders.containsKey( elemName ) )
            {
                final String message =
                    REZ.getString( "multiple-adder-methods-for-element.error",
                                   m_class.getName(),
                                   elemName );
                throw new ConfigurationException( message );
            }
            adders.put( elemName, method );
        }
        return adders;
    }

    /**
     * Find all 'create' methods, which return a non-primitive type,
     *  and take no parameters.
     */
    private Map findCreators()
        throws ConfigurationException
    {
        final Map creators = new HashMap();
        final List methodSet = new ArrayList();
        findMethodsWithPrefix( "create", methodSet );

        final Iterator iterator = methodSet.iterator();
        while( iterator.hasNext() )
        {
            final Method method = (Method)iterator.next();
            final String methodName = method.getName();
            if( method.getReturnType().isPrimitive() ||
                method.getParameterTypes().length != 0 )
            {
                continue;
            }

            // Extract element name
            final String elemName = extractName( 6, methodName );

            // Add to the creators map
            if( creators.containsKey( elemName ) )
            {
                final String message =
                    REZ.getString( "multiple-creator-methods-for-element.error",
                                   m_class.getName(),
                                   elemName );
                throw new ConfigurationException( message );
            }
            creators.put( elemName, method );
        }
        return creators;
    }

    /**
     * Enables content.
     */
    public void enableContent()
        throws ConfigurationException
    {
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

            Class type = method.getParameterTypes()[0];
            m_contentConfigurer = new DefaultPropertyConfigurer( type, null, method );
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
     * Returns the class.
     */
    public Class getType()
    {
        return m_class;
    }

    /**
     * Returns a configurer for an element of this class.
     */
    public PropertyConfigurer getProperty( final String name )
    {
        return (PropertyConfigurer)m_props.get( name );
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
        for( int i = 0; i < sb.length(); i++ )
        {
            char ch = sb.charAt( i );
            if( Character.isUpperCase( ch ) )
            {
                if( i > 0 )
                {
                    sb.insert( i, '-' );
                    i++;
                }
                sb.setCharAt( i, Character.toLowerCase( ch ) );
            }
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
                methodName.length() <= prefixLen ||
                !methodName.startsWith( prefix ) )
            {
                continue;
            }

            matches.add( method );
        }
    }
}
