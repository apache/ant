/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskException;

/**
 * Simple holder for parameters.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @todo Refactor this and all the other parameter, sysproperty,
 *   property etc into a single class in framework
 */
public class AntParam
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( AntParam.class );

    private String m_name;
    private Object m_value;

    /**
     * Set the name of the parameter.
     *
     * @param name the name of parameter
     */
    public void setName( final String name )
    {
        m_name = name;
    }

    /**
     * Set the value of the parameter.
     *
     * @param value the parameter value
     */
    public void setValue( final Object value )
    {
        m_value = value;
    }

    /**
     * Retrieve name of parameter.
     *
     * @return the name of parameter.
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Retrieve the value of parameter.
     *
     * @return the value of parameter.
     */
    public Object getValue()
    {
        return m_value;
    }

    /**
     * Make sure that neither the name or the value
     * is null.
     */
    public void validate()
        throws TaskException
    {
        if( null == m_name )
        {
            final String message = REZ.getString( "param.noname.error" );
            throw new TaskException( message );
        }
        else if( null == m_value )
        {
            final String message =
                REZ.getString( "param.novalue.error", m_name );
            throw new TaskException( message );
        }
    }
}
