/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.javadoc;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class GroupArgument
{
    private ArrayList m_packages = new ArrayList( 3 );
    private Html m_title;

    public void setPackages( final String src )
    {
        final StringTokenizer tok = new StringTokenizer( src, "," );
        while( tok.hasMoreTokens() )
        {
            final String p = tok.nextToken();
            final PackageName pn = new PackageName();
            pn.setName( p );
            addPackage( pn );
        }
    }

    public void setTitle( final String src )
    {
        final Html h = new Html();
        h.addContent( src );
        addTitle( h );
    }

    public String getPackages()
    {
        final StringBuffer p = new StringBuffer();
        for( int i = 0; i < m_packages.size(); i++ )
        {
            if( i > 0 )
            {
                p.append( ":" );
            }
            p.append( m_packages.get( i ).toString() );
        }
        return p.toString();
    }

    public String getTitle()
    {
        return m_title != null ? m_title.getText() : null;
    }

    public void addPackage( final PackageName pn )
    {
        m_packages.add( pn );
    }

    public void addTitle( final Html text )
    {
        m_title = text;
    }
}
