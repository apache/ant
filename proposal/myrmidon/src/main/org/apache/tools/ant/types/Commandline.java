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
import java.util.StringTokenizer;
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
    implements Cloneable
{
    protected final ArrayList m_arguments = new ArrayList();
    private String m_executable;

    public Commandline( String to_process )
        throws TaskException
    {
        String[] tmp = translateCommandline( to_process );
        if( tmp != null && tmp.length > 0 )
        {
            setExecutable( tmp[ 0 ] );
            for( int i = 1; i < tmp.length; i++ )
            {
                createArgument().setValue( tmp[ i ] );
            }
        }
    }

    public Commandline()
    {
        super();
    }

    /**
     * Put quotes around the given String if necessary. <p>
     *
     * If the argument doesn't include spaces or quotes, return it as is. If it
     * contains double quotes, use single quotes - else surround the argument by
     * double quotes.</p>
     *
     * @param argument Description of Parameter
     * @return Description of the Returned Value
     */
    public static String quoteArgument( String argument )
        throws TaskException
    {
        if( argument.indexOf( "\"" ) > -1 )
        {
            if( argument.indexOf( "\'" ) > -1 )
            {
                throw new TaskException( "Can\'t handle single and double quotes in same argument" );
            }
            else
            {
                return '\'' + argument + '\'';
            }
        }
        else if( argument.indexOf( "\'" ) > -1 || argument.indexOf( " " ) > -1 )
        {
            return '\"' + argument + '\"';
        }
        else
        {
            return argument;
        }
    }

    public static String toString( String[] line )
    {
        // empty path return empty string
        if( line == null || line.length == 0 )
            return "";

        // path containing one or more elements
        final StringBuffer result = new StringBuffer();
        for( int i = 0; i < line.length; i++ )
        {
            if( i > 0 )
            {
                result.append( ' ' );
            }

            try
            {
                result.append( quoteArgument( line[ i ] ) );
            }
            catch( TaskException e )
            {
            }

        }
        return result.toString();
    }

    public static String[] translateCommandline( String to_process )
        throws TaskException
    {
        if( to_process == null || to_process.length() == 0 )
        {
            return new String[ 0 ];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        StringTokenizer tok = new StringTokenizer( to_process, "\"\' ", true );
        ArrayList v = new ArrayList();
        StringBuffer current = new StringBuffer();

        while( tok.hasMoreTokens() )
        {
            String nextTok = tok.nextToken();
            switch( state )
            {
                case inQuote:
                    if( "\'".equals( nextTok ) )
                    {
                        state = normal;
                    }
                    else
                    {
                        current.append( nextTok );
                    }
                    break;
                case inDoubleQuote:
                    if( "\"".equals( nextTok ) )
                    {
                        state = normal;
                    }
                    else
                    {
                        current.append( nextTok );
                    }
                    break;
                default:
                    if( "\'".equals( nextTok ) )
                    {
                        state = inQuote;
                    }
                    else if( "\"".equals( nextTok ) )
                    {
                        state = inDoubleQuote;
                    }
                    else if( " ".equals( nextTok ) )
                    {
                        if( current.length() != 0 )
                        {
                            v.add( current.toString() );
                            current.setLength( 0 );
                        }
                    }
                    else
                    {
                        current.append( nextTok );
                    }
                    break;
            }
        }

        if( current.length() != 0 )
        {
            v.add( current.toString() );
        }

        if( state == inQuote || state == inDoubleQuote )
        {
            throw new TaskException( "unbalanced quotes in " + to_process );
        }

        final String[] args = new String[ v.size() ];
        return (String[])v.toArray( args );
    }

    /**
     * Sets the executable to run.
     *
     * @param executable The new Executable value
     */
    public void setExecutable( final String executable )
    {
        if( executable == null || executable.length() == 0 )
            return;
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

    public void addArguments( String[] line )
    {
        for( int i = 0; i < line.length; i++ )
        {
            createArgument().setValue( line[ i ] );
        }
    }

    /**
     * Clear out the whole command line.
     */
    public void clear()
    {
        m_executable = null;
        m_arguments.clear();
    }

    /**
     * Clear out the arguments but leave the executable in place for another
     * operation.
     */
    public void clearArgs()
    {
        m_arguments.clear();
    }

    public Object clone()
    {
        final Commandline commandline = new Commandline();
        commandline.setExecutable( m_executable );
        commandline.addArguments( getArguments() );
        return commandline;
    }

    /**
     * Creates an argument object. Each commandline object has at most one
     * instance of the argument class.
     *
     * @return the argument object.
     */
    public Argument createArgument()
    {
        final Argument argument = new Argument();
        m_arguments.add( argument );
        return argument;
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
        return toString( getCommandline() );
    }

}
