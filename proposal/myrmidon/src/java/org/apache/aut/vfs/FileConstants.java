/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

/**
 * Several constants for use in the VFS API.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface FileConstants
{
    /**
     * A {@link FileSelector} which selects only the base file/folder.
     */
    FileSelector SELECT_SELF = new FileDepthSelector( 0, 0 );

    /**
     * A {@link FileSelector} which selects the base file/folder and its
     * direct children.
     */
    FileSelector SELECT_SELF_AND_CHILDREN = new FileDepthSelector( 0, 1 );

    /**
     * A {@link FileSelector} which selects only the direct children
     * of the base folder.
     */
    FileSelector SELECT_CHILDREN = new FileDepthSelector( 1, 1 );

    /**
     * A {@link FileSelector} which selects all the descendents of the
     * base folder, but does not select the base folder itself.
     */
    FileSelector EXCLUDE_SELF = new FileDepthSelector( 1, Integer.MAX_VALUE );

    /**
     * A {@link FileSelector} which selects the base file/folder, plus all
     * its descendents.
     */
    FileSelector SELECT_ALL = new AllFileSelector();
}
