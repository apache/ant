/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.converter;

/**
 * The context in which objects can be converted from one type to another type.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant:role shorthand="converter"
 */
public interface ConverterContext
{
    /**
     * Retrieve a vlaue from the context with the specified key.
     * Will return null if no such value exists.
     */
    Object get( Object key );
}
