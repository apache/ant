/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.text;

import org.apache.tools.ant.util.FileNameMapper;

class ExtMapper
    implements FileNameMapper
{
    private final String m_extension;

    public ExtMapper( final String extension )
    {
        m_extension = extension;
    }

    public void setFrom( final String from )
    {
    }

    public void setTo( final String to )
    {
    }

    public String[] mapFileName( final String filename )
    {
        final int index = filename.lastIndexOf( '.' );
        if( index >= 0 )
        {
            final String reult = filename.substring( 0, index ) + m_extension;
            return new String[]{reult};
        }
        else
        {
            return new String[]{filename + m_extension};
        }
    }
}
