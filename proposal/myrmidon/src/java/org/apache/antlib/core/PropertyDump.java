/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.util.Iterator;
import java.util.Map;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * This is a simple task used to dump out all the proeprtys in the
 * runtime. Useful for debugging behaviour in ant build directives.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jimcook@visualxs.com">Jim Cook</a>
 * @version $Revision$ $Date$
 * @ant.task name="property-dump"
 * @todo Consider moving to new antlib
 */
public class PropertyDump
    extends AbstractTask
{
    /**
     * The prefix which the keys must start with if they are
     * to be dumped.
     */
    private String m_prefix;

    /**
     * Set the prefix which the keys must start with if they are
     * to be dumped. If not specified then all keys are dumped.
     *
     * @param prefix the prefix
     */
    public void setPrefix( final String prefix )
    {
        m_prefix = prefix;
    }

    /**
     * Printout all the properties in ant runtime.
     */
    public void execute()
        throws TaskException
    {
        final Map properties = getContext().getProperties();
        final Iterator iterator = properties.keySet().iterator();
        while( iterator.hasNext() )
        {
            final String key = (String)iterator.next();
            final Object value = properties.get( key );

            //Check to see if property starts with specified prefix
            //and if it doesn't then skip property
            if( null != m_prefix && !key.startsWith( m_prefix ) )
            {
                continue;
            }

            getContext().info( key + "=" + value );
        }
    }
}
