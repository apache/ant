/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.deployer;

/**
 * A general-purpose type definition.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class TypeDefinition
{
    private final String m_name;
    private final String m_role;
    private final String m_classname;

    public TypeDefinition( final String name,
                           final String roleShorthand,
                           final String className )
    {
        m_name = name;
        m_role = roleShorthand;
        m_classname = className;
    }

    /**
     * Returns the type's implementation class name.
     */
    public final String getClassname()
    {
        return m_classname;
    }

    /**
     * Returns the type's role.
     */
    public final String getRole()
    {
        return m_role;
    }

    /**
     * Returns the type's name.
     */
    public String getName()
    {
        return m_name;
    }
}
