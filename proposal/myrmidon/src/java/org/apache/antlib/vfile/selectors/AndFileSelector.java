/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile.selectors;

import java.util.ArrayList;
import org.apache.antlib.vfile.FileSelector;
import org.apache.aut.vfs.FileObject;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A file selector that performs an AND of nested selectors.  Performs
 * lazy evaluation.  Returns true when no nested elements are supplied.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:data-type name="and-selector"
 * @ant:type type="v-file-selector" name="and"
 */
public class AndFileSelector
    implements FileSelector
{
    private final ArrayList m_selectors = new ArrayList();

    /**
     * Adds a nested selector.
     */
    public void add( final FileSelector selector )
    {
        m_selectors.add( selector );
    }

    /**
     * Accepts a file.
     */
    public boolean accept( final FileObject file,
                           final String path,
                           final TaskContext context )
        throws TaskException
    {
        for( int i = 0; i < m_selectors.size(); i++ )
        {
            final FileSelector fileSelector = (FileSelector)m_selectors.get( i );
            if( !fileSelector.accept( file, path, context ) )
            {
                return false;
            }
        }

        return true;
    }
}
