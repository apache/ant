/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.basic;

import java.util.ArrayList;
import org.apache.ant.AntException;
import org.apache.myrmidon.components.model.Project;
import org.apache.ant.project.ProjectEngine;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.DefaultTaskContext;
import org.apache.myrmidon.api.TaskContext;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.context.Context;

/**
 * This is abstract base class for tasklets.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class AntCall 
    extends AbstractTask
    implements Composable
{
    protected ProjectEngine         m_projectEngine;
    protected Project               m_project;
    protected String                m_target;
    protected ArrayList             m_properties     = new ArrayList();
    protected TaskContext           m_childContext;
    protected ComponentManager      m_componentManager;

    public void contextualize( final Context context )
    {
        super.contextualize( context );
        m_childContext = new DefaultTaskContext( getContext() );
    } 

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_componentManager = componentManager;
        m_projectEngine = (ProjectEngine)componentManager.
            lookup( "org.apache.ant.project.ProjectEngine" );
        m_project = (Project)componentManager.lookup( "org.apache.ant.project.Project" );
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
        throws AntException
    {
        if( null == m_target )
        {
            throw new AntException( "Target attribute must be specified" );
        }

        final int size = m_properties.size();
        for( int i = 0; i < size; i++ )
        {
            final Property property = (Property)m_properties.get( i );
            property.execute();
        }

        getLogger().info( "Calling target " + m_target );
        //This calls startProject() which is probably not wanted???
        m_projectEngine.executeTarget( m_project, m_target, m_childContext );
    }
}
