/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.type;

/**
 * The interface that is used to manage types.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface TypeManager
{
    String ROLE = TypeManager.class.getName();

    /**
     * Registers a new type.
     *
     * @param roleName The role for the type.
     * @param shorthandName The shorthand name for the type.
     * @param factory The type factory.
     */
    void registerType( String roleName, String shorthandName, TypeFactory factory )
        throws TypeException;

    /**
     * Returns the factory for a role.
     *
     * @param roleName The role for the type.
     */
    TypeFactory getFactory( String roleName )
        throws TypeException;

    /**
     * Creates a child type manager.  The child inherits the type factories
     * from this type manager.  Additional type factories may be added to the
     * child, without affecting this type manager.
     */
    TypeManager createChildTypeManager();
}
