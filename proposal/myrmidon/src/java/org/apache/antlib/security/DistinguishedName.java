/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.security;

import java.util.ArrayList;
import java.util.Iterator;

public class DistinguishedName
{
    private ArrayList m_params = new ArrayList();

    public Iterator getParams()
    {
        return m_params.iterator();
    }

    public Object createParam()
    {
        final DnameParam param = new DnameParam();
        m_params.add( param );
        return param;
    }

    private String encode( final String string )
    {
        int end = string.indexOf( ',' );
        if( -1 == end )
        {
            return string;
        }

        final StringBuffer sb = new StringBuffer();

        int start = 0;
        while( -1 != end )
        {
            sb.append( string.substring( start, end ) );
            sb.append( "\\," );
            start = end + 1;
            end = string.indexOf( ',', start );
        }

        sb.append( string.substring( start ) );

        return sb.toString();
    }

    public String toString()
    {
        final int size = m_params.size();
        final StringBuffer sb = new StringBuffer();
        boolean firstPass = true;

        for( int i = 0; i < size; i++ )
        {
            if( !firstPass )
            {
                sb.append( " ," );
            }
            firstPass = false;

            final DnameParam param = (DnameParam)m_params.get( i );
            sb.append( encode( param.getName() ) );
            sb.append( '=' );
            sb.append( encode( param.getValue() ) );
        }

        return sb.toString();
    }
}
