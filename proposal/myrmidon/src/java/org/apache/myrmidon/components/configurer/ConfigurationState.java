/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

/**
 * A default configuration state implementation.  Keeps track of which
 * of the object's properties have been set.
 *
 * @author Adam Murdoch
 * @version $Revision$ $Date$
 */
class ConfigurationState
{
    private final int[] m_propertyCount;
    private final ObjectConfigurer m_configurer;
    private final Object m_object;

    public ConfigurationState( final ObjectConfigurer configurer,
                                      final Object object,
                                      final int propertyCount )
    {
        m_configurer = configurer;
        m_object = object;
        m_propertyCount = new int[ propertyCount ];
    }

    /**
     * Returns the configurer being used to configure the object.
     */
    public ObjectConfigurer getConfigurer()
    {
        return m_configurer;
    }

    /** Returns the object being configured. */
    public Object getObject()
    {
        return m_object;
    }

    /** Returns a property count. */
    public int getPropertyCount( final int index )
    {
        return m_propertyCount[ index ];
    }

    /** Increments a property count. */
    public void incPropertyCount( final int index )
    {
        m_propertyCount[ index ]++;
    }
}
