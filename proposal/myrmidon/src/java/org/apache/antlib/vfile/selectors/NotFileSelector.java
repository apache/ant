/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile.selectors;

import org.apache.aut.vfs.FileObject;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A file selector that negates a nested file selector.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="v-file-selector" name="not"
 */
public class NotFileSelector
    implements FileSelector
{
    private static final Resources REZ
        = ResourceManager.getPackageResources( NotFileSelector.class );

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
            final String message = REZ.getString( "notfileselector.no-selector.error" );
            throw new TaskException( message );
        }
        return !m_selector.accept( file, path, context );
    }
}
