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
public class GeneralTypeDefinition
    extends TypeDefinition
{
    private String m_name;
    private String m_roleShorthand;

    /**
     * Returns the type's role.
     */
    public String getRoleShorthand()
    {
        return m_roleShorthand;
    }

    /**
     * Sets the type's role.
     */
    public void setType( String roleShorthand )
    {
        m_roleShorthand = roleShorthand;
    }

    /**
     * Returns the type's name.
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Sets the type's name.
     */
    public void setName( String name )
    {
        m_name = name;
    }
}
