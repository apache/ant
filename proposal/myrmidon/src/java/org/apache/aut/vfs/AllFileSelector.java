/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

/**
 * A {@link FileSelector} which selects everything.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class AllFileSelector
    implements FileSelector
{
    /**
     * Determines if a file or folder should be selected.
     */
    public boolean includeFile( final FileSelectInfo fileInfo )
        throws FileSystemException
    {
        return true;
    }

    /**
     * Determines whether a folder should be traversed.
     */
    public boolean traverseDescendents( final FileSelectInfo fileInfo )
        throws FileSystemException
    {
        return true;
    }
}
