/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.io.InputStream;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.conditions.Condition;

/**
 * A condition that evaluates to true if the requested resource is available
 * at runtime.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 *
 * @ant:type type="condition" name="resource-available"
 */
public class ResourceAvailableCondition
    extends AbstractAvailableCondition
    implements Condition
{
    private String m_resource;

    /**
     * Sets the name of the resource to look for.
     */
    public void setResource( final String resource )
    {
        m_resource = resource;
    }

    /**
     * Evaluates the condition.
     */
    public boolean evaluate( final TaskContext context )
        throws TaskException
    {
        if( m_resource == null )
        {
            throw new TaskException( "Resource was not specified." );
        }

        // Check whether the resource is available
        final ClassLoader classLoader = buildClassLoader();
        final InputStream instr = classLoader.getResourceAsStream( m_resource );
        if( instr != null )
        {
            IOUtil.shutdownStream( instr );
            return true;
        }
        return false;
    }
}
