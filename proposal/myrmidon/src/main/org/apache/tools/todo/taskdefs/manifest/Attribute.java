/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.manifest;

/**
 * Class to hold manifest attributes
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$ $Date$
 */
public class Attribute
{
    /**
     * The attribute's name
     */
    private String m_name;

    /**
     * The attribute's value
     */
    private String m_value;

    /**
     * Construct an empty attribute
     */
    public Attribute()
    {
    }

    /**
     * Construct a manifest by specifying its name and value
     *
     * @param name the attribute's name
     * @param value the Attribute's value
     */
    public Attribute( final String name, final String value )
    {
        m_name = name;
        m_value = value;
    }

    /**
     * Set the Attribute's name
     *
     * @param name the attribute's name
     */
    public void setName( final String name )
    {
        m_name = name;
    }

    /**
     * Set the Attribute's value
     *
     * @param value the attribute's value
     */
    public void setValue( final String value )
    {
        m_value = value;
    }

    /**
     * Get the Attribute's name
     *
     * @return the attribute's name.
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Get the Attribute's value
     *
     * @return the attribute's value.
     */
    public String getValue()
    {
        return m_value;
    }

    /**
     * Add a continuation line from the Manifest file When lines are too
     * long in a manifest, they are continued on the next line by starting
     * with a space. This method adds the continuation data to the attribute
     * value by skipping the first character.
     *
     * @param line The feature to be added to the Continuation attribute
     */
    public void addContinuation( final String line )
    {
        m_value += line.substring( 1 );
    }

    public boolean equals( Object object )
    {
        if( !( object instanceof Attribute ) )
        {
            return false;
        }

        final Attribute other = (Attribute)object;
        final String name = other.m_name;
        return
            ( null != m_name && null != name &&
            m_name.toLowerCase().equals( name.toLowerCase() ) &&
            null != m_value && m_value.equals( other.m_value ) );
    }

}
