/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

/**
 * Information about a file, that is used to select files during the
 * traversal of a hierarchy.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface FileSelectInfo
{
    /**
     * Returns the base folder of the traversal.
     */
    FileObject getBaseFolder();

    /**
     * Returns the file (or folder) to be considered.
     */
    FileObject getFile();

    /**
     * Returns the depth of the file relative to the base folder.
     */
    int getDepth();
}
