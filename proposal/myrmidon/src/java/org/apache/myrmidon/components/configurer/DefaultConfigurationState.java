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
 * of the object's properties have been set.  Also keeps track of the
 * objects created by the creator methods, but not yet set by the adder
 * methods.
 *
 * @author Adam Murdoch
 */
public class DefaultConfigurationState
    implements ConfigurationState
{
    final private int[] m_propCount;
    final private Object[] m_createdObjects;
    final private ObjectConfigurer m_configurer;
    final private Object m_object;

    public DefaultConfigurationState( final ObjectConfigurer configurer,
                                      final Object object,
                                      final int numProps )
    {
        m_configurer = configurer;
        m_object = object;
        m_propCount = new int[ numProps ];
        m_createdObjects = new Object[ numProps ];
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
    public int getPropCount( final int propIndex )
    {
        return m_propCount[ propIndex ];
    }

    /** Increments a property count. */
    public void incPropCount( final int propIndex )
    {
        m_propCount[ propIndex ]++;
    }

    /** Returns a property's pending objects. */
    public Object getCreatedObject( final int propIndex )
    {
        return m_createdObjects[ propIndex ];
    }

    /** Sets a property's pending objects. */
    public void setCreatedObject( final int propIndex, final Object object )
    {
        m_createdObjects[ propIndex ] = object;
    }
}
