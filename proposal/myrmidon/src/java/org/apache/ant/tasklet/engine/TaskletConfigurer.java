/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import org.apache.ant.configuration.Configuration;
import org.apache.ant.tasklet.Tasklet;
import org.apache.avalon.Component;
import org.apache.avalon.ConfigurationException;
import org.apache.avalon.Context;

/**
 * Class used to configure tasks.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface TaskletConfigurer
    extends Component
{
    /**
     * Configure a task based on a configuration in a particular context.
     * This configuring can be done in different ways for different 
     * configurers.
     *
     * @param tasklet the tasklet
     * @param configuration the configuration
     * @param context the Context
     * @exception ConfigurationException if an error occurs
     */
    void configure( Tasklet tasklet, Configuration configuration, Context context )
        throws ConfigurationException;
}
