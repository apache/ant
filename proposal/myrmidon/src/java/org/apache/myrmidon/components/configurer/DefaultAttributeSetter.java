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
 * A default attribute setter implementation, which uses reflection to
 * set the attribute value.
 *
 * @author <a href="mailto:adammurdoch_ml@yahoo.com">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class DefaultAttributeSetter
    implements AttributeSetter
{
    private final Method m_method;
    private final Class m_type;

    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultAttributeSetter.class );

    public DefaultAttributeSetter( final Method method )
    {
        m_method = method;
        m_type = method.getParameterTypes()[ 0 ];
    }

    /**
     * Returns the attribute type.
     */
    public Class getType()
    {
        return m_type;
    }

    /**
     * Sets the value of the attribute.
     */
    public void setAttribute( final Object object, final Object value )
        throws ConfigurationException
    {
        try
        {
            m_method.invoke( object, new Object[]{value} );
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
