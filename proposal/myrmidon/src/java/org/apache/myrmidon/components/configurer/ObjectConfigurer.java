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
 * Configures objects of a particular class.
 *
 * @author <a href="mailto:adammurdoch_ml@yahoo.com">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
interface ObjectConfigurer
{
    /**
     * Starts the configuration of an object.
     *
     * @param object The object about to be configured.
     * @return  The state object, used to track type-specific state during
     *          configuration.
     * @throws  ConfigurationException On error starting the configuration.
     */
    ConfigurationState startConfiguration( Object object )
        throws ConfigurationException;

    /**
     * Finishes the configuration of an object, performing any final
     * validation and type conversion.
     *
     * @param state The state object.
     * @return The configured object.
     * @throws ConfigurationException On error finishing the configurtion.
     */
    Object finishConfiguration( ConfigurationState state )
        throws ConfigurationException;

    /**
     * Returns a configurer for a property of this class.
     *
     * @param name The element name.  Property names are case-insensitive.
     * @return A configurer for the property.
     * @throws NoSuchPropertyException If the property is not valid for this
     *         class
     */
    PropertyConfigurer getProperty( String name )
        throws NoSuchPropertyException;

    /**
     * Returns a configurer for the content of this class.
     *
     * @return A configurer for the content.
     * @throws NoSuchPropertyException If the class does not handle content.
     */
    PropertyConfigurer getContentConfigurer()
        throws NoSuchPropertyException;

    /**
     * Returns a configurer for the typed property of this class.
     *
     * @return A configurer for the typed property.
     * @throws NoSuchPropertyException If the class does not have a typed
     *         property.
     */
    PropertyConfigurer getTypedProperty()
        throws NoSuchPropertyException;
}
