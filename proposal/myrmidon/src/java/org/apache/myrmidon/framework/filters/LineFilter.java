/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.filters;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * Filters lines of text.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:role shorthand="line-filter"
 */
public interface LineFilter
{
    /**
     * Filters a line of text.
     *
     * @param line the text to filter.
     * @param context the context to use when filtering.
     */
    void filterLine( StringBuffer line, TaskContext context )
        throws TaskException;
}
