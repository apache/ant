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
 * An adaptor from a {@link FileSet} to a {@link FileList}.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class FileSetAdaptor
    implements FileList
{
    private final FileSet m_fileset;

    public FileSetAdaptor( final FileSet fileset )
    {
        m_fileset = fileset;
    }

    /**
     * Returns the files in the list.
     */
    public FileObject[] listFiles( TaskContext context )
        throws TaskException
    {
        final FileSetResult result = m_fileset.getResult( context );
        return result.getFiles();
    }
}
