/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * An adapter for running (in Myrmidon) Ant1 tasks which do not extend Task
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class Ant1CompatTaskAdapter
    extends TaskAdapter
{
    public void configure( Configuration configuration ) throws ConfigurationException
    {
        // Create a new instance of the proxy object,
        // and configure it.
        String taskName = getAnt1Name( configuration.getName() );

        Class taskClass = (Class)project.getTaskDefinitions().get( taskName );

        if( taskClass == null )
        {
            throw new ConfigurationException( "Invalid task name for TaskAdapter: " + taskName );
        }

        Object adaptedTask = null;
        try
        {
            adaptedTask = taskClass.newInstance();
        }
        catch( Exception e )
        {
            throw new ConfigurationException( "Could not instantiate adapted task: " + taskClass.getName() );
        }

        configure( adaptedTask, configuration );

        setProxy( adaptedTask );
    }
}
