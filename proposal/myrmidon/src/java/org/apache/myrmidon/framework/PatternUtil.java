/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 *
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class PatternUtil
{
    /**
     * Returns the filtered include patterns.
     */
    public static String[] getIncludePatterns( final PatternSet set,
                                               final TaskContext context )
        throws TaskException
    {
        return toArray( set.getIncludes(), context );
    }

    public static String[] getExcludePatterns( final PatternSet set,
                                               final TaskContext context )
        throws TaskException
    {
        return toArray( set.getExcludes(), context );
    }

    /**
     * Convert a vector of Pattern elements into an array of Strings.
     */
    private static String[] toArray( final ArrayList list, final TaskContext context )
    {
        if( list.size() == 0 )
        {
            return null;
        }

        final ArrayList names = new ArrayList();
        final Iterator patterns = list.iterator();
        while( patterns.hasNext() )
        {
            final Pattern pattern = (Pattern)patterns.next();
            final String result = pattern.evaluateName( context );
            if( null != result && result.length() > 0 )
            {
                names.add( result );
            }
        }

        return (String[])names.toArray( new String[ names.size() ] );
    }
}
