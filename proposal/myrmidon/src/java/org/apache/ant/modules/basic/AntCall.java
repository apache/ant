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
import org.apache.ant.project.Project;
import org.apache.ant.project.ProjectEngine;
import org.apache.ant.tasklet.AbstractTasklet;
import org.apache.ant.tasklet.DefaultTaskletContext;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentManagerException;
import org.apache.avalon.Composer;
import org.apache.avalon.Context;

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
    protected TaskletContext        m_childContext;
    protected ComponentManager      m_componentManager;

    public void contextualize( final Context context )
    {
        super.contextualize( context );
        m_childContext = new DefaultTaskletContext( getContext() );
    } 

    public void compose( final ComponentManager componentManager )
        throws ComponentManagerException
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

    public void run()
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
            property.run();
        }

        getLogger().info( "Calling target " + m_target );
        m_projectEngine.execute( m_project, m_target, m_childContext );
    }
}
