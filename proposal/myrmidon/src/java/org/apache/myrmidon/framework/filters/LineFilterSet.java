/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.filters;

import java.util.ArrayList;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A collection of line filters.
 *
 * @ant:data-type name="filterset"
 * @ant:type type="line-filter" name="filterset"
 */
public class LineFilterSet
    implements LineFilter
{
    private ArrayList m_filterSets = new ArrayList();

    public void add( final LineFilter filter )
    {
        m_filterSets.add( filter );
    }

    /**
     * Filters a line of text.
     *
     * @param line the text to filter.
     * @param context the context to use when filtering.
     */
    public void filterLine( final StringBuffer line, final TaskContext context )
        throws TaskException
    {
        final int count = m_filterSets.size();
        for( int i = 0; i < count; i++ )
        {
            final LineFilter filter = (LineFilter)m_filterSets.get( i );
            filter.filterLine( line, context );
        }
    }
}


