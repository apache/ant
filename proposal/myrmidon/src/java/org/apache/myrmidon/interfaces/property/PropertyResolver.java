/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.property;

import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;

/**
 *
 * Provides a service for the resolution of property identifiers within
 * String content.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public interface PropertyResolver
{
    String ROLE = PropertyResolver.class.getName();

    /**
     * Resolve a string property. This evaluates all property
     * substitutions based on specified contex.
     * Rules used for property resolution are implementation dependent.
     *
     * @param value the value to resolve, which may contain property identifiers
     * @param context the set of properties to resolve against.
     * @return the resolved content
     * @exception TaskException if an error occurs
     */
    Object resolveProperties( final String value,
                              final TaskContext context )
        throws TaskException;
}
