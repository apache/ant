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
import org.apache.ant.configuration.Configurer;
import org.apache.ant.configuration.DefaultConfigurer;
import org.apache.ant.convert.engine.ConverterEngine;
import org.apache.ant.tasklet.Tasklet;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.avalon.AbstractLoggable;
import org.apache.avalon.Component;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.DefaultComponentManager;
import org.apache.avalon.ComponentManagerException;
import org.apache.avalon.Composer;
import org.apache.avalon.Context;
import org.apache.avalon.Contextualizable;
import org.apache.avalon.DefaultComponentManager;
import org.apache.avalon.Disposable;
import org.apache.avalon.Initializable;
import org.apache.avalon.Loggable;
import org.apache.avalon.camelot.DefaultFactory;
import org.apache.avalon.camelot.DefaultLocatorRegistry;
import org.apache.avalon.camelot.Factory;
import org.apache.avalon.camelot.FactoryException;
import org.apache.avalon.camelot.Locator;
import org.apache.avalon.camelot.LocatorRegistry;
import org.apache.avalon.camelot.RegistryException;
import org.apache.log.Logger;

public class DefaultTaskletEngine
    extends AbstractLoggable
    implements TaskletEngine, Composer
{
    protected TskDeployer          m_tskDeployer;
    protected Factory              m_factory;
    protected LocatorRegistry      m_locatorRegistry   = new DefaultLocatorRegistry();
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

    public LocatorRegistry getRegistry()
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
     * @exception ComponentManagerException if an error occurs
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentManagerException
    {
        //cache CM so it can be used while executing tasks
        m_componentManager = componentManager;

        m_factory = (Factory)componentManager.lookup( "org.apache.avalon.camelot.Factory" );
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
        tasklet.run();

        getLogger().debug( "Disposing" );
        doDispose( tasklet, task );
    }
    
    protected Tasklet createTasklet( final String name )
        throws AntException
    {
        try
        {
            final Locator locator = m_locatorRegistry.getLocator( name );
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
        if( tasklet instanceof Composer )
        {
            try { ((Composer)tasklet).compose( m_componentManager ); }
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
}
