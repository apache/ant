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
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.i18n.ResourceManager;

/**
 * An adapter for running (in Myrmidon) Ant1 tasks which do not extend Task
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class Ant1CompatTaskAdapter
    extends TaskAdapter
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( Ant1CompatTaskAdapter.class );

    /**
     * Gets the adapted task name from the configuration, and looks up the
     * Class for the adapted task. The adapted task is then instantiated and
     * configured.
     * @param configuration The Task Model
     * @throws ConfigurationException If the configuration is invalid.
     */
    public void configure( Configuration configuration )
        throws ConfigurationException
    {
        // Create a new instance of the proxy object,
        // and configure it.
        String taskName = getAnt1Name( configuration.getName() );

        Class taskClass = (Class)project.getTaskDefinitions().get( taskName );

        if( taskClass == null )
        {
            String message =
                REZ.getString( "taskadapter.invalid-task-name.error", taskName );
            throw new ConfigurationException( message );
        }

        Object adaptedTask = null;
        try
        {
            adaptedTask = taskClass.newInstance();
        }
        catch( Exception e )
        {
            String message =
                REZ.getString( "taskadapter.no-create.error", taskClass.getName() );
            throw new ConfigurationException( message );
        }

        configure( adaptedTask, configuration );

        setProxy( adaptedTask );
    }
}
