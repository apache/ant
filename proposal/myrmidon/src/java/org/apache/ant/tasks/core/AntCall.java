/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasks.core;

import java.util.ArrayList;
import org.apache.ant.AntException;
import org.apache.ant.project.ProjectEngine;
import org.apache.ant.project.Project;
import org.apache.ant.tasklet.AbstractTasklet;
import org.apache.ant.tasklet.DefaultTaskletContext;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentNotAccessibleException;
import org.apache.avalon.ComponentNotFoundException;
import org.apache.avalon.Composer;

/**
 * This is abstract base class for tasklets.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class AntCall 
    extends AbstractTasklet
    implements Composer
{
    protected ProjectEngine         m_projectEngine;
    protected Project               m_project;
    protected String                m_target;
    protected ArrayList             m_properties     = new ArrayList();

    public void compose( final ComponentManager componentManager )
        throws ComponentNotFoundException, ComponentNotAccessibleException
    {
        m_projectEngine = (ProjectEngine)componentManager.
            lookup( "org.apache.ant.project.ProjectEngine" );
        m_project = (Project)componentManager.lookup( "org.apache.ant.project.Project" );
    }

    public void setTarget( final String target )
    {
        m_target = target;
    }

    public Property createParam()
    {
        final Property property = new Property();
        m_properties.add( property );
        return property;
    }

    public void run()
        throws AntException
    {
        if( null == m_target )
        {
            throw new AntException( "Target attribute must be specified" );
        }

        final TaskletContext context = new DefaultTaskletContext( getContext() );

        final int size = m_properties.size();
        for( int i = 0; i < size; i++ )
        {
            final Property property = (Property)m_properties.get( i );
            property.contextualize( context );
            property.run();
        }

        getLogger().info( "Calling target " + m_target );
        m_projectEngine.execute( m_project, m_target, context );
    }
}
