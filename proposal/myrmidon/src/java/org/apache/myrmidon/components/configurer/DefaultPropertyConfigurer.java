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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    private final Class m_type;
    private final Method m_createMethod;
    private final Method m_addMethod;

    public DefaultPropertyConfigurer( Class type,
                                      Method createMethod,
                                      Method addMethod )
    {
        if ( type.isPrimitive() )
        {
            type = getComplexTypeFor(type);
        }
        m_type = type;
        m_createMethod = createMethod;
        m_addMethod = addMethod;
    }

    /**
     * Returns the type of the element.
     */
    public Class getType()
    {
        return m_type;
    }

    /**
     * Determines if the property value must be created via {@link #createValue}.
     */
    public boolean useCreator()
    {
        return (m_createMethod != null);
    }

    /**
     * Creates a nested element.
     */
    public Object createValue( final Object parent )
        throws ConfigurationException
    {
        try
        {
            if( null != m_createMethod )
            {
                return m_createMethod.invoke( parent, null );
            }
            else
            {
                return m_type.newInstance();
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
     * Sets the nested element, after it has been configured.
     */
    public void setValue( final Object parent, final Object child )
        throws ConfigurationException
    {
        try
        {
            if( null != m_addMethod )
            {
                m_addMethod.invoke( parent, new Object[]{child} );
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
