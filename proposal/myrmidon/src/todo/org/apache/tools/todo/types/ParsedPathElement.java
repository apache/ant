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
import org.apache.tools.todo.util.FileUtils;

/**
 * A PathElement that is parsed from a string.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class ParsedPathElement
    implements FileList
{
    private final String m_path;

    public ParsedPathElement( final String path )
    {
        m_path = path;
    }

    public String[] listFiles( final TaskContext context )
        throws TaskException
    {
        return FileUtils.translatePath( context.getBaseDirectory(), m_path );
    }
}
