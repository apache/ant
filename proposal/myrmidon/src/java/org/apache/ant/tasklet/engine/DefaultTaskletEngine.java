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
import org.apache.ant.convert.ConverterFactory;
import org.apache.ant.convert.ConverterRegistry;
import org.apache.ant.tasklet.Tasklet;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentNotAccessibleException;
import org.apache.avalon.ComponentNotFoundException;
import org.apache.avalon.Composer;
import org.apache.avalon.Context;
import org.apache.avalon.Contextualizable;
import org.apache.avalon.DefaultComponentManager;
import org.apache.avalon.Disposable;
import org.apache.avalon.Initializable;
import org.apache.avalon.camelot.FactoryException;
import org.apache.avalon.camelot.RegistryException;
import org.apache.log.Logger;

public class DefaultTaskletEngine
    implements TaskletEngine, Initializable, Composer
{
    protected TaskletFactory       m_taskletFactory;
    protected ConverterFactory     m_converterFactory;
    protected TaskletRegistry      m_taskletRegistry;
    protected ConverterRegistry    m_converterRegistry;
    protected TaskletConfigurer    m_configurer;
    protected Logger               m_logger;

    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentNotFoundException, ComponentNotAccessibleException
    {
        m_taskletRegistry = (TaskletRegistry)componentManager.
            lookup( "org.apache.ant.tasklet.engine.TaskletRegistry" );
        m_converterRegistry = (ConverterRegistry)componentManager.
            lookup( "org.apache.ant.convert.ConverterRegistry" );
    }

    public TaskletRegistry getTaskletRegistry()
    {
        return m_taskletRegistry;
    }

    public ConverterRegistry getConverterRegistry()
    {
        return m_converterRegistry;
    }

    public void init()
        throws Exception
    {
        m_taskletFactory = createTaskletFactory();
        m_converterFactory =  createConverterFactory();
        m_configurer = createTaskletConfigurer();

        if( m_configurer instanceof Composer )
        {
            final DefaultComponentManager componentManager = new DefaultComponentManager();
            componentManager.put( "org.apache.ant.convert.ConverterFactory", 
                                  m_converterFactory );
            componentManager.put( "org.apache.ant.convert.ConverterRegistry",
                                  m_converterRegistry );

            ((Composer)m_configurer).compose( componentManager );
        }

        if( m_configurer instanceof Initializable )
        {
            ((Initializable)m_configurer).init();
        }
    }

    protected TaskletConfigurer createTaskletConfigurer()
    {
        return new DefaultTaskletConfigurer();
    }

    protected TaskletFactory createTaskletFactory()
    {
        return new DefaultTaskletFactory();
    }

    protected ConverterFactory createConverterFactory()
    {
        return (ConverterFactory)m_taskletFactory;
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

        TaskletEntry entry = null;

        try { entry = m_taskletFactory.create( info ); }
        catch( final FactoryException fe )
        {
            throw new AntException( "Unable to create task " + name + 
                                    " (of type " + info.getClassname() + " from " +
                                    info.getLocation() + ")",
                                    fe );
        }
        
        return entry.getTasklet();
    }
}
