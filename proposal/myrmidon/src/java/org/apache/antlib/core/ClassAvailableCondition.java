/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.conditions.Condition;

/**
 * A condition that evaluates to true if the requested class is available
 * at runtime.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 *
 * @ant:type type="condition" name="class-available"
 */
public class ClassAvailableCondition
    extends AbstractAvailableCondition
    implements Condition
{
    private String m_classname;

    /**
     * Sets the name of the class to search for.
     */
    public void setClassname( final String classname )
    {
        m_classname = classname;
    }

    /**
     * Evaluates the condition.
     */
    public boolean evaluate( final TaskContext context )
        throws TaskException
    {
        if( m_classname == null )
        {
            throw new TaskException( "Classname not specified." );
        }

        // Build the classloader to use to check resources
        final ClassLoader classLoader = buildClassLoader();

        // Do the check
        try
        {
            classLoader.loadClass( m_classname );
            return true;
        }
        catch( final Exception e )
        {
            return false;
        }
    }

}
