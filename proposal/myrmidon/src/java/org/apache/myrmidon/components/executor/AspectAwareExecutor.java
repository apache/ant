/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.executor;

import java.util.ArrayList;
import java.util.HashMap;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.log.Logger;
import org.apache.myrmidon.api.Task;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.aspects.AspectHandler;
import org.apache.myrmidon.interfaces.aspect.AspectManager;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;

public class AspectAwareExecutor
    extends DefaultExecutor
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( AspectAwareExecutor.class );

    private final static Parameters       EMPTY_PARAMETERS;
    private final static Configuration[]  EMPTY_ELEMENTS       = new Configuration[ 0 ];

    static
    {
        EMPTY_PARAMETERS = new Parameters();
        EMPTY_PARAMETERS.makeReadOnly();
    }

    private AspectManager        m_aspectManager;

    /**
     * Retrieve relevent services.
     *
     * @param componentManager the ComponentManager
     * @exception ComponentException if an error occurs
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        super.compose( componentManager );

        m_aspectManager = (AspectManager)componentManager.lookup( AspectManager.ROLE );
    }

    public void execute( final Configuration taskModel, final ExecutionFrame frame )
        throws TaskException
    {
        try
        {
            executeTask( taskModel, frame );
        }
        catch( final TaskException te )
        {
            if( false == getAspectManager().error( te ) )
            {
                throw te;
            }
        }
    }

    private void executeTask( Configuration taskModel, final ExecutionFrame frame )
        throws TaskException
    {
        taskModel = getAspectManager().preCreate( taskModel );
        taskModel = prepareAspects( taskModel );

        debug( "creating.notice" );
        final Task task = createTask( taskModel.getName(), frame );
        getAspectManager().postCreate( task );

        debug( "logger.notice" );
        final Logger logger = frame.getLogger();
        getAspectManager().preLoggable( logger );
        doLoggable( task, taskModel, logger );

        debug( "contextualizing.notice" );
        doContextualize( task, taskModel, frame.getContext() );

        debug( "composing.notice" );
        doCompose( task, taskModel, frame.getComponentManager() );

        debug( "configuring.notice" );
        getAspectManager().preConfigure( taskModel );
        doConfigure( task, taskModel, frame.getContext() );

        debug( "initializing.notice" );
        doInitialize( task, taskModel );

        debug( "executing.notice" );
        getAspectManager().preExecute();
        doExecute( taskModel, task );

        debug( "disposing.notice" );
        getAspectManager().preDestroy();
        doDispose( task, taskModel );
    }

    protected void doExecute( final Configuration taskModel, final Task task )
        throws TaskException
    {
        task.execute();
    }

    //TODO: Extract and clean taskModel here.
    //Get all parameters from model and provide to appropriate aspect.
    //aspect( final Parameters parameters, final Configuration[] elements )
    private final Configuration prepareAspects( final Configuration taskModel )
        throws TaskException
    {
        final DefaultConfiguration newTaskModel =
            new DefaultConfiguration( taskModel.getName(), taskModel.getLocation() );
        final HashMap parameterMap = new HashMap();
        final HashMap elementMap = new HashMap();

        processAttributes( taskModel, newTaskModel, parameterMap );
        processElements( taskModel, newTaskModel, elementMap );

        dispatchAspectsSettings( parameterMap, elementMap );
        checkForUnusedSettings( parameterMap, elementMap );

        return newTaskModel;
    }

    private final void dispatchAspectsSettings( final HashMap parameterMap,
                                                final HashMap elementMap )
        throws TaskException
    {
        final String[] names = getAspectManager().getNames();

        for( int i = 0; i < names.length; i++ )
        {
            final ArrayList elementList = (ArrayList)elementMap.remove( names[ i ] );

            Parameters parameters = (Parameters)parameterMap.remove( names[ i ] );
            if( null == parameters ) parameters = EMPTY_PARAMETERS;

            Configuration[] elements = null;
            if( null == elementList ) elements = EMPTY_ELEMENTS;
            else
            {
                elements = (Configuration[])elementList.toArray( EMPTY_ELEMENTS );
            }

            dispatch( names[ i ], parameters, elements );
        }
    }

    private final void checkForUnusedSettings( final HashMap parameterMap,
                                               final HashMap elementMap )
        throws TaskException
    {
        if( 0 != parameterMap.size() )
        {
            final String[] namespaces =
                (String[])parameterMap.keySet().toArray( new String[ 0 ] );

            for( int i = 0; i < namespaces.length; i++ )
            {
                final String namespace = namespaces[ i ];
                final Parameters parameters = (Parameters)parameterMap.get( namespace );
                final ArrayList elementList = (ArrayList)elementMap.remove( namespace );

                Configuration[] elements = null;

                if( null == elementList ) elements = EMPTY_ELEMENTS;
                else
                {
                    elements = (Configuration[])elementList.toArray( EMPTY_ELEMENTS );
                }

                unusedSetting( namespace, parameters, elements );
            }
        }

        if( 0 != elementMap.size() )
        {
            final String[] namespaces =
                (String[])elementMap.keySet().toArray( new String[ 0 ] );

            for( int i = 0; i < namespaces.length; i++ )
            {
                final String namespace = namespaces[ i ];
                final ArrayList elementList = (ArrayList)elementMap.remove( namespace );
                final Configuration[] elements =
                    (Configuration[])elementList.toArray( EMPTY_ELEMENTS );

                unusedSetting( namespace, EMPTY_PARAMETERS, elements );
            }
        }
    }

    private void unusedSetting( final String namespace,
                                final Parameters parameters,
                                final Configuration[] elements )
        throws TaskException
    {
        final String message =
            REZ.getString( "unused-settings.error",
                           namespace,
                           Integer.toString( parameters.getNames().length ),
                           Integer.toString( elements.length ) );
        throw new TaskException( message );
    }

    private void dispatch( final String namespace,
                           final Parameters parameters,
                           final Configuration[] elements )
        throws TaskException
    {
        getAspectManager().dispatchAspectSettings( namespace, parameters, elements );

        if( getLogger().isDebugEnabled() )
        {
            final String message =
                REZ.getString( "dispatch-settings.notice",
                               namespace,
                               Integer.toString( parameters.getNames().length ),
                               Integer.toString( elements.length ) );
            getLogger().debug( message );
        }
    }

    private final void processElements( final Configuration taskModel,
                                        final DefaultConfiguration newTaskModel,
                                        final HashMap map )
    {
        final Configuration[] elements = taskModel.getChildren();
        for( int i = 0; i < elements.length; i++ )
        {
            final String name = elements[ i ].getName();
            final int index = name.indexOf( ':' );

            if( -1 == index )
            {
                newTaskModel.addChild( elements[ i ] );
            }
            else
            {
                final String namespace = name.substring( 0, index );
                final String localName = name.substring( index + 1 );
                final ArrayList elementSet = getElements( namespace, map );
                elementSet.add( elements[ i ] );
            }
        }
    }

    private final void processAttributes( final Configuration taskModel,
                                          final DefaultConfiguration newTaskModel,
                                          final HashMap map )
    {
        final String[] attributes = taskModel.getAttributeNames();
        for( int i = 0; i < attributes.length; i++ )
        {
            final String name = attributes[ i ];
            final String value = taskModel.getAttribute( name, null );

            final int index = name.indexOf( ':' );

            if( -1 == index )
            {
                newTaskModel.setAttribute( name, value );
            }
            else
            {
                final String namespace = name.substring( 0, index );
                final String localName = name.substring( index + 1 );
                final Parameters parameters = getParameters( namespace, map );
                parameters.setParameter( localName, value );
            }
        }
    }

    private final ArrayList getElements( final String namespace, final HashMap map )
    {
        ArrayList elements = (ArrayList)map.get( namespace );

        if( null == elements )
        {
            elements = new ArrayList();
            map.put( namespace, elements );
        }

        return elements;
    }

    private final Parameters getParameters( final String namespace, final HashMap map )
    {
        Parameters parameters = (Parameters)map.get( namespace );

        if( null == parameters )
        {
            parameters = new Parameters();
            map.put( namespace, parameters );
        }

        return parameters;
    }

    protected final AspectManager getAspectManager()
    {
        return m_aspectManager;
    }
}
