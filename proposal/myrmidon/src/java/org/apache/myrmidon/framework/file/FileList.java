/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.file;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A list of files.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface FileList
{
    /**
     * Returns the files in this list.
     *
     * @param context the context to use to evaluate the list.
     * @return The names of the files in this list.  All names are absolute paths.
     */
    public String[] listFiles( TaskContext context )
        throws TaskException;
}
