/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.property.PropertyUtil;
import org.apache.avalon.framework.CascadingException;
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
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultConfigurer.class );

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
        try
        {
            configureObject( object, configuration, context );
        }
        catch( InvocationTargetException ite )
        {
            // A configuration exception thrown from a nested object.  Unpack
            // and re-throw
            throw (ConfigurationException)ite.getTargetException();
        }
    }

    /**
     * Does the work of configuring an object.
     */
    private void configureObject( final Object object,
                                  final Configuration configuration,
                                  final Context context )
        throws ConfigurationException, InvocationTargetException
    {
        if( object instanceof Configurable )
        {
            // Let the object configure itself
            ( (Configurable)object ).configure( configuration );
        }
        else
        {
            final String elemName = configuration.getName();

            // Locate the configurer for this object
            final ObjectConfigurer configurer = getConfigurer( object.getClass() );

            // Start configuring this object
            final ConfigurationState state = configurer.startConfiguration( object );

            // Set each of the attributes
            final String[] attributes = configuration.getAttributeNames();
            for( int i = 0; i < attributes.length; i++ )
            {
                final String name = attributes[ i ];
                try
                {
                    // Set the attribute
                    final String value = configuration.getAttribute( name );
                    setAttribute( state, name, value, context );
                }
                catch( final NoSuchPropertyException nspe )
                {
                    final String message =
                        REZ.getString( "no-such-attribute.error", elemName, name );
                    throw new ConfigurationException( message, nspe );
                }
                catch( final CascadingException ce )
                {
                    final String message =
                        REZ.getString( "bad-set-attribute.error", elemName, name );
                    throw new ConfigurationException( message, ce );
                }
            }

            // Set the text content
            final String content = configuration.getValue( null );
            if( null != content && content.length() > 0 )
            {
                try
                {
                    // Set the content
                    final PropertyConfigurer contentConfigurer = state.getConfigurer().getContentConfigurer();
                    setValue( contentConfigurer, state, content, context );
                }
                catch( final NoSuchPropertyException nspe )
                {
                    final String message =
                        REZ.getString( "no-content.error", elemName );
                    throw new ConfigurationException( message, nspe );
                }
                catch( final CascadingException ce )
                {
                    final String message =
                        REZ.getString( "bad-set-content.error", elemName );
                    throw new ConfigurationException( message, ce );
                }
            }

            // Create and configure each of the child elements
            final Configuration[] children = configuration.getChildren();
            for( int i = 0; i < children.length; i++ )
            {
                final Configuration childConfig = children[ i ];
                final String name = childConfig.getName();
                try
                {
                    configureElement( state, childConfig, context );
                }
                catch( final NoSuchPropertyException nspe )
                {
                    final String message =
                        REZ.getString( "no-such-element.error", elemName, name );
                    throw new ConfigurationException( message, nspe );
                }
                catch( final CascadingException ce )
                {
                    final String message =
                        REZ.getString( "bad-set-element.error", name );
                    throw new ConfigurationException( message, ce );
                }
            }

            // Finish configuring the object
            configurer.finishConfiguration( state );
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

        // TODO - this ain't right, the validation is going to be screwed up
        final ConfigurationState state = configurer.startConfiguration( object );

        // Set the attribute value
        try
        {
            setAttribute( state, name, value, context );
        }
        catch( final CascadingException ce )
        {
            final String message =
                REZ.getString( "bad-set-class-attribute.error",
                               name,
                               object.getClass().getName() );
            throw new ConfigurationException( message, ce );
        }

        // Finish up
        configurer.finishConfiguration( state );
    }

    /**
     * Configures a property from a nested element.
     */
    private void configureElement( final ConfigurationState state,
                                   final Configuration element,
                                   final Context context )
        throws CascadingException, InvocationTargetException
    {
        final String elementName = element.getName();
        if( elementName.toLowerCase().endsWith( "-ref" ) )
        {
            // A reference
            configureReference( state, element, context );
        }
        else
        {
            // An inline object
            configureInline( state, element, context );
        }
    }

    /**
     * Configure a property from an inline object.
     */
    private void configureInline( final ConfigurationState state,
                                  final Configuration element,
                                  final Context context )
        throws CascadingException, InvocationTargetException
    {
        final String elementName = element.getName();

        // Locate the configurer for the child element
        final PropertyConfigurer childConfigurer = state.getConfigurer().getProperty( elementName );

        // Create the child element
        Object child = childConfigurer.createValue( state );
        if( child == null )
        {
            // Create an instance using the default constructor
            try
            {
                child = childConfigurer.getType().newInstance();
            }
            catch( final Exception e )
            {
                final String message =
                    REZ.getString( "create-object.error",
                                   childConfigurer.getType().getName() );
                throw new ConfigurationException( message, e );
            }
        }

        // Configure the child element
        try
        {
            configureObject( child, element, context );
        }
        catch( final ConfigurationException ce )
        {
            // Nasty hack-o-rama, used to get this exception up through
            // the stack of doConfigure() calls.  This is unpacked by the
            // top-most configure() call, and rethrown.
            throw new InvocationTargetException( ce );
        }

        // Set the child element
        childConfigurer.addValue( state, child );
    }

    /**
     * Configures a property from a reference.
     */
    private void configureReference( final ConfigurationState state,
                                     final Configuration element,
                                     final Context context )
        throws CascadingException
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
        setReference( state, name, id, context );
    }

    /**
     * Sets a property using a reference.
     */
    private void setReference( final ConfigurationState state,
                               final String name,
                               final String unresolvedId,
                               final Context context )
        throws CascadingException
    {
        // Locate the configurer for the child element
        final PropertyConfigurer childConfigurer = state.getConfigurer().getProperty( name );

        // Resolve any props in the id
        Object id = PropertyUtil.resolveProperty( unresolvedId, context, false );

        // Locate the referenced object
        Object ref = null;
        try
        {
            ref = context.get( id );
        }
        catch( final ContextException exc )
        {
            final String message = REZ.getString( "get-ref.error", id );
            throw new ConfigurationException( message, exc );
        }

        // Check the types
        final Class type = childConfigurer.getType();
        if( !type.isInstance( ref ) )
        {
            final String message = REZ.getString( "mismatch-ref-types.error", id, type.getName(), ref.getClass().getName() );
            throw new ConfigurationException( message );
        }

        // Set the child element
        childConfigurer.addValue( state, ref );
    }

    /**
     * Sets an attribute value.
     */
    private void setAttribute( final ConfigurationState state,
                               final String name,
                               final String value,
                               final Context context )
        throws CascadingException
    {
        if( name.toLowerCase().endsWith( "-ref" ) )
        {
            // A reference
            final String refName = name.substring( 0, name.length() - 4 );
            setReference( state, refName, value, context );
        }
        else
        {
            // Set the value
            final PropertyConfigurer propConfigurer =
                state.getConfigurer().getProperty( name );
            setValue( propConfigurer, state, value, context );
        }
    }

    /**
     * Sets an attribute value, or an element's text content.
     */
    private void setValue( final PropertyConfigurer setter,
                           final ConfigurationState state,
                           final String value,
                           final Context context )
        throws CascadingException
    {
        // Resolve property references in the attribute value
        Object objValue = PropertyUtil.resolveProperty( value, context, false );

        // Convert the value to the appropriate type
        final Class clazz = setter.getType();
        objValue = m_converter.convert( clazz, objValue, context );

        // Set the value
        setter.addValue( state, objValue );
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
