/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.conditions.Condition;
import org.apache.myrmidon.framework.file.Path;
import org.apache.tools.todo.types.PathUtil;

/**
 * An abstract condition which checks for the availability of a particular
 * resource in a classpath.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractAvailableCondition
    implements Condition
{
    private Path m_classpath = new Path();

    /**
     * Adds a classpath element.
     */
    public void setClasspath( final Path classpath )
    {
        m_classpath.add( classpath );
    }

    /**
     * Adds a classpath element.
     */
    public void addClasspath( final Path classpath )
    {
        m_classpath.add( classpath );
    }

    /**
     * Builds the ClassLoader to use to check resources.
     */
    protected ClassLoader buildClassLoader( final TaskContext context ) throws TaskException
    {
        return PathUtil.createClassLoader( m_classpath, context );
    }
}
