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
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultPropertyConfigurer.class );

    private final int m_propertyIndex;
    private final Class m_type;
    private final Method m_method;
    private final int m_maxCount;

    public DefaultPropertyConfigurer( final int propIndex,
                                      final Class type,
                                      final Method method,
                                      final int maxCount )
    {
        m_propertyIndex = propIndex;
        if( type.isPrimitive() )
        {
            m_type = getComplexTypeFor( type );
        }
        else
        {
            m_type = type;
        }
        m_method = method;
        m_maxCount = maxCount;

        if( null == m_method )
        {
            throw new NullPointerException( "method" );
        }
    }

    /**
     * Returns the type of the element.
     */
    public Class getType()
    {
        return m_type;
    }

    /**
     * Adds a value for this property, to an object.
     */
    public void addValue( final ConfigurationState state, final Object value )
        throws ConfigurationException
    {
        final ConfigurationState defState = (ConfigurationState)state;
        // Check the property count
        if( defState.getPropertyCount( m_propertyIndex ) >= m_maxCount )
        {
            final String message = REZ.getString( "too-many-values.error" );
            throw new ConfigurationException( message );
        }
        defState.incPropertyCount( m_propertyIndex );

        try
        {
            // Add the value
            m_method.invoke( defState.getObject(), new Object[]{value} );
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
