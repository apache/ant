/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

/**
 * This interface is used to select files when traversing a file hierarchy.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface FileSelector
{
    /**
     * Determines if a file or folder should be selected.
     *
     * @param fileInfo the file or folder to select.
     * @return true if the file should be selected.
     */
    boolean includeFile( FileSelectInfo fileInfo )
        throws FileSystemException;

    /**
     * Determines whether a folder should be traversed.  If this method returns
     * true, {@link #includeFile} is called for each of the children of
     * the folder, and each of the child folders is recursively traversed.
     *
     * @param fileInfo the file or folder to select.
     *
     * @return true if the folder should be traversed.
     */
    boolean traverseDescendents( FileSelectInfo fileInfo )
        throws FileSystemException;
}
