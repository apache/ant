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
import org.apache.avalon.Composer;
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
    protected Deployer                 m_deployer;
    protected TaskletRegistry          m_taskletRegistry;
    protected ConverterRegistry        m_converterRegistry;
    protected TaskletEngine            m_taskletEngine;
    protected Logger                   m_logger;
    protected ProjectListenerSupport   m_listenerSupport;
    protected DefaultComponentManager  m_componentManager;
    
    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    public void addProjectListener( final ProjectListener listener )
    {
        m_listenerSupport.addProjectListener( listener );
    }

    public void removeProjectListener( final ProjectListener listener )
    {
        m_listenerSupport.removeProjectListener( listener );
    }

    public void init()
        throws Exception
    {
        m_listenerSupport = new ProjectListenerSupport();

        m_taskletRegistry = createTaskletRegistry();
        m_converterRegistry = createConverterRegistry();
        m_deployer = createDeployer();

        setupTaskletEngine();

        m_componentManager = new DefaultComponentManager();
        m_componentManager.put( "org.apache.ant.project.ProjectEngine", this );
        m_componentManager.put( "org.apache.ant.tasklet.engine.TaskletEngine", m_taskletEngine );
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

    protected void setupTaskletEngine()
        throws Exception
    {
        m_taskletEngine = createTaskletEngine();
        m_taskletEngine.setLogger( m_logger );
        
        if( m_taskletEngine instanceof Composer )
        {
            final DefaultComponentManager componentManager = new DefaultComponentManager();
            componentManager.put( "org.apache.ant.tasklet.engine.TaskletRegistry", 
                                  m_taskletRegistry );
            componentManager.put( "org.apache.ant.convert.ConverterRegistry",
                                  m_converterRegistry );
            
            ((Composer)m_taskletEngine).compose( componentManager );
        }
        
        if( m_taskletEngine instanceof Initializable )
        {
            ((Initializable)m_taskletEngine).init();
        }
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
        m_componentManager.put( "org.apache.ant.project.Project", project );

        final TaskletContext context = project.getContext();

        final String projectName = (String)context.getProperty( Project.PROJECT );

        m_listenerSupport.projectStarted( projectName );

        executeTargetWork( "<init>", project.getImplicitTarget(), context );

        //context = new DefaultTaskletContext( context );
        
        //placing logger lower (at targetlevel or at task level)
        //is possible if you want more fine grained control
        context.setProperty( TaskletContext.LOGGER, m_logger );

        execute( project, target, context );

        m_listenerSupport.projectFinished();
    }

    public void execute( Project project, String target, TaskletContext context )
        throws AntException
    {
        execute( project, target, context, new ArrayList() );
    }

    protected void execute( final Project project, 
                            final String targetName, 
                            final TaskletContext context,
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
                execute( project, dependency, context, done );
            }
        }

        executeTarget( targetName, target, context );
    }

    protected void executeTarget( final String targetName, 
                                  final Target target, 
                                  final TaskletContext context )
        throws AntException
    {
        m_componentManager.put( "org.apache.ant.project.Target", target );

        final TaskletContext targetContext = new DefaultTaskletContext( context );
        targetContext.setProperty( Project.TARGET, targetName );
        
        m_listenerSupport.targetStarted( targetName );

        executeTargetWork( targetName, target, targetContext );
        
        m_listenerSupport.targetFinished();
    }

    protected void executeTargetWork( final String name, 
                                      final Target target, 
                                      final TaskletContext context )
    {
        m_logger.debug( "Executing target " + name );

        final Iterator tasks = target.getTasks();
        while( tasks.hasNext() )
        {
            final Configuration task = (Configuration)tasks.next();
            executeTask( task, context );
        }
    }

    protected void executeTask( final Configuration configuration, 
                                final TaskletContext context )
        throws AntException
    {
        final String name = configuration.getName();
        m_logger.debug( "Executing task " + name );

        //Set up context for task...

        //is Only necessary if we are multi-threaded
        //final TaskletContext targetContext = new DefaultTaskletContext( context );
        context.setProperty( TaskletContext.NAME, name );

        //notify listeners
        m_listenerSupport.taskletStarted( name );

        //run task
        m_taskletEngine.execute( configuration, context, m_componentManager );

        //notify listeners task has ended
        m_listenerSupport.taskletFinished();
    }
}
