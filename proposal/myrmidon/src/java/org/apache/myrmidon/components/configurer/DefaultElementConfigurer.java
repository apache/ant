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
 * The default element configurer implementation, which uses reflection to
 * create and/or add nested elements.
 *
 * @author <a href="mailto:adammurdoch_ml@yahoo.com">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class DefaultElementConfigurer
    implements ElementConfigurer
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultElementConfigurer.class );

    private final Class m_type;
    private final Method m_createMethod;
    private final Method m_addMethod;

    public DefaultElementConfigurer( final Class type,
                                     final Method createMethod,
                                     final Method addMethod )
    {
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
     * Creates a nested element.
     */
    public Object createElement( final Object parent )
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
    public void addElement( final Object parent, final Object child )
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
}
