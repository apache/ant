/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider;

import java.io.File;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;

/**
 * A file system provider which handles local file systems.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface LocalFileSystemProvider
    extends FileSystemProvider
{
    /**
     * Determines if a name is an absolute file name.
     *
     * @todo Move this to a general file name parser interface.
     *
     * @param name The name to test.
     */
    boolean isAbsoluteLocalName( final String name );

    /**
     * Finds a local file, from its local name.
     */
    FileObject findLocalFile( final String name )
        throws FileSystemException;

    /**
     * Finds a local file.
     */
    FileObject findLocalFile( final File file )
        throws FileSystemException;
}
