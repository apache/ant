/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.model;

/**
 * Imports in a build file.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class TypeLib
{
    //Name of library (this is location independent)
    private final String    m_library;

    //Do we need this??
    //private final String    m_namespace;

    //The role of object to be imported
    private final String    m_role;

    //The name of type instance
    private final String    m_name;

    public TypeLib( final String library )
    {
        this( library, null, null );
    }

    public TypeLib( final String library, final String role, final String name )
    {
        m_library = library;
        m_role = role;
        m_name = name;
       
        //If only one of name or type is null, throw an exception
        if( null == m_role || null == m_name )
        {
            if( null != m_role || null != m_name )
            {
                throw new IllegalArgumentException( "Can not have an import that specifies " +
                                                    "only one of; name or role" );
            }
        }
    }

    /**
     * Get role
     *
     * @return the role
     */
    public final String getRole()
    {
        return m_role;
    }

    /**
     * Get name of imported 
     *
     * @return the name
     */
    public final String getName()
    {
        return m_name;
    }

    /**
     * Get name of library 
     *
     * @return the library name
     */
    public final String getLibrary()
    {
        return m_library;
    }
}


