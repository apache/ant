/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider;

import org.apache.aut.vfs.FileName;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;

/**
 * A file system.
 *
 * @author Adam Murdoch
 */
public interface FileSystem
{
    /**
     * Returns the root of this file system.
     */
    FileObject getRoot() throws FileSystemException;

    /**
     * Finds a file in this file system.
     *
     * @param name
     *          The name of the file.
     */
    FileObject findFile( FileName name ) throws FileSystemException;

    /**
     * Finds a file in this file system.
     *
     * @param name
     *          The name of the file.  This must be an absolute path.
     */
    FileObject findFile( String name ) throws FileSystemException;
}
