/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * The default property configurer implementation, which uses reflection to
 * create and set property values.
 *
 * @author <a href="mailto:adammurdoch_ml@yahoo.com">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class DefaultPropertyConfigurer
    implements PropertyConfigurer
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultPropertyConfigurer.class );

    private final int m_propIndex;
    private final Class m_type;
    private final Method m_createMethod;
    private final Method m_addMethod;
    private final int m_maxCount;

    public DefaultPropertyConfigurer( final int propIndex,
                                      final Class type,
                                      final Method createMethod,
                                      final Method addMethod,
                                      final int maxCount )
    {
        m_propIndex = propIndex;
        if( type.isPrimitive() )
        {
            m_type = getComplexTypeFor( type );
        }
        else
        {
            m_type = type;
        }
        m_createMethod = createMethod;
        m_addMethod = addMethod;
        m_maxCount = maxCount;
    }

    /**
     * Returns the type of the element.
     */
    public Class getType()
    {
        return m_type;
    }

    /**
     * Creates a default value for this property.
     */
    public Object createValue( final ConfigurationState state )
        throws ConfigurationException
    {
        if( null == m_createMethod )
        {
            return null;
        }

        final DefaultConfigurationState defState = (DefaultConfigurationState)state;

        // Make sure there isn't a pending object for this property
        if( defState.getCreatedObject( m_propIndex ) != null )
        {
            final String message = REZ.getString( "pending-property-value.error" );
            throw new ConfigurationException( message );
        }

        try
        {
            // Create the value
            final Object object = m_createMethod.invoke( defState.getObject(), null );
            defState.setCreatedObject( m_propIndex, object );
            return object;
        }
        catch( final InvocationTargetException ite )
        {
            final Throwable cause = ite.getTargetException();
            throw new ConfigurationException( cause.getMessage(), cause );
        }
        catch( final Exception e )
        {
            throw new ConfigurationException( e.getMessage(), e );
        }
    }

    /**
     * Adds a value for this property, to an object.
     */
    public void addValue( final ConfigurationState state, final Object value )
        throws ConfigurationException
    {
        final DefaultConfigurationState defState = (DefaultConfigurationState)state;

        // Make sure the supplied object is the pending object
        final Object pending = defState.getCreatedObject( m_propIndex );
        if( pending != null && pending != value )
        {
        }

        // Make sure the creator method was called, if necessary
        if( pending == null && m_createMethod != null )
        {
            final String message = REZ.getString( "must-be-element.error" );
            throw new ConfigurationException( message );
        }

        defState.setCreatedObject( m_propIndex, null );

        // Check the property count
        if( defState.getPropCount( m_propIndex ) >= m_maxCount )
        {
            final String message = REZ.getString( "too-many-values.error" );
            throw new ConfigurationException( message );
        }
        defState.incPropCount( m_propIndex );

        try
        {
            // Add the value
            if( null != m_addMethod )
            {
                m_addMethod.invoke( defState.getObject(), new Object[]{value} );
            }
        }
        catch( final InvocationTargetException ite )
        {
            final Throwable cause = ite.getTargetException();
            throw new ConfigurationException( cause.getMessage(), cause );
        }
        catch( final Exception e )
        {
            throw new ConfigurationException( e.getMessage(), e );
        }
    }

    /**
     * Determines the complex type for a prmitive type.
     */
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
