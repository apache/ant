/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.cvslib;

/**
 * Represents a RCS File cheange.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jeff.martin@synamic.co.uk">Jeff Martin</a>
 * @version $Revision$ $Date$
 */
class RCSFile
{
    private final String m_name;
    private final String m_revision;
    private String m_previousRevision;

    RCSFile( final String name, final String rev )
    {
        this( name, rev, null );
    }

    RCSFile( final String name,
                     final String revision,
                     final String previousRevision )
    {
        m_name = name;
        m_revision = revision;
        if( !revision.equals( previousRevision ) )
        {
            m_previousRevision = previousRevision;
        }
    }

    String getName()
    {
        return m_name;
    }

    String getRevision()
    {
        return m_revision;
    }

    String getPreviousRevision()
    {
        return m_previousRevision;
    }
}
