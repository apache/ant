/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.types;// java io classes

// java util classes

import java.util.Enumeration;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;

// ant classes

/**
 * A FilterSetCollection is a collection of filtersets each of which may have a
 * different start/end token settings.
 *
 * @author <A href="mailto:conor@apache.org">Conor MacNeill</A>
 */
public class FilterSetCollection
{

    private Vector filterSets = new Vector();

    public FilterSetCollection()
    {
    }

    public FilterSetCollection( FilterSet filterSet )
    {
        addFilterSet( filterSet );
    }

    public void addFilterSet( FilterSet filterSet )
    {
        filterSets.addElement( filterSet );
    }

    /**
     * Test to see if this filter set it empty.
     *
     * @return Return true if there are filter in this set otherwise false.
     */
    public boolean hasFilters()
        throws TaskException
    {
        for( Enumeration e = filterSets.elements(); e.hasMoreElements(); )
        {
            FilterSet filterSet = (FilterSet)e.nextElement();
            if( filterSet.hasFilters() )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Does replacement on the given string with token matching. This uses the
     * defined begintoken and endtoken values which default to @ for both.
     *
     * @param line The line to process the tokens in.
     * @return The string with the tokens replaced.
     */
    public String replaceTokens( String line )
        throws TaskException
    {
        String replacedLine = line;
        for( Enumeration e = filterSets.elements(); e.hasMoreElements(); )
        {
            FilterSet filterSet = (FilterSet)e.nextElement();
            replacedLine = filterSet.replaceTokens( replacedLine );
        }
        return replacedLine;
    }
}


