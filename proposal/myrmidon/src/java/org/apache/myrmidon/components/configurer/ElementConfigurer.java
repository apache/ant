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
 * Configures an element of an object.
 *
 * @author <a href="mailto:adammurdoch_ml@yahoo.com">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface ElementConfigurer
{
    /**
     * Returns the type of the element.
     */
    Class getType();

    /**
     * Creates an object for an element.
     *
     * @param parent The parent object.
     * @return An object which is assignable to the type returned by
     *         {@link #getType}.
     * @throws ConfigurationException If the object cannot be created.
     */
    Object createElement( Object parent )
        throws ConfigurationException;

    /**
     * Attaches an element object to its parent, after it has been configured.
     *
     * @param parent The parent object.
     *
     * @param child The element object.  This must be assignable to the type
     *              returned by {@link #getType}.
     * @throws ConfigurationException If the object cannot be attached.
     */
    void addElement( Object parent, Object child )
        throws ConfigurationException;
}
