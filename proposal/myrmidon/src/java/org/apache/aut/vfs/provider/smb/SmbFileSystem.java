/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.smb;

import org.apache.aut.vfs.FileName;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.AbstractFileSystem;
import org.apache.aut.vfs.provider.FileSystem;

/**
 * A SMB file system.
 *
 * @author Adam Murdoch
 */
public class SmbFileSystem extends AbstractFileSystem implements FileSystem
{
    public SmbFileSystem( FileName rootName )
    {
        super( rootName );
    }

    /**
     * Creates a file object.
     */
    protected FileObject createFile( FileName name ) throws FileSystemException
    {
        String fileName = name.getURI();
        return new SmbFileObject( fileName, name, this );
    }
}
