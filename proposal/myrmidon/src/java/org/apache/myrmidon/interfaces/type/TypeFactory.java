/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.type;

/**
 * Create an instance on name.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version CVS $Revision$ $Date$
 */
public interface TypeFactory
{
    /**
     * Determines if this factory can create instances of a particular type.
     *
     * @param name the type name.
     * @return <code>true</code> if this is a valid factory for the named type.
     */
    boolean canCreate( String name );

    /**
     * Create a type instance based on name.
     *
     * @param name the type name
     * @return the type instance
     * @exception TypeException if the type is unknown, or an error occurs.
     */
    Object create( String name )
        throws TypeException;
}
