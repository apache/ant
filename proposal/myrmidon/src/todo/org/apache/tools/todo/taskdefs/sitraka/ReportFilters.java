/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.sitraka;

import java.util.ArrayList;
import org.apache.tools.todo.util.regexp.RegexpMatcher;
import org.apache.tools.todo.util.regexp.RegexpMatcherFactory;

/**
 * Filters information from coverage, somewhat similar to a <tt>FileSet</tt> .
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class ReportFilters
{
    /**
     * user defined filters
     */
    private ArrayList filters = new ArrayList();

    /**
     * cached matcher for each filter
     */
    private ArrayList m_matchers;

    /**
     * Check whether a given &lt;classname&gt;&lt;method&gt;() is accepted by
     * the list of filters or not.
     *
     * @param methodname the full method name in the format
     *      &lt;classname&gt;&lt;method&gt;()
     * @return Description of the Returned Value
     */
    public boolean accept( String methodname )
    {
        // I'm deferring matcher instantiations at runtime to avoid computing
        // the filters at parsing time
        if( m_matchers == null )
        {
            createMatchers();
        }
        boolean result = false;
        // assert filters.size() == matchers.size()
        final int size = filters.size();
        for( int i = 0; i < size; i++ )
        {
            FilterElement filter = (FilterElement)filters.get( i );
            RegexpMatcher matcher = (RegexpMatcher)m_matchers.get( i );
            if( filter instanceof Include )
            {
                result = result || matcher.matches( methodname );
            }
            else if( filter instanceof Exclude )
            {
                result = result && !matcher.matches( methodname );
            }
            else
            {
                //not possible
                throw new IllegalArgumentException( "Invalid filter element: " + filter.getClass().getName() );
            }
        }
        return result;
    }

    public void addExclude( Exclude excl )
    {
        filters.add( excl );
    }

    public void addInclude( Include incl )
    {
        filters.add( incl );
    }

    public int size()
    {
        return filters.size();
    }

    /**
     * should be called only once to cache matchers
     */
    protected void createMatchers()
    {
        RegexpMatcherFactory factory = new RegexpMatcherFactory();
        final int size = filters.size();
        m_matchers = new ArrayList();
        for( int i = 0; i < size; i++ )
        {
            FilterElement filter = (FilterElement)filters.get( i );
            RegexpMatcher matcher = factory.newRegexpMatcher();
            String pattern = filter.getAsPattern();
            matcher.setPattern( pattern );
            m_matchers.add( matcher );
        }
    }
}

