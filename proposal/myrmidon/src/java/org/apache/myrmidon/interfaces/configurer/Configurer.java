/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.configurer;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.myrmidon.api.TaskContext;

/**
 * Class used to configure tasks.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant:role shorthand="configurer"
 */
public interface Configurer
{
    String ROLE = Configurer.class.getName();

    /**
     * Configure an object based on a configuration in a particular context.
     * This configuring can be done in different ways for different
     * configurers.
     *
     * @param object the object
     * @param configuration the configuration
     * @param context the Context
     * @exception ConfigurationException if an error occurs
     */
    void configure( Object object, Configuration configuration, TaskContext context )
        throws ConfigurationException;

    /**
     * Configure named attribute of object in a particular context.
     * This configuring can be done in different ways for different
     * configurers.
     *
     * @param object the object
     * @param name the attribute name
     * @param value the attribute value
     * @param context the Context
     * @exception ConfigurationException if an error occurs
     */
    void configure( Object object, String name, String value, TaskContext context )
        throws ConfigurationException;
}
