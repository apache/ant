/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import java.util.HashMap;
import org.apache.ant.AntException;
import org.apache.ant.configuration.Configurable;
import org.apache.ant.configuration.Configuration;
import org.apache.ant.convert.ConverterEngine;
import org.apache.ant.convert.ConverterFactory;
import org.apache.ant.tasklet.Tasklet;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.avalon.Component;
import org.apache.avalon.Composer;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.Context;
import org.apache.avalon.Contextualizable;
import org.apache.avalon.DefaultComponentManager;
import org.apache.avalon.Disposable;
import org.apache.avalon.Initializable;
import org.apache.avalon.camelot.FactoryException;
import org.apache.avalon.camelot.RegistryException;
import org.apache.log.Logger;

public class DefaultTaskletEngine
    implements TaskletEngine, Initializable
{
    protected TskDeployer          m_tskDeployer;
    protected TaskletFactory       m_taskletFactory;
    protected TaskletRegistry      m_taskletRegistry;
    protected TaskletConfigurer    m_configurer;
    protected Logger               m_logger;
    protected ConverterEngine      m_converterEngine;

    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    public TskDeployer getTskDeployer()
    {
        return m_tskDeployer;
    }

    public ConverterEngine getConverterEngine()
    {
        return m_converterEngine;
    }

    public TaskletRegistry getTaskletRegistry()
    {
        return m_taskletRegistry;
    }

    public void init()
        throws Exception
    {
        m_taskletRegistry = createTaskletRegistry();
        m_taskletFactory = createTaskletFactory();

        m_converterEngine = createConverterEngine();
        m_converterEngine.setLogger( m_logger );
        setupSubComponent( m_converterEngine );

        m_configurer = createTaskletConfigurer();
        setupSubComponent( m_configurer );

        m_tskDeployer = createTskDeployer();
        m_tskDeployer.setLogger( m_logger );
        setupSubComponent( m_tskDeployer );
    }
    
    protected void setupSubComponent( final Component component )
        throws Exception
    {
        if( component instanceof Composer )
        {
            final DefaultComponentManager componentManager = new DefaultComponentManager();
            componentManager.put( "org.apache.ant.convert.Converter", 
                                  getConverterEngine() );
            componentManager.put( "org.apache.ant.convert.ConverterEngine",
                                  getConverterEngine() );
            componentManager.put( "org.apache.ant.tasklet.engine.TaskletEngine", 
                                  this );

            ((Composer)component).compose( componentManager );
        }
        
        if( component instanceof Initializable )
        {
            ((Initializable)component).init();
        }
    }
    
    protected TskDeployer createTskDeployer()
    {
        return new DefaultTskDeployer();
    }

    protected TaskletConfigurer createTaskletConfigurer()
    {
        return new DefaultTaskletConfigurer();
    }
    
    protected TaskletRegistry createTaskletRegistry()
    {
        return new DefaultTaskletRegistry();
    }
    
    protected TaskletFactory createTaskletFactory()
    {
        return new DefaultTaskletFactory();
    }
    
    protected ConverterEngine createConverterEngine()
    {
        //this is done so that the loaders are shared
        //which results in much less overhead
        final TaskletConverterEngine engine = new TaskletConverterEngine();
        engine.setConverterFactory( (ConverterFactory)m_taskletFactory );
        return engine;
    }

    public void execute( final Configuration task, 
                         final TaskletContext context, 
                         final ComponentManager componentManager )
        throws AntException
    {

        m_logger.debug( "Creating" );
        final Tasklet tasklet = createTasklet( task );

        m_logger.debug( "Contextualizing" );
        doContextualize( tasklet, task, context );

        m_logger.debug( "Composing" );
        doCompose( tasklet, task, componentManager );

        m_logger.debug( "Configuring" );
        doConfigure( tasklet, task, context );

        m_logger.debug( "Initializing" );
        doInitialize( tasklet, task );

        m_logger.debug( "Running" );
        tasklet.run();

        m_logger.debug( "Disposing" );
        doDispose( tasklet, task );
    }

    protected void doConfigure( final Tasklet tasklet, 
                                final Configuration task,
                                final TaskletContext context )
        throws AntException
    {
        try { m_configurer.configure( tasklet, task, context ); }
        catch( final Throwable throwable )
        {
            throw new AntException( "Error configuring task " +  task.getName() + " at " +
                                    task.getLocation() + "(Reason: " + 
                                    throwable.getMessage() + ")", throwable );
        }
    }
    
    protected void doCompose( final Tasklet tasklet, 
                              final Configuration task,
                              final ComponentManager componentManager )
        throws AntException
    {
        if( tasklet instanceof Composer )
        {
            try { ((Composer)tasklet).compose( componentManager ); }
            catch( final Throwable throwable )
            {
                throw new AntException( "Error composing task " +  task.getName() + " at " +
                                        task.getLocation() + "(Reason: " + 
                                        throwable.getMessage() + ")", throwable );            
            }
        }
    }

    protected void doContextualize( final Tasklet tasklet, 
                                    final Configuration task,
                                    final TaskletContext context )
        throws AntException
    {
        // Already done in container ...
        //context.setProperty( TaskletContext.NAME, name );

        try { tasklet.contextualize( context ); }
        catch( final Throwable throwable )
        {
            throw new AntException( "Error contextualizing task " +  task.getName() + " at " +
                                    task.getLocation() + "(Reason: " + 
                                    throwable.getMessage() + ")", throwable );            
        }
    }

    protected void doDispose( final Tasklet tasklet, final Configuration task )
        throws AntException
    {
        if( tasklet instanceof Disposable )
        {
            try { ((Disposable)tasklet).dispose(); }
            catch( final Throwable throwable )
            {
                throw new AntException( "Error disposing task " +  task.getName() + " at " +
                                        task.getLocation() + "(Reason: " + 
                                        throwable.getMessage() + ")", throwable );
            }
        }
    }

    protected void doInitialize( final Tasklet tasklet, final Configuration task )
        throws AntException
    {
        if( tasklet instanceof Initializable )
        {
            try { ((Initializable)tasklet).init(); }
            catch( final Throwable throwable )
            {
                throw new AntException( "Error initializing task " +  task.getName() + " at " +
                                        task.getLocation() + "(Reason: " +
                                        throwable.getMessage() + ")", throwable );
            }
        }
    }

    protected Tasklet createTasklet( final Configuration configuration )
        throws AntException
    {
        final String name = configuration.getName();
        TaskletInfo info = null;

        try { info = (TaskletInfo)m_taskletRegistry.getInfo( name ); }
        catch( final RegistryException re )
        {
            throw new AntException( "Unable to locate task " + name, re );
        }

        try { return m_taskletFactory.createTasklet( info ); }
        catch( final FactoryException fe )
        {
            throw new AntException( "Unable to create task " + name + 
                                    " (of type " + info.getClassname() + " from " +
                                    info.getLocation() + ")",
                                    fe );
        }
    }
}
