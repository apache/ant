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
import org.apache.ant.configuration.Configurer;
import org.apache.ant.configuration.DefaultConfigurer;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.ant.convert.engine.ConverterEngine;
import org.apache.ant.tasklet.Tasklet;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.logger.Loggable;
import org.apache.avalon.framework.camelot.DefaultFactory;
import org.apache.avalon.framework.camelot.DefaultRegistry;
import org.apache.avalon.framework.camelot.Factory;
import org.apache.avalon.framework.camelot.FactoryException;
import org.apache.avalon.framework.camelot.Locator;
import org.apache.avalon.framework.camelot.Registry;
import org.apache.avalon.framework.camelot.RegistryException;
import org.apache.log.Logger;

public class DefaultTaskletEngine
    extends AbstractLoggable
    implements TaskletEngine, Composable
{
    protected TskDeployer          m_tskDeployer;
    protected Factory              m_factory;
    protected Registry             m_locatorRegistry   = new DefaultRegistry( Locator.class );
    protected Configurer           m_configurer;
    protected DataTypeEngine       m_dataTypeEngine;
    protected ConverterEngine      m_converterEngine;

    protected ComponentManager     m_componentManager;

    public TskDeployer getTskDeployer()
    {
        return m_tskDeployer;
    }

    public ConverterEngine getConverterEngine()
    {
        return m_converterEngine;
    }

    public Registry getRegistry()
    {
        return m_locatorRegistry;
    }

    /**
     * Retrieve datatype engine.
     *
     * @return the DataTypeEngine
     */
    public DataTypeEngine getDataTypeEngine()
    {
        return m_dataTypeEngine;
    }
    
    /**
     * Retrieve relevent services needed to deploy.
     *
     * @param componentManager the ComponentManager
     * @exception ComponentException if an error occurs
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        //cache CM so it can be used while executing tasks
        m_componentManager = componentManager;

        m_factory = (Factory)componentManager.lookup( "org.apache.avalon.framework.camelot.Factory" );
        m_tskDeployer = (TskDeployer)componentManager.
            lookup( "org.apache.ant.tasklet.engine.TskDeployer" );
        m_configurer = (Configurer)componentManager.
            lookup( "org.apache.ant.configuration.Configurer" );
        m_dataTypeEngine = (DataTypeEngine)componentManager.
            lookup( "org.apache.ant.tasklet.engine.DataTypeEngine" );
        m_converterEngine = (ConverterEngine)componentManager.
            lookup( "org.apache.ant.convert.engine.ConverterEngine" );
    }

    public void execute( final Configuration task, final TaskletContext context )
        throws AntException
    {
        getLogger().debug( "Creating" );
        final Tasklet tasklet = createTasklet( task.getName() );
        setupLogger( tasklet );

        getLogger().debug( "Contextualizing" );
        doContextualize( tasklet, task, context );

        getLogger().debug( "Composing" );
        doCompose( tasklet, task );

        getLogger().debug( "Configuring" );
        doConfigure( tasklet, task, context );

        getLogger().debug( "Initializing" );
        doInitialize( tasklet, task );

        getLogger().debug( "Running" );
        try { tasklet.execute(); }
        catch( final Exception e )
        {
            throw new AntException( "Error executing task", e );
        }

        getLogger().debug( "Disposing" );
        doDispose( tasklet, task );
    }
    
    protected Tasklet createTasklet( final String name )
        throws AntException
    {
        try
        {
            final Locator locator = (Locator)m_locatorRegistry.getInfo( name, Locator.class );
            return (Tasklet)m_factory.create( locator, Tasklet.class );
        }
        catch( final RegistryException re )
        {
            throw new AntException( "Unable to locate task " + name, re );
        }
        catch( final FactoryException fe )
        {
            throw new AntException( "Unable to create task " + name, fe );
        }
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
    
    protected void doCompose( final Tasklet tasklet, final Configuration task )
        throws AntException
    {
        if( tasklet instanceof Composable )
        {
            try { ((Composable)tasklet).compose( m_componentManager ); }
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
        try 
        { 
            if( tasklet instanceof Contextualizable )
            {
                ((Contextualizable)tasklet).contextualize( context ); 
            }
        }
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
            try { ((Initializable)tasklet).initialize(); }
            catch( final Throwable throwable )
            {
                throw new AntException( "Error initializing task " +  task.getName() + " at " +
                                        task.getLocation() + "(Reason: " +
                                        throwable.getMessage() + ")", throwable );
            }
        }
    }
}
