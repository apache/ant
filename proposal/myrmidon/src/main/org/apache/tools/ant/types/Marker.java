/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.util.ArrayList;

/**
 * Class to keep track of the position of an Argument.
 *
 * <p>This class is there to support the srcfile and targetfile
 * elements of &lt;execon&gt; and &lt;transform&gt; - don't know
 * whether there might be additional use cases.</p> --SB
 */
public class Marker
{
    private int m_realPos = -1;
    private int m_position;
    private Commandline m_commandline;

    Marker( Commandline commandline, int position )
    {
        m_commandline = commandline;
        m_position = position;
    }

    /**
     * Return the number of arguments that preceeded this marker. <p>
     *
     * The name of the executable - if set - is counted as the very first
     * argument.</p>
     *
     * @return The Position value
     */
    public int getPosition()
    {
        if( m_realPos == -1 )
        {
            m_realPos = ( m_commandline.getExecutable() == null ? 0 : 1 );
            final ArrayList arguments = m_commandline.m_arguments;
            for( int i = 0; i < m_position; i++ )
            {
                final Argument arg = (Argument)arguments.get( i );
                m_realPos += arg.getParts().length;
            }
        }
        return m_realPos;
    }
}
