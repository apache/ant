/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.FilterSet;

/**
 * A FilterSetCollection is a collection of filtersets each of which may have a
 * different start/end token settings.
 *
 * @author <A href="mailto:conor@apache.org">Conor MacNeill</A>
 */
public class FilterSetCollection
{
    private ArrayList m_filterSets = new ArrayList();

    public void addFilterSet( final FilterSet filterSet )
    {
        m_filterSets.add( filterSet );
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

        final Iterator filterSets = m_filterSets.iterator();
        while( filterSets.hasNext() )
        {
            final FilterSet filterSet = (FilterSet)filterSets.next();
            replacedLine = filterSet.replaceTokens( replacedLine );
        }
        return replacedLine;
    }
}


