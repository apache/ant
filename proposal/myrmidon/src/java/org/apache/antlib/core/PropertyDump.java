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

/**
 * This is a simple task used to dump out all the proeprtys in the
 * runtime. Useful for debugging behaviour in ant build directives.
 * Could possibly be moved to a new antlib.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jimcook@visualxs.com">Jim Cook</a>
 * @version $Revision$ $Date$
 * @ant:task name="property-dump"
 */
public class PropertyDump
    extends AbstractTask
{
    /**
     * Printout all the properties in ant runtime.
     */
    public void execute()
    {
        final Map properties = getContext().getProperties();
        final Iterator iterator = properties.keySet().iterator();
        while( iterator.hasNext() )
        {
            final String key = (String)iterator.next();
            final Object value = properties.get( key );
            getContext().warn( key + "=" + value );
        }
    }
}
