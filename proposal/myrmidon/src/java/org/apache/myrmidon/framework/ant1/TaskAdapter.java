/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.framework.ant1;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.myrmidon.api.DataType;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.type.TypeException;
import org.apache.myrmidon.components.type.TypeFactory;
import org.apache.myrmidon.components.type.TypeManager;
import org.apache.myrmidon.framework.AbstractContainerTask;
import org.apache.tools.ant.Task;

/**
 * This is the property "task" to declare a binding of a datatype to a name.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class TaskAdapter
    extends AbstractContainerTask
    implements Configurable
{
    private Task         m_ant1Task;
    private Ant1Project  m_project = new Ant1Project();

    public TaskAdapter( final Task ant1Task )
    {
        m_ant1Task = ant1Task;
    }

    protected final Task getTask()
    {
        return m_ant1Task;
    }

    protected final Ant1Project getProject()
    {
        return m_project;
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        getTask().setTaskName( configuration.getName() );

        //do configuration
        configure( getTask(), configuration );
    }

    public void execute()
        throws TaskException
    {
        try
        {
            getProject().setLogger( getLogger() );
            getProject().contextualize( getContext() );
            getProject().init();

            //getTask().setProject( getProject() );

            getTask().init();
            getTask().execute();
        }
        catch( final Exception e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }
}
