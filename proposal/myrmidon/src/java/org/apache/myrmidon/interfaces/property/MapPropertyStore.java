/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.property;

import java.util.Map;
import java.util.HashMap;
import org.apache.myrmidon.api.TaskException;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * A simple unscoped, unsynchronized property store which is backed by a Map.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class MapPropertyStore
    implements PropertyStore
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( MapPropertyStore.class );

    private final Map m_properties = new HashMap();

    /**
     * Creates an empty store.
     */
    public MapPropertyStore()
    {
    }

    /**
     * Creates a store containing the given properties.
     */
    public MapPropertyStore( final Map properties )
    {
        m_properties.putAll( properties );
    }

    /**
     * Return <code>true</code> if the specified property is set.
     *
     * @param name the name of property
     */
    public boolean isPropertySet( final String name )
    {
        return m_properties.containsKey( name );
    }

    /**
     * Retrieve the value of specified property.
     *
     * @param name the name of the property
     * @return the value of the property.  Never returns null.
     * @throws TaskException if there is no such property, or on error
     *         retrieving property, such as an invalid property name.
     */
    public Object getProperty( final String name )
        throws TaskException
    {
        final Object value = m_properties.get( name );
        if( value == null )
        {
            final String message = REZ.getString( "unknown-property.error", name );
            throw new TaskException( message );
        }
        return value;
    }

    /**
     * Retrieve a copy of all the properties that are "in-scope"
     * for store.
     *
     * @return a copy of all the properties that are "in-scope"
     *         for store.
     * @throws TaskException if theres an error retrieving propertys
     */
    public Map getProperties()
        throws TaskException
    {
        return new HashMap( m_properties );
    }

    /**
     * Set the property with specified name to specified value.
     * The specific implementation will apply various rules
     * before setting the property.
     *
     * @param name the name of property
     * @param value the value of property
     * @throws TaskException if property can not be set
     */
    public void setProperty( String name, Object value )
        throws TaskException
    {
        m_properties.put( name, value );
    }

    /**
     * Return a child PropertyStore with specified name.
     * This is to allow support for scoped stores. However a
     * store may choose to be unscoped and just return a
     * reference to itself.
     *
     * @param name the name of child store
     * @return the child store
     * @throws TaskException if theres an error creating child store
     */
    public PropertyStore createChildStore( String name )
        throws TaskException
    {
        return this;
    }
}
