/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.types;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A PathElement made up of an array of strings.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class ArrayFileList
    implements FileList
{
    private final String[] m_parts;

    public ArrayFileList( final String part )
    {
        m_parts = new String[] { part } ;
    }

    public ArrayFileList( final String[] parts )
    {
        m_parts = parts;
    }

    public String[] listFiles( final TaskContext context )
        throws TaskException
    {
        return m_parts;
    }
}
