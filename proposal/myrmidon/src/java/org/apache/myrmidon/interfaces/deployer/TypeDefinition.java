/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.deployer;

/**
 * A basic type definition.  This class is used to build a type definition,
 * from a typelib descriptor, or via introspection.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public abstract class TypeDefinition
{
    private String m_className;

    /**
     * Returns the type's name.
     */
    public abstract String getName();

    /**
     * Returns the type's role.
     */
    public abstract String getRoleShorthand();

    /**
     * Returns the type's implementation class name.
     */
    public String getClassname()
    {
        return m_className;
    }

    /**
     * Sets the type's implementation class name.
     */
    public void setClassname( final String className )
    {
        m_className = className;
    }
}
