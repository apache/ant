/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * Configures a property of an object.
 * TODO - axe useCreator() and createValue().
 *
 * @author <a href="mailto:adammurdoch_ml@yahoo.com">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
interface PropertyConfigurer
{
    /**
     * Returns the type of this property.
     */
    Class getType();

    /**
     * Creates a default value for this property.  This value must be configured,
     * and then attached to the object using {@link #setValue}.  This
     * method must be called if {@link #useCreator} returns true.
     *
     * @param state The state object, representing the object being configured.
     * @return An object which is assignable to the type returned by
     *         {@link #getType}.  Returns null if this property does not
     *         need a default value.
     * @throws ConfigurationException If the object cannot be created.
     */
    Object createValue( ConfigurationState state )
        throws ConfigurationException;

    /**
     * Adds a value for this property, to an object.
     *
     * @param state The state object, representing the object being configured.
     * @param value The property value.  This must be assignable to the type
     *              returned by {@link #getType}.
     * @throws ConfigurationException If the property cannot be set.
     */
    void addValue( ConfigurationState state, Object value )
        throws ConfigurationException;
}
