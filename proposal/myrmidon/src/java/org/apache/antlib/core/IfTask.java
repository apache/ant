/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.util.ArrayList;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.AbstractContainerTask;
import org.apache.myrmidon.framework.conditions.Condition;
import org.apache.myrmidon.framework.conditions.IsSetCondition;
import org.apache.myrmidon.framework.conditions.NotCondition;

/**
 * A simple task to test a supplied condition. If the condition is true
 * then it will execute the inner tasks, else it won't.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant.task name="if"
 */
public class IfTask
    extends AbstractContainerTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( IfTask.class );

    private Condition m_condition;
    private ArrayList m_tasks = new ArrayList();

    /**
     * Set if clause on pattern.
     *
     * @param condition the condition
     * @exception TaskException if an error occurs
     */
    public void setTest( final String condition )
        throws TaskException
    {
        verifyConditionNull();
        m_condition = new IsSetCondition( condition );
    }

    /**
     * Set unless clause of pattern.
     *
     * @param condition the unless clause
     * @exception TaskException if an error occurs
     */
    public void setNotTest( final String condition )
        throws TaskException
    {
        verifyConditionNull();
        m_condition = new NotCondition( new IsSetCondition( condition ) );
    }

    public void add( final Configuration task )
    {
        m_tasks.add( task );
    }

    public void execute()
        throws TaskException
    {
        if( null == m_condition )
        {
            final String message = REZ.getString( "if.no-condition.error" );
            throw new TaskException( message );
        }

        // Evaluate the condition
        if( !m_condition.evaluate( getContext() ) )
        {
            return;
        }

        final Configuration[] tasks =
            (Configuration[])m_tasks.toArray( new Configuration[ m_tasks.size() ] );

        executeTasks( tasks );
    }

    public String toString()
    {
        return "If['" + m_condition + "]";
    }

    /**
     * Utility method to make sure condition unset.
     * Made so that it is not possible for both if and unless to be set.
     *
     * @exception TaskException if an error occurs
     */
    private void verifyConditionNull()
        throws TaskException
    {
        if( null != m_condition )
        {
            final String message = REZ.getString( "if.ifelse-duplicate.error" );
            throw new TaskException( message );
        }
    }
}
