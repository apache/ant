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
 * A file list that contains a single file.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @ant:data-type name="v-file"
 */
public class SingletonFileList implements FileList
{
    private FileObject m_file;

    /**
     * Sets the file to use for tils file list.
     */
    public void setFile( final FileObject file )
    {
        m_file = file;
    }

    /**
     * Returns the list of files.
     */
    public FileObject[] listFiles( TaskContext context ) throws TaskException
    {
        return new FileObject[]{m_file};
    }
}
