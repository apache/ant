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
import org.apache.myrmidon.framework.DataType;

/**
 * Accepts files as part of a set.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:role shorthand="v-file-selector"
 */
public interface FileSelector
    extends DataType
{
    /**
     * Accepts a file.
     *
     * @param path The virtual path associated with the file.  May be null
     *             if such a path is not available.
     * @param file The file to select.
     * @param context The context to perform the selection in.
     */
    boolean accept( FileObject file, String path, TaskContext context )
        throws TaskException;
}
