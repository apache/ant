/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskContext;

/**
 * P4Change - grab a new changelist from Perforce. P4Change creates a new
 * changelist in perforce. P4Change sets the property ${p4.change} with the new
 * changelist number. This should then be passed into p4edit and p4submit.
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 * @see P4Edit
 * @see P4Submit
 */
public class P4Change
    extends P4Base
{
    private String m_emptyChangeList;
    private String m_description = "AutoSubmit By Ant";
    private final StringBuffer m_changelistData = new StringBuffer();
    private boolean m_changelist;

    /*
     * Set Description Variable.
     */
    public void setDescription( final String description )
    {
        m_description = description;
    }

    private String getEmptyChangeList()
        throws TaskException
    {
        m_changelist = true;
        execP4Command( "change -o", null );
        m_changelist = false;

        return m_changelistData.toString();
    }

    /**
     * Receive notification about the process writing
     * to standard output.
     */
    public void stdout( final String line )
    {
        if( m_changelist )
        {
            changelist_stdout( line );
        }
        else
        {
            change_stdout( line );
        }
    }

    public void execute()
        throws TaskException
    {
        if( m_emptyChangeList == null )
        {
            m_emptyChangeList = getEmptyChangeList();
        }

        //handler.setOutput( m_emptyChangeList );

        execP4Command( "change -i", null );
    }

    /**
     * Ensure that a string is backslashing slashes so that it does not confuse
     * them with Perl substitution delimiter in Oro. Backslashes are always
     * backslashes in a string unless they escape the delimiter.
     *
     * @param value the string to backslash for slashes
     * @return the backslashed string
     * @see < a href="http://jakarta.apache.org/oro/api/org/apache/oro/text/perl/Perl5Util.html#substitute(java.lang.String,%20java.lang.String)">
     *      Oro</a>
     */
    private String backslash( String value )
    {
        final StringBuffer buf = new StringBuffer( value.length() );
        final int len = value.length();
        for( int i = 0; i < len; i++ )
        {
            char c = value.charAt( i );
            if( c == '/' )
            {
                buf.append( '\\' );
            }
            buf.append( c );
        }
        return buf.toString();
    }

    private void changelist_stdout( String line )
    {
        if( !util.match( "/^#/", line ) )
        {
            if( util.match( "/error/", line ) )
            {
                getContext().debug( "Client Error" );
                registerError( new TaskException( "Perforce Error, check client settings and/or server" ) );
            }
            else if( util.match( "/<enter description here>/", line ) )
            {

                // we need to escape the description in case there are /
                m_description = backslash( m_description );
                line = util.substitute( "s/<enter description here>/" + m_description + "/", line );

            }
            else if( util.match( "/\\/\\//", line ) )
            {
                //Match "//" for begining of depot filespec
                return;
            }

            m_changelistData.append( line );
            m_changelistData.append( "\n" );
        }
    }

    private void change_stdout( String line )
    {
        if( util.match( "/Change/", line ) )
        {
            //Remove any non-numerical chars - should leave the change number
            line = util.substitute( "s/[^0-9]//g", line );

            final int changenumber = Integer.parseInt( line );
            getContext().info( "Change Number is " + changenumber );
            try
            {
                getContext().setProperty( "p4.change", "" + changenumber );
            }
            catch( final TaskException te )
            {
                registerError( te );
            }
        }
        else if( util.match( "/error/", line ) )
        {
            final String message = "Perforce Error, check client settings and/or server";
            registerError( new TaskException( message ) );
        }
    }
}
