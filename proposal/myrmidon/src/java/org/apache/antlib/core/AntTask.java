/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.embeddor.Embeddor;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.workspace.Workspace;

/**
 * Create a new Workspace and process a build in
 * that new workspace.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.task name="ant"
 */
public class AntTask
    extends AbstractTask
{
    /**
     * Default build file.
     */
    private static final String DEFAULT_BUILD_FILE = "build.ant";

    /**
     * If true, inherit all properties from parent Project
     * If false, inherit only userProperties and those defined
     * inside the ant call itself
     */
    private boolean m_inheritAll;

    /**
     * The build file which to execute. If not set defaults to
     * using "build.ant" in the basedir of current project.
     */
    private File m_file;

    /**
     * The target to process in build file. If not specified
     * will use default in specified build file.
     */
    private String m_target;

    /**
     * The "type" of the build file. By default this is null which
     * means the type will be determined by the build file extension.
     */
    private String m_type;
    private final ArrayList m_parameters = new ArrayList();

    /**
     * Specify whether should inherit properties in sub-build.
     *
     * @param inheritAll true to inherit else false
     */
    public void setInheritAll( final boolean inheritAll )
    {
        m_inheritAll = inheritAll;
    }

    /**
     * set the build file to process.
     *
     * @param file the build file
     */
    public void setFile( final File file )
    {
        m_file = file;
    }

    /**
     * set the type of build file.
     *
     * @param type the type of build file
     */
    public void setType( final String type )
    {
        m_type = type;
    }

    /**
     * set the target to process. If none is defined it will
     * execute the default target of the build file
     */
    public void setTarget( final String target )
    {
        m_target = target;
    }

    /**
     * Add a parameter to processing of build file.
     *
     * @param param the parameter
     */
    public void addParam( final AntParam param )
    {
        m_parameters.add( param );
    }

    /**
     * Execute the specified build, with specified parameters.
     *
     * @throws TaskException if an error occurs.
     */
    public void execute()
        throws TaskException
    {
        if( null == m_file )
        {
            m_file = getContext().resolveFile( DEFAULT_BUILD_FILE );
        }

        final Embeddor embeddor =
            (Embeddor)getContext().getService( Embeddor.class );

        try
        {
            final Project project =
                embeddor.createProject( m_file.toString(),
                                        m_type,
                                        new Parameters() );
            final Workspace workspace =
                embeddor.createWorkspace( buildParameters() );

            if( null == m_target )
            {
                m_target = project.getDefaultTargetName();
            }

            workspace.executeProject( project, m_target );
        }
        catch( final Exception e )
        {
            throw new TaskException( e.toString(), e );
        }
    }

    /**
     * Build the parameters to pass to sub-project.
     * These include the current tasks properties
     * (if inheritall=true) and any supplied by the user.
     *
     * @return the created parameters
     */
    private Parameters buildParameters()
        throws TaskException
    {
        final Parameters parameters = new Parameters();

        if( m_inheritAll )
        {
            final Map properties = getContext().getProperties();
            final Iterator keys = properties.keySet().iterator();
            while( keys.hasNext() )
            {
                final String key = (String)keys.next();
                final Object value = properties.get( key );
                setProperty( parameters, key, value );
            }
        }

        final int size = m_parameters.size();
        for( int i = 0; i < size; i++ )
        {
            final AntParam param = (AntParam)m_parameters.get( i );
            param.validate();
            final String name = param.getName();
            final String value = param.getValue().toString();
            setProperty( parameters, name, value );
        }

        return parameters;
    }

    /**
     * Utility method to add the property into parameters object.
     *
     * @param parameters where to put property
     * @param name the property
     * @param value the value of property
     * @todo allow non-string params to be passed down
     */
    private void setProperty( final Parameters parameters,
                              final String name,
                              final Object value )
    {
        if( !name.startsWith( "myrmidon." ) )
        {
            parameters.setParameter( name, value.toString() );
        }
    }
}
