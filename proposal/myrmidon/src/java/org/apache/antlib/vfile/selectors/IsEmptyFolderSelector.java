/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile.selectors;

import org.apache.antlib.vfile.FileSelector;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.FileType;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A file selector that selects empty directories.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:type type="v-file-selector" name="is-empty-folder"
 */
public class IsEmptyFolderSelector
    implements FileSelector
{
    /**
     * Accepts a file.
     */
    public boolean accept( final FileObject file,
                           final String path,
                           final TaskContext context )
        throws TaskException
    {
        try
        {
            return ( file.exists()
                && file.getType() == FileType.FOLDER
                && file.getChildren().length == 0 );
        }
        catch( FileSystemException e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }
}
