/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.AbstractContainerTask;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.executor.Executor;

/**
 * A task that emulates the try-catch-finally construct in a number
 * of languages.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant:task name="try-catch"
 */
public final class TryCatchTask
    extends AbstractContainerTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( TryCatchTask.class );

    private TaskList m_try;
    private TaskList m_catch;
    private TaskList m_finally;

    public void addTry( final TaskList taskList )
        throws TaskException
    {
        if( null != m_try )
        {
            final String message = REZ.getString( "trycatch.multiple-trys.error" );
            throw new TaskException( message );
        }
        m_try = taskList;
    }

    public void addCatch( final TaskList taskList )
        throws TaskException
    {
        if( null == m_try )
        {
            final String message = REZ.getString( "trycatch.missing-try-before-catch.error" );
            throw new TaskException( message );
        }
        else if( null != m_catch )
        {
            final String message = REZ.getString( "trycatch.multiple-catches.error" );
            throw new TaskException( message );
        }
        m_catch = taskList;
    }

    public void addFinally( final TaskList taskList )
        throws TaskException
    {
        if( null == m_try )
        {
            final String message = REZ.getString( "trycatch.missing-try-before-finally.error" );
            throw new TaskException( message );
        }
        else if( null != m_finally )
        {
            final String message = REZ.getString( "trycatch.multiple-finallys.error" );
            throw new TaskException( message );
        }
        m_finally = taskList;
    }

    public void execute()
        throws TaskException
    {
        validate();

        final ExecutionFrame frame = (ExecutionFrame)getService( ExecutionFrame.class );
        final Executor executor = (Executor)getService( Executor.class );

        try
        {
            final Configuration[] tasks = m_try.getTasks();
            executeTasks( executor, frame, tasks );
        }
        catch( final TaskException te )
        {
            if( null != m_catch )
            {
                final Configuration[] tasks = m_catch.getTasks();
                executeTasks( executor, frame, tasks );
            }
            else
            {
                throw te;
            }
        }
        finally
        {
            if( null != m_finally )
            {
                final Configuration[] tasks = m_finally.getTasks();
                executeTasks( executor, frame, tasks );
            }
        }
    }

    private void validate()
        throws TaskException
    {
        if( null == m_try )
        {
            final String message = REZ.getString( "trycatch.no-try.error" );
            throw new TaskException( message );
        }
        else if( null == m_catch && null == m_finally )
        {
            final String message = REZ.getString( "trycatch.missing-second.error" );
            throw new TaskException( message );
        }
    }

    /**
     * Utility method to execute the tasks in an appropriate environment.
     */
    private void executeTasks( final Executor executor,
                               final ExecutionFrame frame,
                               final Configuration[] tasks )
        throws TaskException
    {
        for( int i = 0; i < tasks.length; i++ )
        {
            final Configuration task = tasks[ i ];
            executor.execute( task, frame );
        }
    }

    public String toString()
    {
        return "Try-Catch-Finally";
    }
}
