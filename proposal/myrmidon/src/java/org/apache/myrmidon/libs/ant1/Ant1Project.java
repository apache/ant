/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.libs.ant1;

import java.io.File;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.*;
import org.apache.tools.ant.Project;

public class Ant1Project
    extends Project
    implements LogEnabled, Contextualizable
{
    private Logger   m_logger;

    ///Variable to hold context for use by sub-classes
    private TaskContext            m_context;

    public void enableLogging( final Logger logger )
    {
        m_logger = logger;
    }

    protected final Logger getLogger()
    {
        return m_logger;
    }

    /**
     * Retrieve context from container.
     *
     * @param context the context
     */
    public void contextualize( final Context context )
    {
        m_context = (TaskContext)context;
    }

    protected final TaskContext getContext()
    {
        return m_context;
    }

    /**
     * Initialise the project.
     */
    public void init()
        throws TaskException
    {
        setJavaVersionProperty();
    }

    public void setProperty( final String name, final String value )
    {
        try { getContext().setProperty( name, value ); }
        catch( final Exception e )
        {
            getLogger().warn( "Failed to set property " + name + " to " + value, e );
        }
    }

    public void setUserProperty( final String name, final String value )
    {
        setProperty( name, value );
    }

    public String getProperty( final String name )
    {
        return "" + getContext().getProperty( name );
    }

    public String getUserProperty( final String name )
    {
        return getProperty( name );
    }

    public String getName()
    {
        return "Ant1 Project";
    }

    public Task createTask( final String taskType )
        throws TaskException
    {
        throw new UnsupportedOperationException();
    }

    public Object createDataType( final String typeName )
        throws TaskException
    {
        throw new UnsupportedOperationException();
    }

    public File resolveFile( final String fileName )
    {
        try { return getContext().resolveFile( fileName ); }
        catch( final Exception e )
        {
            return null;
        }
    }

    protected void fireBuildStarted() {}
    protected void fireBuildFinished(Throwable exception) {}
    protected void fireTargetStarted(Target target) {}
    protected void fireTargetFinished(Target target, Throwable exception) {}
    protected void fireTaskStarted(Task task) {}
    protected void fireTaskFinished(Task task, Throwable exception) {}

    private void fireMessageLoggedEvent(BuildEvent event, String message, int priority)
    {
        messageLogged( message, priority );
    }

    protected void fireMessageLogged(Project project, String message, int priority)
    {
        messageLogged( message, priority );
    }

    protected void fireMessageLogged(Target target, String message, int priority)
    {
        messageLogged( message, priority );
    }

    protected void fireMessageLogged(Task task, String message, int priority)
    {
        messageLogged( message, priority );
    }

    private void messageLogged( String message, int priority )
    {
        switch( priority )
        {
        case MSG_ERR: getLogger().error( message ); break;
        case MSG_WARN: getLogger().warn( message ); break;
        case MSG_INFO: getLogger().info( message ); break;
        case MSG_VERBOSE: getLogger().debug( message ); break;
        case MSG_DEBUG: getLogger().debug( message ); break;

        default:
            getLogger().debug( message );
        }
    }
}
