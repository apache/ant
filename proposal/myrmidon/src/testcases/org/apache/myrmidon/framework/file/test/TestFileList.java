/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.file.test;

import org.apache.myrmidon.framework.file.FileList;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import java.io.File;

/**
 * A test FileList implementation.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:type type="path" name="test-file-list"
 */
public class TestFileList
    implements FileList
{
    private String m_name;

    public void setName( final String name )
    {
        m_name = name;
    }

    /**
     * Returns the files in this list.
     */
    public String[] listFiles( final TaskContext context )
        throws TaskException
    {
        final File file = context.resolveFile( m_name );
        return new String[] { file.getAbsolutePath() };
    }
}
