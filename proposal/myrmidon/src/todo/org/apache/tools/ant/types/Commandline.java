/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.io.File;
import java.util.ArrayList;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.myrmidon.api.TaskException;

/**
 * Commandline objects help handling command lines specifying processes to
 * execute. The class can be used to define a command line as nested elements or
 * as a helper to define a command line by an application. <p>
 *
 * <code>
 * &lt;someelement&gt;<br>
 * &nbsp;&nbsp;&lt;acommandline executable="/executable/to/run"&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument value="argument 1" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument line="argument_1 argument_2 argument_3"
 * /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument value="argument 4" /&gt;<br>
 * &nbsp;&nbsp;&lt;/acommandline&gt;<br>
 * &lt;/someelement&gt;<br>
 * </code> The element <code>someelement</code> must provide a method <code>createAcommandline</code>
 * which returns an instance of this class.
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Commandline
{
    protected final ArrayList m_arguments = new ArrayList();
    private String m_executable;

    public Commandline()
    {
    }

    public Commandline( final String[] command )
    {
        if( 0 == command.length )
        {
            throw new IllegalArgumentException( "command" );
        }

        m_executable = command[ 0 ];
        for( int i = 1; i < command.length; i++ )
        {
            addArgument( command[ i ] );
        }
    }

    /**
     * Sets the executable to run.
     *
     * @param executable The new Executable value
     */
    public void setExecutable( final String executable )
    {
        if( executable == null || executable.length() == 0 ) {
            return;
        }
        m_executable = executable.replace( '/', File.separatorChar )
            .replace( '\\', File.separatorChar );
    }

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

    /**
     * Returns the executable and all defined arguments.
     *
     * @return The Commandline value
     */
    public String[] getCommandline()
    {
        final String[] args = getArguments();
        if( m_executable == null )
        {
            return args;
        }
        final String[] result = new String[ args.length + 1 ];
        result[ 0 ] = m_executable;
        System.arraycopy( args, 0, result, 1, args.length );
        return result;
    }

    public String getExecutable()
    {
        return m_executable;
    }

    public void addArguments( final String[] args )
    {
        for( int i = 0; i < args.length; i++ )
        {
            addArgument( args[ i ] );
        }
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
        final Argument argument = new Argument();
        argument.setLine( line );
        addArgument( argument );
    }

    /**
     * Return a marker. <p>
     *
     * This marker can be used to locate a position on the commandline - to
     * insert something for example - when all parameters have been set.</p>
     *
     * @return Description of the Returned Value
     */
    public Marker createMarker()
    {
        return new Marker( this, m_arguments.size() );
    }

    public int size()
    {
        return getCommandline().length;
    }

    public String toString()
    {
        return StringUtil.join( getCommandline(), " " );
    }
}
