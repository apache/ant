/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import org.apache.tools.ant.Project;

/**
 * inner class to hold a name on list. "If" and "Unless" attributes may be
 * used to invalidate the entry based on the existence of a property
 * (typically set thru the use of the Available task).
 */
public class NameEntry
{
    private String m_if;
    private String m_name;
    private String m_unless;
    private PatternSet m_set;

    public NameEntry( final PatternSet set )
    {
        m_set = set;
    }

    public void setIf( final String ifCondition )
    {
        m_if = ifCondition;
    }

    public void setName( final String name )
    {
        m_name = name;
    }

    public void setUnless( final String unlessCondition )
    {
        m_unless = unlessCondition;
    }

    public String evalName( Project p )
    {
        return valid( p ) ? m_name : null;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer( m_name );
        if( ( m_if != null ) || ( m_unless != null ) )
        {
            buf.append( ":" );
            String connector = "";

            if( m_if != null )
            {
                buf.append( "if->" );
                buf.append( m_if );
                connector = ";";
            }
            if( m_unless != null )
            {
                buf.append( connector );
                buf.append( "unless->" );
                buf.append( m_unless );
            }
        }

        return buf.toString();
    }

    private boolean valid( Project p )
    {
        if( m_if != null && p.getProperty( m_if ) == null )
        {
            return false;
        }
        else if( m_unless != null && p.getProperty( m_unless ) != null )
        {
            return false;
        }
        else
        {
            return true;
        }
    }
}
