/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.types;

import java.util.ArrayList;
import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.util.FileUtils;

/**
 * A utility class to use to assemble a list of command-line arguments.
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class ArgumentList
{
    protected final ArrayList m_arguments = new ArrayList();

    /**
     * Returns all arguments defined by <code>addLine</code>, <code>addValue</code>
     * or the argument object.
     *
     * @return The Arguments value
     */
    public String[] getArguments()
    {
        final int size = m_arguments.size();
        final ArrayList result = new ArrayList( size * 2 );
        for( int i = 0; i < size; i++ )
        {
            final Argument arg = (Argument)m_arguments.get( i );
            final String[] s = arg.getParts();
            for( int j = 0; j < s.length; j++ )
            {
                result.add( s[ j ] );
            }
        }

        final String[] res = new String[ result.size() ];
        return (String[])result.toArray( res );
    }

    public void addArguments( final String[] args )
    {
        for( int i = 0; i < args.length; i++ )
        {
            addArgument( args[ i ] );
        }
    }

    public void addArguments( final ArgumentList args )
    {
        addArguments( args.getArguments() );
    }

    public void addArgument( final File argument )
    {
        addArgument( new Argument( argument ) );
    }

    public void addArgument( final String argument )
    {
        addArgument( new Argument( argument ) );
    }

    public void addArgument( final Argument argument )
    {
        m_arguments.add( argument );
    }

    public void addLine( final String line )
        throws TaskException
    {
        final String[] parts = FileUtils.translateCommandline( line );
        addArguments( parts );
    }
}
