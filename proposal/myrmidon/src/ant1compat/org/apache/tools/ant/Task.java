/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.util.Locale;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * Ant1 Task proxy for Myrmidon.
 * Note that this class and OriginalAnt1Task (superclass) comprise a single logical
 * class, but the code is kept separate for ease of development. OriginalAnt1Task
 * is barely modified from the Ant1 original, whereas this class contains
 * all of the Myrmidon-specific adaptations.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class Task extends OriginalAnt1Task
    implements org.apache.myrmidon.api.Task, Configurable
{
    protected TaskContext m_context;

    /**
     * Specify the context in which the task operates in.
     * The Task will use the TaskContext to receive information
     * about it's environment.
     */
    public void contextualize( TaskContext context )
        throws TaskException
    {
        m_context = context;

        this.setTaskType( context.getName() );
        this.setTaskName( context.getName() );

        // Create/recontextualise the Ant1 Project.
        Ant1CompatProject project =
            (Ant1CompatProject)context.getProperty( Ant1CompatProject.ANT1_PROJECT_PROP );
        if( project == null )
        {
            project = createProject();
            m_context.setProperty( Ant1CompatProject.ANT1_PROJECT_PROP, project );
        }
        else
        {
            project.recontextulize( context );
        }

        this.setProject( project );
    }

    /**
     * Create and initialise an Ant1CompatProject
     */
    private Ant1CompatProject createProject()
        throws TaskException
    {
        Ant1CompatProject project = new Ant1CompatProject( m_context );
        project.init();
        return project;
    }

    /**
     * Uses the task Configuration to perform Ant1-style configuration
     * on the Ant1 task. This method configures *all* tasks the way Ant1
     * configures tasks inside a target.
     *
     * @param configuration The TaskModel for this Ant1 Task.
     * @throws ConfigurationException if the Configuration supplied is not valid
     */
    public void configure( Configuration configuration ) throws ConfigurationException
    {
        configure( this, configuration );
    }

    /**
     * Uses reflection to configure any Object, with the help of the Ant1
     * IntrospectionHelper. using . This aims to mimic (to some extent) the
     * Ant1-style configuration rules implemented by ProjectHelperImpl.
     * @param target
     *          The object to be configured.
     * @param configuration
     *          The data to configure the object with.
     * @throws ConfigurationException
     *          If the Configuration is not valid for the configured object
     */
    protected void configure( Object target, Configuration configuration ) throws ConfigurationException
    {
        //TODO Maybe provide different configuration order for tasks not in a target,
        // elements in a TaskContainer etc...
        Ant1CompatConfigurer configurer =
            new Ant1CompatConfigurer( target, configuration, project );
        configurer.createChildren();
        configurer.configure();
        this.init();
    }

    /**
     * Returns the name of a Task/Datatype as referred to by Ant1 code, without
     * the "ant1." prefix.
     * @param fullName The full name as known by Myrmidon.
     * @return the name without the Ant1 prefix.
     */
    protected String getAnt1Name( String fullName )
    {
        return fullName.substring( Ant1CompatProject.ANT1_TASK_PREFIX.length() );
    }
}
