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
 * An ordered list of files.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @ant:role shorthand="v-path"
 */
public interface FileList
    extends DataType
{
    /**
     * Returns the files in the list.
     *
     * @param context
     *      The context to use to build the list of files.
     *
     * @throws TaskException
     *      On error building the list of files.
     */
    FileObject[] listFiles( TaskContext context ) throws TaskException;
}
