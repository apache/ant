/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.myrmidon.framework.conditions.Condition;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.Path;
import org.apache.tools.todo.types.PathUtil;
import java.net.URL;
import java.net.URLClassLoader;

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
    public void addClasspath( final Path classpath )
        throws TaskException
    {
        m_classpath.addPath( classpath );
    }

    /**
     * Builds the ClassLoader to use to check resources.
     */
    protected ClassLoader buildClassLoader() throws TaskException
    {
        final URL[] urls = PathUtil.toURLs( m_classpath );
        final ClassLoader classLoader;
        if( urls.length > 0 )
        {
            classLoader = new URLClassLoader( urls );
        }
        else
        {
            // TODO - using system classloader is kinda useless now, because
            // the system classpath contains almost nothing.  Should be using
            // the 'common' classloader instead
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return classLoader;
    }
}
