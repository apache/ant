/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import java.util.HashMap;
import java.util.Map;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.property.PropertyUtil;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.converter.MasterConverter;

/**
 * Class used to configure tasks.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class DefaultConfigurer
    extends AbstractLogEnabled
    implements Configurer, Composable, LogEnabled
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultConfigurer.class );

    ///Compile time constant to turn on extreme debugging
    private final static boolean DEBUG = false;

    ///Converter to use for converting between values
    private MasterConverter m_converter;

    ///Cached object configurers.  This is a map from Class to the
    ///ObjectConfigurer for that class.
    private Map m_configurerCache = new HashMap();

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_converter = (MasterConverter)componentManager.lookup( MasterConverter.ROLE );
    }

    /**
     * Configure a task based on a configuration in a particular context.
     * This configuring can be done in different ways for different
     * configurers.
     * This one does it by first checking if object implements Configurable
     * and if it does will pass the task the configuration - else it will use
     * mapping rules to map configuration to types
     *
     * @param object the object
     * @param configuration the configuration
     * @param context the Context
     * @exception ConfigurationException if an error occurs
     */
    public void configure( final Object object,
                           final Configuration configuration,
                           final Context context )
        throws ConfigurationException
    {
        if( DEBUG )
        {
            final String message = REZ.getString( "configuring-object.notice", object );
            getLogger().debug( message );
        }

        if( object instanceof Configurable )
        {
            if( DEBUG )
            {
                final String message = REZ.getString( "configurable.notice" );
                getLogger().debug( message );
            }

            // Let the object configure itself
            ( (Configurable)object ).configure( configuration );
        }
        else
        {
            if( DEBUG )
            {
                final String message = REZ.getString( "reflection.notice" );
                getLogger().debug( message );
            }

            // Locate the configurer for this object
            final ObjectConfigurer configurer = getConfigurer( object.getClass() );

            // Set each of the attributes
            final String[] attributes = configuration.getAttributeNames();
            for( int i = 0; i < attributes.length; i++ )
            {
                final String name = attributes[ i ];
                final String value = configuration.getAttribute( name );

                // Set the attribute
                setAttribute( configurer, object, name, value, context );
            }

            // Set the text content
            final String content = configuration.getValue( null );
            if( null != content && content.length() > 0 )
            {
                setContent( configurer, object, content, context );
            }

            // Create and configure each of the child elements
            final Configuration[] children = configuration.getChildren();
            for( int i = 0; i < children.length; i++ )
            {
                final Configuration childConfig = children[ i ];
                configureElement( configurer, object, childConfig, context );
            }
        }
    }

    /**
     * Sets the text content of an object.
     */
    private void setContent( final ObjectConfigurer configurer,
                             final Object object,
                             final String content,
                             final Context context )
        throws ConfigurationException
    {
        if( DEBUG )
        {
            final String message =
                REZ.getString( "configure-content.notice", content );
            getLogger().debug( message );
        }

        // Set the content
        final AttributeSetter setter = configurer.getContentSetter();
        if( null == setter )
        {
            final String message = REZ.getString( "content-not-supported.error" );
            throw new ConfigurationException( message );
        }
        try
        {
            setValue( setter, object, content, context );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "bad-set-content.error" );
            throw new ConfigurationException( message, e );
        }
    }

    /**
     * Creates and configures a nested element
     */
    private void configureElement( final ObjectConfigurer configurer,
                                   final Object object,
                                   final Configuration childConfig,
                                   final Context context )
        throws ConfigurationException
    {
        final String childName = childConfig.getName();

        if( DEBUG )
        {
            final String message =
                REZ.getString( "configure-subelement.notice", childName );
            getLogger().debug( message );
        }

        // Locate the configurer for the child element
        final ElementConfigurer childConfigurer = configurer.getElement( childName );
        if( null == childConfigurer )
        {
            final String message = REZ.getString( "unknown-subelement.error", childName );
            throw new ConfigurationException( message );
        }

        try
        {
            // Create the child element
            final Object child = childConfigurer.createElement( object );

            // Configure the child element
            configure( child, childConfig, context );

            // Set the child element
            childConfigurer.addElement( object, child );
        }
        catch( final ConfigurationException ce )
        {
            final String message =
                REZ.getString( "bad-configure-subelement.error", childName );
            throw new ConfigurationException( message, ce );
        }
    }

    /**
     * Configure named attribute of object in a particular context.
     * This configuring can be done in different ways for different
     * configurers.
     *
     * @param object the object
     * @param name the attribute name
     * @param value the attribute value
     * @param context the Context
     * @exception ConfigurationException if an error occurs
     */
    public void configure( final Object object,
                           final String name,
                           final String value,
                           final Context context )
        throws ConfigurationException
    {
        // Locate the configurer for this object
        final ObjectConfigurer configurer = getConfigurer( object.getClass() );

        // Set the attribute value
        setAttribute( configurer, object, name, value, context );
    }

    /**
     * Sets an attribute value.
     */
    private void setAttribute( final ObjectConfigurer configurer,
                               final Object object,
                               final String name,
                               final String value,
                               final Context context )
        throws ConfigurationException
    {
        if( DEBUG )
        {
            final String message = REZ.getString( "configure-attribute.notice",
                                                  name,
                                                  value );
            getLogger().debug( message );
        }

        // Locate the setter for this attribute
        final AttributeSetter setter = configurer.getAttributeSetter( name );
        if( null == setter )
        {
            final String message = REZ.getString( "unknown-attribute.error", name );
            throw new ConfigurationException( message );
        }

        // Set the value
        try
        {
            setValue( setter, object, value, context );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "bad-set-attribute.error", name );
            throw new ConfigurationException( message, e );
        }
    }

    /**
     * Sets an attribute value, or an element's text content.
     */
    private void setValue( final AttributeSetter setter,
                           final Object object,
                           final String value,
                           final Context context )
        throws Exception
    {
        // Resolve property references in the attribute value
        Object objValue = PropertyUtil.resolveProperty( value, context, false );

        // Convert the value to the appropriate type
        Class clazz = setter.getType();
        if( clazz.isPrimitive() )
        {
            clazz = getComplexTypeFor( clazz );
        }

        objValue = m_converter.convert( clazz, objValue, context );

        // Set the value
        setter.setAttribute( object, objValue );
    }

    /**
     * Locates the configurer for a particular class.
     */
    private ObjectConfigurer getConfigurer( final Class clazz )
        throws ConfigurationException
    {
        ObjectConfigurer configurer =
            (ObjectConfigurer)m_configurerCache.get( clazz );
        if( null == configurer )
        {
            configurer = DefaultObjectConfigurer.getConfigurer( clazz );
            m_configurerCache.put( clazz, configurer );
        }
        return configurer;
    }

    private Class getComplexTypeFor( final Class clazz )
    {
        if( String.class == clazz )
        {
            return String.class;
        }
        else if( Integer.TYPE.equals( clazz ) )
        {
            return Integer.class;
        }
        else if( Long.TYPE.equals( clazz ) )
        {
            return Long.class;
        }
        else if( Short.TYPE.equals( clazz ) )
        {
            return Short.class;
        }
        else if( Byte.TYPE.equals( clazz ) )
        {
            return Byte.class;
        }
        else if( Boolean.TYPE.equals( clazz ) )
        {
            return Boolean.class;
        }
        else if( Float.TYPE.equals( clazz ) )
        {
            return Float.class;
        }
        else if( Double.TYPE.equals( clazz ) )
        {
            return Double.class;
        }
        else
        {
            final String message = REZ.getString( "no-complex-type.error", clazz.getName() );
            throw new IllegalArgumentException( message );
        }
    }
}
