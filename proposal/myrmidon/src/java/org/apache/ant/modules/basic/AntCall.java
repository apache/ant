/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.basic;

import java.util.ArrayList;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.context.Context;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.DefaultTaskContext;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.components.manager.ProjectManager;
import org.apache.myrmidon.components.model.Project;

/**
 * This is abstract base class for tasklets.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class AntCall
    extends AbstractTask
    implements Composable
{
    private ProjectManager        m_projectManager;
    private Project               m_project;
    private String                m_target;
    private ArrayList             m_properties     = new ArrayList();
    private TaskContext           m_childContext;
    private ComponentManager      m_componentManager;

    public void contextualize( final Context context )
    {
        super.contextualize( context );
        m_childContext = new DefaultTaskContext( getContext() );
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_componentManager = componentManager;
        m_projectManager = (ProjectManager)componentManager.lookup( ProjectManager.ROLE );
        m_project = (Project)componentManager.lookup( Project.ROLE );
    }

    public void setTarget( final String target )
    {
        m_target = target;
    }

    public Property createParam()
        throws Exception
    {
        final Property property = new Property();
        property.setLogger( getLogger() );
        property.contextualize( m_childContext );
        property.compose( m_componentManager );
        m_properties.add( property );
        return property;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_target )
        {
            throw new TaskException( "Target attribute must be specified" );
        }

        final int size = m_properties.size();
        for( int i = 0; i < size; i++ )
        {
            final Property property = (Property)m_properties.get( i );
            property.execute();
        }

        getLogger().info( "Calling target " + m_target );

        //This calls startProject() which is probably not wanted???
        //TODO: FIXME when scoping is decided
        //m_projectManager.executeProject( m_project, m_target );
        getLogger().warn( "ANTCALL NOT IMPLEMENTED - waiting for " + 
                          "scope rules to be decided" );
    }
}
