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
import org.apache.aut.converter.Converter;
import org.apache.aut.converter.ConverterException;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.framework.DataType;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.role.RoleInfo;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * Class used to configure tasks.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant.type type="configurer" name="default"
 */
public class DefaultConfigurer
    extends AbstractLogEnabled
    implements Configurer, Serviceable, LogEnabled
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultConfigurer.class );

    ///Converter to use for converting between values
    private Converter m_converter;

    //TypeManager to use to create types in typed adders
    private TypeManager m_typeManager;

    //RoleManager to use to map from type names -> role shorthand
    private RoleManager m_roleManager;

    ///Cached object configurers.  This is a map from Class to the
    ///ObjectConfigurer for that class.
    private Map m_configurerCache = new HashMap();

    public void service( final ServiceManager serviceManager )
        throws ServiceException
    {
        m_converter = (Converter)serviceManager.lookup( Converter.ROLE );
        m_typeManager = (TypeManager)serviceManager.lookup( TypeManager.ROLE );
        m_roleManager = (RoleManager)serviceManager.lookup( RoleManager.ROLE );
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
    public void configureElement( final Object object,
                                  final Configuration configuration,
                                  final TaskContext context )
        throws ConfigurationException
    {
        configureElement( object, object.getClass(), configuration, context );
    }

    public void configureElement( final Object object,
                                  final Class clazz,
                                  final Configuration configuration,
                                  final TaskContext context )
        throws ConfigurationException
    {
        try
        {
            // Configure the object
            configureObject( object, clazz, configuration, context );
        }
        catch( final ReportableConfigurationException e )
        {
            // Already have a reasonable error message - so rethrow
            throw e.getCause();
        }
        catch( final Exception e )
        {
            // Wrap all other errors with general purpose error message
            final String message = REZ.getString( "bad-configure-element.error", configuration.getName() );
            throw new ConfigurationException( message, e );
        }
    }

    /**
     * Does the work of configuring an object.
     *
     * @throws ReportableConfigurationException On error.  This exception
     *         indicates that the error has been wrapped with an appropriate
     *         error message.
     * @throws Exception On error
     */
    private void configureObject( final Object object,
                                  final Class clazz,
                                  final Configuration configuration,
                                  final TaskContext context )
        throws Exception
    {
        if( object instanceof Configurable )
        {
            // Let the object configure itself
            ( (Configurable)object ).configure( configuration );
        }
        else
        {
            // Start configuration of the object
            final String elemName = configuration.getName();
            final ObjectConfigurer configurer = getConfigurer( clazz );
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
                    throw new ReportableConfigurationException( message );
                }
                catch( final Exception ce )
                {
                    final String message =
                        REZ.getString( "bad-set-attribute.error", elemName, name );
                    throw new ReportableConfigurationException( message, ce );
                }
            }

            // Set the text content
            final String content = configuration.getValue( null );
            if( null != content && content.length() > 0 )
            {
                try
                {
                    // Set the content
                    setContent( state, content, context );
                }
                catch( final NoSuchPropertyException nspe )
                {
                    final String message =
                        REZ.getString( "no-content.error", elemName );
                    throw new ReportableConfigurationException( message );
                }
                catch( final Exception ce )
                {
                    final String message =
                        REZ.getString( "bad-set-content.error", elemName );
                    throw new ReportableConfigurationException( message, ce );
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
                    throw new ReportableConfigurationException( message );
                }
                catch( final ReportableConfigurationException ce )
                {
                    throw ce;
                }
                catch( final Exception ce )
                {
                    final String message =
                        REZ.getString( "bad-configure-element.error", name );
                    throw new ReportableConfigurationException( message, ce );
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
    public void configureAttribute( final Object object,
                                    final String name,
                                    final String value,
                                    final TaskContext context )
        throws ConfigurationException
    {
        configureAttribute( object, object.getClass(), name, value, context );
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
    public void configureAttribute( final Object object,
                                    final Class clazz,
                                    final String name,
                                    final String value,
                                    final TaskContext context )
        throws ConfigurationException
    {
        // Locate the configurer for this object
        final ObjectConfigurer configurer = getConfigurer( clazz );

        // TODO - this ain't right, the validation is going to be screwed up
        final ConfigurationState state = configurer.startConfiguration( object );

        // Set the attribute value
        try
        {
            setAttribute( state, name, value, context );
        }
        catch( final Exception ce )
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
     * Sets the text content for the element.
     */
    private void setContent( final ConfigurationState state,
                             final String content,
                             final TaskContext context )
        throws Exception
    {
        // Locate the content configurer
        final PropertyConfigurer contentConfigurer = state.getConfigurer().getContentConfigurer();
        if( contentConfigurer == null )
        {
            throw new NoSuchPropertyException();
        }

        // Set the content
        setValue( contentConfigurer, state, content, context );
    }

    /**
     * Configures a property from a nested element.
     */
    private void configureElement( final ConfigurationState state,
                                   final Configuration element,
                                   final TaskContext context )
        throws Exception
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
                                  final TaskContext context )
        throws Exception
    {
        final String name = element.getName();

        // Locate the configurer for the child element
        final PropertyConfigurer childConfigurer =
            getConfigurerFromName( state.getConfigurer(), name, true, true );

        // Create & configure the child element
        final Object child =
            setupChild( state, element, context, childConfigurer );

        // Set the child element
        childConfigurer.addValue( state, child );
    }

    /**
     * Configures a property from a reference.
     */
    private void configureReference( final ConfigurationState state,
                                     final Configuration element,
                                     final TaskContext context )
        throws Exception
    {
        // Extract the id
        final String id = element.getAttribute( "id" );
        if( 1 != element.getAttributeNames().length ||
            0 != element.getChildren().length )
        {
            final String message = REZ.getString( "extra-config-for-ref.error" );
            throw new ConfigurationException( message );
        }

        // Set the property
        final String name = element.getName();
        setReference( state, name, id, context, true );
    }

    /**
     * Sets a property using a reference.
     */
    private void setReference( final ConfigurationState state,
                               final String refName,
                               final String unresolvedId,
                               final TaskContext context,
                               final boolean isAdder )
        throws Exception
    {
        // Adjust the name
        final String name = refName.substring( 0, refName.length() - 4 );

        // Locate the configurer for the property
        final PropertyConfigurer configurer =
            getConfigurerFromName( state.getConfigurer(), name, false, isAdder );

        // Resolve any props in the id
        String id = context.resolveValue( unresolvedId ).toString();

        // Locate the referenced object
        Object ref = context.getProperty( id );
        if( null == ref )
        {
            final String message = REZ.getString( "unknown-reference.error", id );
            throw new ConfigurationException( message );
        }

        // Convert the object, if necessary
        final Class type = configurer.getType();
        if( !type.isInstance( ref ) )
        {
            try
            {
                ref = m_converter.convert( type, ref, context );
            }
            catch( ConverterException e )
            {
                final String message = REZ.getString( "mismatch-ref-types.error", id, name );
                throw new ConfigurationException( message, e );
            }
        }

        // Set the child element
        configurer.addValue( state, ref );
    }

    /**
     * Sets an attribute value.
     */
    private void setAttribute( final ConfigurationState state,
                               final String name,
                               final String value,
                               final TaskContext context )
        throws Exception
    {
        if( name.toLowerCase().endsWith( "-ref" ) )
        {
            // A reference
            setReference( state, name, value, context, false );
        }
        else
        {
            // Set the value
            PropertyConfigurer propConfigurer = getConfigurerFromName( state.getConfigurer(), name, false, false );
            setValue( propConfigurer, state, value, context );
        }
    }

    /**
     * Sets an attribute value, or an element's text content.
     */
    private void setValue( final PropertyConfigurer setter,
                           final ConfigurationState state,
                           final String value,
                           final TaskContext context )
        throws Exception
    {
        // Resolve property references in the attribute value
        Object objValue = context.resolveValue( value );

        // Convert the value to the appropriate type
        final Class type = setter.getType();
        if( !type.isInstance( objValue ) )
        {
            objValue = m_converter.convert( type, objValue, context );
        }

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

    /**
     * Creates and configures an inline object.
     */
    private Object setupChild( final ConfigurationState state,
                               final Configuration element,
                               final TaskContext context,
                               final PropertyConfigurer childConfigurer )
        throws Exception
    {
        final String name = element.getName();
        final Class type = childConfigurer.getType();

        if( Configuration.class == type )
        {
            //special case where you have add...(Configuration)
            return element;
        }

        // Create an instance
        Object child = null;
        if( childConfigurer == state.getConfigurer().getTypedProperty() )
        {
            // Typed property
            child = createTypedObject( name, type );
        }
        else
        {
            // Named property
            child = createNamedObject( type );
        }

        // Configure the object
        final Object object = child;
        configureObject( object, object.getClass(), element, context );

        // Convert the object, if necessary
        if( !type.isInstance( child ) )
        {
            child = m_converter.convert( type, child, context );
        }

        return child;
    }

    /**
     * Determines the property configurer to use for a particular element
     * or attribute.  If the supplied name matches a property of the
     * class being configured, that property configurer is returned.  If
     * the supplied name matches the role shorthand for the class' typed
     * property, then the typed property configurer is used.
     *
     * @param configurer The configurer for the class being configured.
     * @param name The attribute/element name.
     */
    private PropertyConfigurer getConfigurerFromName( final ObjectConfigurer configurer,
                                                      final String name,
                                                      boolean ignoreRoleName,
                                                      final boolean isAdder )
        throws Exception
    {
        // Try a named property
        if( !isAdder )
        {
            PropertyConfigurer propertyConfigurer = configurer.getSetter( name );
            if( propertyConfigurer != null )
            {
                return propertyConfigurer;
            }
        }
        else
        {
            PropertyConfigurer propertyConfigurer = configurer.getAdder( name );
            if( propertyConfigurer != null )
            {
                return propertyConfigurer;
            }

            // Try a typed property
            propertyConfigurer = configurer.getTypedProperty();
            if( propertyConfigurer != null )
            {
                if( ignoreRoleName )
                {
                    return propertyConfigurer;
                }
                else
                {
                    // Check the role name
                    final RoleInfo roleInfo = m_roleManager.getRoleByType( propertyConfigurer.getType() );
                    if( roleInfo != null && name.equalsIgnoreCase( roleInfo.getShorthand() ) )
                    {
                        return propertyConfigurer;
                    }
                }
            }
        }
        // Unknown prop
        throw new NoSuchPropertyException();
    }

    /**
     * Creates an instance for a named property.
     */
    private Object createNamedObject( final Class type )
        throws Exception
    {
        // Map the expected type to a role.  If found, instantiate the default
        // type for that role
        final RoleInfo roleInfo = m_roleManager.getRoleByType( type );
        if( roleInfo != null )
        {
            final String typeName = roleInfo.getDefaultType();
            if( typeName != null )
            {
                // Create the instance
                final TypeFactory factory = m_typeManager.getFactory( roleInfo.getName() );
                return factory.create( typeName );
            }
        }

        if( type.isInterface() )
        {
            // An interface - don't know how to instantiate it
            final String message = REZ.getString( "instantiate-interface.error", type.getName() );
            throw new ConfigurationException( message );
        }

        // Use the no-args constructor
        return createObject( type );
    }

    /**
     * Creates an instance of the typed property.
     */
    private Object createTypedObject( final String name,
                                      final Class type )
        throws Exception
    {
        // Map the expected type to a role.  If found, attempt to create
        // an instance
        final RoleInfo roleInfo = m_roleManager.getRoleByType( type );
        if( roleInfo != null )
        {
            final TypeFactory factory = m_typeManager.getFactory( roleInfo.getName() );
            if( factory.canCreate( name ) )
            {
                return factory.create( name );
            }
        }

        // Use the generic 'data-type' role.
        final TypeFactory factory = m_typeManager.getFactory( DataType.ROLE );
        if( !factory.canCreate( name ) )
        {
            throw new NoSuchPropertyException();
        }
        return factory.create( name );
    }

    /**
     * Utility method to instantiate an instance of the specified class.
     */
    private Object createObject( final Class type )
        throws Exception
    {
        try
        {
            return type.newInstance();
        }
        catch( final Exception e )
        {
            final String message =
                REZ.getString( "create-object.error",
                               type.getName() );
            throw new ConfigurationException( message, e );
        }
    }
}
