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
import org.apache.myrmidon.framework.FileNameMapper;

/**
 * A filename mapper that applies a prefix to each file.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:type type="mapper" name="prefix"
 */
public class PrefixFileNameMapper
    implements FileNameMapper
{
    private String m_prefix;

    /**
     * Sets the prefix.
     */
    public void setPrefix( final String prefix )
    {
        m_prefix = prefix;
        if( ! m_prefix.endsWith( "/" ) )
        {
            m_prefix = m_prefix + '/';
        }
    }

    /**
     * Returns an array containing the target filename(s) for the given source
     * file.
     */
    public String[] mapFileName( final String sourceFileName,
                                 final TaskContext context )
        throws TaskException
    {
        if( m_prefix == null )
        {
            return new String[]{ sourceFileName };
        }
        else
        {
            return new String[] { m_prefix + sourceFileName };
        }
    }
}
