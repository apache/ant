/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.DataType;

/**
 * A set of files, where each file in the list has a virtual path associated
 * with it.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @ant:role shorthand="v-fileset"
 */
public interface FileSet
    extends DataType
{
    /**
     * Returns the contents of the set.
     *
     * @param context
     *      The context to use to build the set.
     *
     * @throws TaskException
     *      On error building the set.
     */
    FileSetResult getResult( TaskContext context ) throws TaskException;
}
