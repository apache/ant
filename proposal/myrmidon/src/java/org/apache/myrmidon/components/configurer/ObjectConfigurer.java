/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

/**
 * Configures objects of a particular class.
 *
 * @author <a href="mailto:adammurdoch_ml@yahoo.com">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface ObjectConfigurer
{
    /**
     * Returns the class.
     */
    Class getType();

    /**
     * Returns a configurer for a property of this class.
     *
     * @param name The element name.
     * @return A configurer for the property.  Returns null if the property
     *         is not valid for this class.
     */
    PropertyConfigurer getProperty( String name );

    /**
     * Returns a configurer for the content of this class.
     *
     * @return A configurer for the content.  Returns null if the class does
     *         not allow text content.
     */
    PropertyConfigurer getContentConfigurer();
}
