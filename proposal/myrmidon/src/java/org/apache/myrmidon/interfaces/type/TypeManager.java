/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.type;

import org.apache.avalon.framework.component.Component;

/**
 * The interface that is used to manage types.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public interface TypeManager
    extends Component
{
    String ROLE = "org.apache.myrmidon.interfaces.type.TypeManager";

    /**
     * Registers a new type.
     *
     * @param roleType
     *      The role interface for the type.  Objects created by the factory
     *      must implement this interface.
     *
     * @param shorthandName
     *      The shorthand name for the type.
     *
     * @param factory
     *      The type factory.
     */
    void registerType( Class roleType, String shorthandName, TypeFactory factory )
        throws TypeException;

    /**
     * Returns the factory for a role.
     */
    TypeFactory getFactory( Class roleType )
        throws TypeException;

    /**
     * Creates a child type manager.  The child inherits the type factories
     * from this type manager.  Additional type factories may be added to the
     * child, without affecting this type manager.
     */
    TypeManager createChildTypeManager();
}
