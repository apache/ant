/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.DataType;

/**
 * This is the property "task" to declare a binding of a datatype to a name.
 *
 * TODO: Determine final format of property task.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant:task name="property"
 */
public class Property
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( Property.class );

    private String m_name;
    private Object m_value;

    public void setName( final String name )
    {
        m_name = name;
    }

    /**
     * Sets the property value from a nested element.
     */
    public void set( final DataType value )
        throws TaskException
    {
        setValue( value );
    }

    /**
     * Sets the property value from text content.
     */
    public void addContent( final String value )
        throws TaskException
    {
        setValue( value );
    }

    /**
     * Sets the property value from an attribute.
     */
    public void setValue( final Object value )
        throws TaskException
    {
        if( null != m_value )
        {
            final String message = REZ.getString( "property.multi-set.error" );
            throw new TaskException( message );
        }

        m_value = value;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_name )
        {
            final String message = REZ.getString( "property.no-name.error" );
            throw new TaskException( message );
        }

        if( null == m_value )
        {
            final String message = REZ.getString( "property.no-value.error" );
            throw new TaskException( message );
        }

        getContext().setProperty( m_name, m_value );
    }
}
