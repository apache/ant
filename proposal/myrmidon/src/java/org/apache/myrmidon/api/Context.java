/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.api;

import java.util.Map;

/**
 * A context - a set of named properties.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface Context
{
    /**
     * Resolve a value according to the context.
     * This involves evaluating the string and replacing
     * ${} sequences with property values.
     *
     * @param value the value to resolve
     * @return the resolved value
     */
    Object resolveValue( String value )
        throws TaskException;

    /**
     * Retrieve property for name.
     *
     * @param name the name of property
     * @return the value of property, or null if the property has no value.
     */
    Object getProperty( String name );

    /**
     * Retrieve a copy of all the properties accessible via context.
     *
     * @return the map of all property names to values
     */
    Map getProperties();

}
