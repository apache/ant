/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.embeddor.Embeddor;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.workspace.Workspace;

/**
 * Abstract base class for Tasks which execute targets.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractAntTask
    extends AbstractTask
{
    /**
     * If true, inherit all properties from parent Project
     * If false, inherit only userProperties and those defined
     * inside the ant call itself
     */
    private boolean m_inheritAll;

    /**
     * The target to process in build file. If not specified
     * will use default in specified build file.
     */
    private String m_target;

    /**
     * The parameters/properties which will be passed to the workspace
     * for the target execution.
     */
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
        try
        {
            Project project = getProject();

            Embeddor embeddor = getEmbeddor();

            final Workspace workspace =
                embeddor.createWorkspace( buildParameters() );

            // TODO - inherit listeners, and services (TypeManager specifically)
            workspace.addProjectListener( embeddor.createListener("default"));

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
     * A convenience method for obtaining the Embeddor from the
     * TaskContext.
     * @return The Embeddor contained in the TaskContext
     * @throws TaskException if the Embeddor could not be obtained.
     */
    protected Embeddor getEmbeddor() throws TaskException
    {
        final Embeddor embeddor =
            (Embeddor)getContext().getService( Embeddor.class );
        return embeddor;
    }

    /**
     * Get/create/build the project containing the target to be executed.
     * Subclasses will override this method to provide different means
     * of obtaining a project to execute.
     * @return The project containing the target to execute.
     * @throws Exception If a problem occurred building the project.
     */
    protected abstract Project getProject() throws Exception;

    /**
     * Build the parameters to pass to sub-project.
     * These include the current tasks properties
     * (if inheritall=true) and any supplied by the user.
     *
     * @return the created parameters
     */
    private Map buildParameters()
        throws TaskException
    {
        final Map parameters = new HashMap();

        if( m_inheritAll )
        {
            parameters.putAll( getContext().getProperties() );
        }

        final int size = m_parameters.size();
        for( int i = 0; i < size; i++ )
        {
            final AntParam param = (AntParam)m_parameters.get( i );
            param.validate();
            final String name = param.getName();
            final Object value = param.getValue();
            parameters.put( name, value );
        }

        return parameters;
    }
}
