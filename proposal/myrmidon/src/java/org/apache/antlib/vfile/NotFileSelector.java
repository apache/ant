/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import org.apache.aut.vfs.FileObject;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A file selector that negates a nested file selector.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:data-type name="not-selector"
 * @ant:type type="v-file-selector" name="not"
 */
public class NotFileSelector
    implements FileSelector
{
    private FileSelector m_selector;

    /**
     * Sets the nested selector.
     */
    public void set( final FileSelector selector )
    {
        m_selector = selector;
    }

    /**
     * Accepts a file.
     */
    public boolean accept( final FileObject file,
                           final String path,
                           final TaskContext context )
        throws TaskException
    {
        if( m_selector == null )
        {
            throw new TaskException( "notfileselector.no-selector.error" );
        }
        return ! m_selector.accept( file, path, context );
    }
}
