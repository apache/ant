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
 * Used to set an attribute or text content of an object.
 *
 * @author <a href="mailto:adammurdoch_ml@yahoo.com">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface AttributeSetter
{
    /**
     * Returns the attribute type.
     */
    Class getType();

    /**
     * Sets the value of the attribute.
     *
     * @param object The object to set the attribute of.
     * @param value The value of the attribute.  Must be assignable to the class
     *              returned by {@link #getType}.
     * @throw ConfigurationException If the value could not be set.
     */
    void setAttribute( Object object, Object value )
        throws ConfigurationException;
}
