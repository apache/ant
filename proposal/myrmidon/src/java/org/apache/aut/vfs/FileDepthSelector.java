/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

/**
 * A {@link FileSelector} which selects all files in a particular depth range.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class FileDepthSelector
    implements FileSelector
{
    private final int m_minDepth;
    private final int m_maxDepth;

    public FileDepthSelector( int minDepth, int maxDepth )
    {
        m_minDepth = minDepth;
        m_maxDepth = maxDepth;
    }

    /**
     * Determines if a file or folder should be selected.
     */
    public boolean includeFile( final FileSelectInfo fileInfo )
        throws FileSystemException
    {
        final int depth = fileInfo.getDepth();
        return m_minDepth <= depth && depth <= m_maxDepth;
    }

    /**
     * Determines whether a folder should be traversed.
     */
    public boolean traverseDescendents( final FileSelectInfo fileInfo )
        throws FileSystemException
    {
        return fileInfo.getDepth() < m_maxDepth;
    }
}
