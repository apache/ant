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
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultObjectConfigurer.class );

    private final Class m_class;

    /**
     * Adder property configurers. (For XML elements)
     */
    private final HashMap m_adders = new HashMap();

    /**
     * Setter property configurers. (For XML attributes)
     */
    private final HashMap m_setters = new HashMap();

    /**
     * The typed property configurer.
     */
    private PropertyConfigurer m_typedPropertyConfigurer;

    /**
     * Content configurer.
     */
    private PropertyConfigurer m_contentConfigurer;

    /**
     * Total number of properties.
     */
    private int m_propCount;

    /**
     * Creates an object configurer for a particular class.  The newly
     * created configurer will not handle any attributes, elements, or content.
     * Use the various <code>enable</code> methods to enable handling of these.
     */
    private DefaultObjectConfigurer( final Class classInfo )
    {
        m_class = classInfo;
    }

    /**
     * Enables all properties and content handling.
     */
    private void enableAll()
        throws ConfigurationException
    {
        enableSetters();
        enableAdders();
        enableTypedAdder();
        enableContent();
    }

    /**
     * Enables all setters.
     */
    private void enableSetters()
        throws ConfigurationException
    {
        // Locate all the setter methods
        final Collection methods = findMethods( "set", false );

        // Create a configurer for each setter
        final Iterator iterator = methods.iterator();
        while( iterator.hasNext() )
        {
            final Method method = (Method)iterator.next();
            final Class type = method.getParameterTypes()[ 0 ];
            final String propName = extractName( 3, method.getName() );

            final DefaultPropertyConfigurer setter =
                new DefaultPropertyConfigurer( getPropertyCount(),
                                               type,
                                               method,
                                               1 );
            m_setters.put( propName, setter );
        }
    }

    /**
     * Enables all adders.
     */
    private void enableAdders()
        throws ConfigurationException
    {
        // Locate all the adder methods
        final Collection methods = findMethods( "add", false );

        final Iterator iterator = methods.iterator();
        while( iterator.hasNext() )
        {
            final Method method = (Method)iterator.next();
            final String methodName = method.getName();

            // Skip the text content method
            if( methodName.equals( "addContent" ) )
            {
                continue;
            }

            final Class type = method.getParameterTypes()[ 0 ];
            final String propName = extractName( 3, methodName );

            final DefaultPropertyConfigurer configurer =
                new DefaultPropertyConfigurer( getPropertyCount(),
                                               type,
                                               method,
                                               Integer.MAX_VALUE );
            m_adders.put( propName, configurer );
        }
    }

    /**
     * Enables the typed adder.
     */
    private void enableTypedAdder()
        throws ConfigurationException
    {
        final Collection methods = findMethods( "add", true );
        if( methods.size() == 0 )
        {
            return;
        }

        final Method method = (Method)methods.iterator().next();
        final Class type = method.getParameterTypes()[ 0 ];

        // TODO - this isn't necessary
        if( !type.isInterface() )
        {
            final String message =
                REZ.getString( "typed-adder-non-interface.error",
                               m_class.getName(),
                               type.getName() );
            throw new ConfigurationException( message );
        }

        m_typedPropertyConfigurer
            = new DefaultPropertyConfigurer( getPropertyCount(),
                                             type,
                                             method,
                                             Integer.MAX_VALUE );
    }

    /**
     * Enables text content.
     */
    private void enableContent()
        throws ConfigurationException
    {
        // Locate the 'addContent' methods, which return void, and take
        // a single parameter.
        final Collection methods = findMethods( "addContent", true );
        if( methods.size() == 0 )
        {
            return;
        }

        final Method method = (Method)methods.iterator().next();
        final Class type = method.getParameterTypes()[ 0 ];
        m_contentConfigurer = new DefaultPropertyConfigurer( getPropertyCount(),
                                                             type,
                                                             method,
                                                             1 );
    }

    /**
     * Locate all methods whose name starts with a particular
     * prefix, and which are non-static, return void, and take a single
     * non-array parameter.  If there are more than one matching methods of
     * a given name, the method that takes a String parameter (if any) is
     * ignored.  If after that there are more than one matching methods of
     * a given name, an exception is thrown.
     *
     * @return Map from property name -> Method object for that property.
     */
    private Collection findMethods( final String prefix,
                                    final boolean exactMatch )
        throws ConfigurationException
    {
        final Map methods = new HashMap();
        final List allMethods = findMethodsWithPrefix( prefix, exactMatch );

        final Iterator iterator = allMethods.iterator();
        while( iterator.hasNext() )
        {
            final Method method = (Method)iterator.next();
            final String methodName = method.getName();
            if( Void.TYPE != method.getReturnType() ||
                1 != method.getParameterTypes().length ||
                method.getParameterTypes()[ 0 ].isArray() )
            {
                continue;
            }

            // Extract property name
            final Class type = method.getParameterTypes()[ 0 ];

            // Add to the adders map
            if( methods.containsKey( methodName ) )
            {
                final Method candidate = (Method)methods.get( methodName );
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
                    // Both are string (which would be odd), or both are not string
                    final String message =
                        REZ.getString( "multiple-methods-for-element.error",
                                       m_class.getName(),
                                       methodName );
                    throw new ConfigurationException( message );
                }

                // Else, current type is string, and new type is not, so
                // continue below, and replace the current method
            }

            methods.put( methodName, method );
        }

        return methods.values();
    }

    private int getPropertyCount()
    {
        return m_propCount++;
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
        return new ConfigurationState( this, object, getPropertyCount() );
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
    public PropertyConfigurer getAdder( final String name )
    {
        return (PropertyConfigurer)m_adders.get( name );
    }

    /**
     * Returns a configurer for an element of this class.
     */
    public PropertyConfigurer getSetter( final String name )
    {
        return (PropertyConfigurer)m_setters.get( name );
    }

    /**
     * Returns a configurer for the typed property of this class.
     */
    public PropertyConfigurer getTypedProperty()
    {
        return m_typedPropertyConfigurer;
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
    private List findMethodsWithPrefix( final String prefix,
                                        final boolean exactMatch )
    {
        final ArrayList matches = new ArrayList();
        final int prefixLen = prefix.length();
        final Method[] methods = m_class.getMethods();
        for( int i = 0; i < methods.length; i++ )
        {
            final Method method = methods[ i ];
            final String methodName = method.getName();
            if( Modifier.isStatic( method.getModifiers() ) )
            {
                continue;
            }
            if( methodName.length() < prefixLen || !methodName.startsWith( prefix ) )
            {
                continue;
            }
            if( exactMatch && methodName.length() != prefixLen )
            {
                continue;
            }

            matches.add( method );
        }
        return matches;
    }
}
