/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.ant.AntException;
import org.apache.ant.configuration.Configuration;
import org.apache.ant.convert.ConverterRegistry;
import org.apache.ant.convert.DefaultConverterRegistry;
import org.apache.ant.tasklet.DefaultTaskletContext;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.ant.tasklet.engine.DefaultTaskletEngine;
import org.apache.ant.tasklet.engine.DefaultTaskletInfo;
import org.apache.ant.tasklet.engine.DefaultTaskletRegistry;
import org.apache.ant.tasklet.engine.TaskletEngine;
import org.apache.ant.tasklet.engine.TaskletRegistry;
import org.apache.ant.tasklet.engine.TskDeployer;
import org.apache.avalon.DefaultComponentManager;
import org.apache.avalon.Disposable;
import org.apache.avalon.Initializable;
import org.apache.avalon.camelot.Deployer;
import org.apache.avalon.camelot.DeploymentException;
import org.apache.avalon.camelot.RegistryException;
import org.apache.log.Logger;

public class DefaultProjectEngine
    implements ProjectEngine, Initializable, Disposable
{
    protected Deployer              m_deployer;
    protected TaskletRegistry       m_taskletRegistry;
    protected ConverterRegistry     m_converterRegistry;
    protected TaskletEngine         m_taskletEngine;
    protected Logger                m_logger;

    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    public void init()
        throws Exception
    {
        m_taskletEngine = createTaskletEngine();

        m_taskletRegistry = createTaskletRegistry();
        m_converterRegistry = createConverterRegistry();
        m_deployer = createDeployer();

        //final DefaultTaskletContext context = new DefaultTaskletContext();
        //m_taskletEngine.contextualize( context );

        final DefaultComponentManager componentManager = new DefaultComponentManager();
        componentManager.put( "org.apache.ant.tasklet.engine.TaskletRegistry", 
                              m_taskletRegistry );

        componentManager.put( "org.apache.ant.convert.ConverterRegistry", 
                              m_converterRegistry );

        componentManager.put( "org.apache.avalon.camelot.Deployer", m_deployer );
       
        m_taskletEngine.compose( componentManager );
        
        if( m_taskletEngine instanceof Initializable )
        {
            ((Initializable)m_taskletEngine).init();
        }
    }

    public void dispose()
        throws Exception
    {
        if( m_taskletEngine instanceof Disposable )
        {
            ((Disposable)m_taskletEngine).dispose();
        }
    }

    public Deployer getDeployer()
    {
        return m_deployer;
    }

    protected TaskletEngine createTaskletEngine()
    {
        return new DefaultTaskletEngine();
    }    
    
    protected TaskletRegistry createTaskletRegistry()
    {
        return new DefaultTaskletRegistry();
    }
    
    protected ConverterRegistry createConverterRegistry()
    {
        return new DefaultConverterRegistry();
    }
       
    protected Deployer createDeployer()
    {
        final TskDeployer deployer = 
            new TskDeployer( m_taskletRegistry, m_converterRegistry );
        deployer.setLogger( m_logger );
        return deployer;
    }
        
    public void execute( final Project project, final String target )
        throws AntException
    {
        m_taskletEngine.contextualize( project.getContext() );
        executeTarget( "<init>", project.getImplicitTarget() );

        final ArrayList done = new ArrayList();
        execute( project, target, done );
    }

    protected void execute( final Project project, 
                            final String targetName, 
                            final ArrayList done )
        throws AntException
    {
        final Target target = project.getTarget( targetName );

        if( null == target )
        {
            throw new AntException( "Unable to find target " + targetName );
        }

        done.add( targetName );

        final Iterator dependencies = target.getDependencies();
        while( dependencies.hasNext() )
        {
            final String dependency = (String)dependencies.next();
            if( !done.contains( dependency ) )
            {
                execute( project, dependency, done );
            }
        }

        final TaskletContext context = getContextFor( project, targetName );
        m_taskletEngine.contextualize( context );
        executeTarget( targetName, target );
    }

    protected TaskletContext getContextFor( final Project project, final String targetName )
    {
        final DefaultTaskletContext context = 
            new DefaultTaskletContext( project.getContext() );

        context.setProperty( Project.TARGET, targetName );
        context.put( TaskletContext.LOGGER, m_logger );

        return context;
    }

    protected void executeTarget( final String targetName, final Target target )
        throws AntException
    {
        m_logger.debug( "Executing target " + targetName );
        
        final Iterator tasks = target.getTasks();
        while( tasks.hasNext() )
        {
            final Configuration task = (Configuration)tasks.next();
            executeTask( task );
        }
    }

    protected void executeTask( final Configuration configuration )
        throws AntException
    {
        final String name = configuration.getName();
        m_logger.debug( "Executing task " + name );

        m_taskletEngine.execute( configuration );
    }
}
