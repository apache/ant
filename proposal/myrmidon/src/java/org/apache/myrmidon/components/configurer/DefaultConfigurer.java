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
import org.apache.avalon.framework.context.ContextException;
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
        final PropertyConfigurer contentConfigurer = configurer.getContentConfigurer();
        if( null == contentConfigurer )
        {
            final String message = REZ.getString( "content-not-supported.error" );
            throw new ConfigurationException( message );
        }

        try
        {
            setValue( contentConfigurer, object, content, context );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "bad-set-content.error" );
            throw new ConfigurationException( message, e );
        }
    }

    /**
     * Configures a property from a nested element.
     */
    private void configureElement( final ObjectConfigurer configurer,
                                   final Object object,
                                   final Configuration element,
                                   final Context context )
        throws ConfigurationException
    {
        final String elementName = element.getName();

        if( DEBUG )
        {
            final String message =
                REZ.getString( "configure-subelement.notice", elementName );
            getLogger().debug( message );
        }

        if( elementName.endsWith( "-ref" ) )
        {
            // A reference
            configureReference( configurer, object, element, context );
        }
        else
        {
            // An inline object
            configureInline( configurer, object, element, context );
        }
    }

    /**
     * Configure a property from an inline object.
     */
    private void configureInline( final ObjectConfigurer configurer,
                                  final Object object,
                                  final Configuration element,
                                  final Context context )
        throws ConfigurationException
    {
        final String elementName = element.getName();

        // Locate the configurer for the child element
        final PropertyConfigurer childConfigurer = configurer.getProperty( elementName );
        if( null == childConfigurer )
        {
            final String message = REZ.getString( "unknown-property.error", elementName );
            throw new ConfigurationException( message );
        }

        try
        {
            // Create the child element
            final Object child = childConfigurer.createValue( object );

            // Configure the child element
            configure( child, element, context );

            // Set the child element
            childConfigurer.setValue( object, child );
        }
        catch( final ConfigurationException ce )
        {
            final String message =
                REZ.getString( "bad-set-property.error", elementName );
            throw new ConfigurationException( message, ce );
        }
    }

    /**
     * Configures a property from a reference.
     */
    private void configureReference( final ObjectConfigurer configurer,
                                     final Object object,
                                     final Configuration element,
                                     final Context context )
        throws ConfigurationException
    {
        // Adjust the name
        final String elementName = element.getName();
        final String name = elementName.substring( 0, elementName.length() - 4 );

        // Extract the id
        final String id = element.getAttribute( "id" );
        if( 1 != element.getAttributeNames().length ||
            0 != element.getChildren().length )
        {
            final String message = REZ.getString( "extra-config-for-ref.error" );
            throw new ConfigurationException( message );
        }

        // Set the property
        setReference( configurer, object, name, id, context );
    }

    /**
     * Sets a property using a reference.
     */
    private void setReference( final ObjectConfigurer configurer,
                               final Object object,
                               final String name,
                               final String id,
                               final Context context )
        throws ConfigurationException
    {
        // Locate the configurer for the child element
        final PropertyConfigurer childConfigurer = configurer.getProperty( name );
        if( null == childConfigurer )
        {
            final String message = REZ.getString( "unknown-property.error", name );
            throw new ConfigurationException( message );
        }

        // Check if the creator method must be used
        if( childConfigurer.useCreator() )
        {
            final String message = REZ.getString( "must-be-element.error" );
            throw new ConfigurationException( message );
        }

        // Locate the referenced object
        Object ref = null;
        try
        {
            ref = context.get( id );
        }
        catch( final ContextException ce )
        {
            final String message = REZ.getString( "get-ref.error", id, name );
            throw new ConfigurationException( message, ce );
        }

        // Check the types
        final Class type = childConfigurer.getType();
        if( !type.isInstance( ref ) )
        {
            final String message = REZ.getString( "mismatch-ref-types.error", id, name );
            throw new ConfigurationException( message );
        }

        // Set the child element
        try
        {
            childConfigurer.setValue( object, ref );
        }
        catch( final ConfigurationException ce )
        {
            final String message =
                REZ.getString( "bad-set-property.error", name );
            throw new ConfigurationException( message, ce );
        }
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

        if( name.endsWith( "-ref" ) )
        {
            // A reference
            final String refName = name.substring( 0, name.length() - 4 );
            setReference( configurer, object, refName, value, context );
        }
        else
        {
            // Locate the configurer for this attribute
            final PropertyConfigurer propConfigurer = configurer.getProperty( name );
            if( null == propConfigurer )
            {
                final String message = REZ.getString( "unknown-property.error", name );
                throw new ConfigurationException( message );
            }

            // Set the value
            try
            {
                setValue( propConfigurer, object, value, context );
            }
            catch( final Exception e )
            {
                final String message = REZ.getString( "bad-set-property.error", name );
                throw new ConfigurationException( message, e );
            }
        }
    }

    /**
     * Sets an attribute value, or an element's text content.
     */
    private void setValue( final PropertyConfigurer setter,
                           final Object object,
                           final String value,
                           final Context context )
        throws Exception
    {
        // Check if the creator method must be used
        if( setter.useCreator() )
        {
            final String message = REZ.getString( "must-be-element.error" );
            throw new ConfigurationException( message );
        }

        // Resolve property references in the attribute value
        Object objValue = PropertyUtil.resolveProperty( value, context, false );

        // Convert the value to the appropriate type
        final Class clazz = setter.getType();
        objValue = m_converter.convert( clazz, objValue, context );

        // Set the value
        setter.setValue( object, objValue );
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
}
