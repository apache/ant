/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.cvslib;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import org.apache.aut.nativelib.ExecOutputHandler;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskContext;

/**
 * A class used to parse the output of the CVS log command.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class ChangeLogParser
    implements ExecOutputHandler
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( ChangeLogParser.class );

    //private static final int GET_ENTRY = 0;
    private static final int GET_FILE = 1;
    private static final int GET_DATE = 2;
    private static final int GET_COMMENT = 3;
    private static final int GET_REVISION = 4;
    private static final int GET_PREVIOUS_REV = 5;

    /** input format for dates read in from cvs log */
    private static final SimpleDateFormat c_inputDate = new SimpleDateFormat( "yyyy/MM/dd" );

    //The following is data used while processing stdout of CVS command
    private String m_file;
    private String m_date;
    private String m_author;
    private String m_comment;
    private String m_revision;
    private String m_previousRevision;

    private int m_status = GET_FILE;

    private final TaskContext m_context;

    /** rcs entries */
    private final Hashtable m_entries = new Hashtable();

    private final Properties m_userList;

    /**
     * Construct a parser that uses specified user list.
     *
     * @param userList the userlist
     */
    ChangeLogParser( final Properties userList,
                     final TaskContext context )
    {
        m_userList = userList;
        m_context = context;
    }

    /**
     * Get a list of rcs entrys as an array.
     *
     * @return a list of rcs entrys as an array
     */
    CVSEntry[] getEntrySetAsArray()
    {
        final CVSEntry[] array = new CVSEntry[ m_entries.values().size() ];
        return (CVSEntry[])m_entries.values().toArray( array );
    }

    /**
     * Receive notification about the process writing
     * to standard error.
     */
    public void stderr( String line )
    {
        m_context.error( line );
    }

    /**
     * Receive notification about the process writing
     * to standard output.
     */
    public void stdout( final String line )
    {
        switch( m_status )
        {
            case GET_FILE:
                processFile( line );
                break;
            case GET_REVISION:
                processRevision( line );
                break;

            case GET_DATE:
                processDate( line );
                break;

            case GET_COMMENT:
                processComment( line );
                break;

            case GET_PREVIOUS_REV:
                processGetPreviousRevision( line );
                break;
        }
    }

    /**
     * Process a line while in "GET_COMMENT" state.
     *
     * @param line the line
     */
    private void processComment( final String line )
    {
        final String lineSeparator = System.getProperty( "line.separator" );
        if( line.startsWith( "======" ) )
        {
            //We have ended changelog for that particular file
            //so we can save it
            final int end = m_comment.length() - lineSeparator.length(); //was -1
            m_comment = m_comment.substring( 0, end );
            saveEntry();
            m_status = GET_FILE;
        }
        else if( line.startsWith( "------" ) )
        {
            final int end = m_comment.length() - lineSeparator.length(); //was -1
            m_comment = m_comment.substring( 0, end );
            m_status = GET_PREVIOUS_REV;
        }
        else
        {
            m_comment += line + lineSeparator;
        }
    }

    /**
     * Process a line while in "GET_FILE" state.
     *
     * @param line the line
     */
    private void processFile( final String line )
    {
        if( line.startsWith( "Working file:" ) )
        {
            m_file = line.substring( 14, line.length() );
            m_status = GET_REVISION;
        }
    }

    /**
     * Process a line while in "REVISION" state.
     *
     * @param line the line
     */
    private void processRevision( final String line )
    {
        if( line.startsWith( "revision" ) )
        {
            m_revision = line.substring( 9 );
            m_status = GET_DATE;
        }
        else if( line.startsWith( "======" ) )
        {
            //There was no revisions in this changelog
            //entry so lets move unto next file
            m_status = GET_FILE;
        }
    }

    /**
     * Process a line while in "DATE" state.
     *
     * @param line the line
     */
    private void processDate( final String line )
    {
        if( line.startsWith( "date:" ) )
        {
            m_date = line.substring( 6, 16 );
            String lineData = line.substring( line.indexOf( ";" ) + 1 );
            m_author = lineData.substring( 10, lineData.indexOf( ";" ) );

            if( m_userList.containsKey( m_author ) )
            {
                m_author = m_userList.getProperty( m_author );
            }

            m_status = GET_COMMENT;

            //Reset comment to empty here as we can accumulate multiple lines
            //in the processComment method
            m_comment = "";
        }
    }

    /**
     * Process a line while in "GET_PREVIOUS_REVISION" state.
     *
     * @param line the line
     */
    private void processGetPreviousRevision( final String line )
    {
        if( !line.startsWith( "revision" ) )
        {
            final String message =
                REZ.getString( "changelog.unexpected.line", line );
            throw new IllegalStateException( message );
        }
        m_previousRevision = line.substring( 9 );

        saveEntry();

        m_revision = m_previousRevision;
        m_status = GET_DATE;
    }

    /**
     * Utility method that saves the current entry.
     */
    private void saveEntry()
    {
        final String entryKey = m_date + m_author + m_comment;
        CVSEntry entry;
        if( !m_entries.containsKey( entryKey ) )
        {
            entry = new CVSEntry( parseDate( m_date ), m_author, m_comment );
            m_entries.put( entryKey, entry );
        }
        else
        {
            entry = (CVSEntry)m_entries.get( entryKey );
        }

        entry.addFile( m_file, m_revision, m_previousRevision );
    }

    /**
     * Parse date out from expected format.
     *
     * @param date the string holding dat
     * @return the date object or null if unknown date format
     */
    private Date parseDate( final String date )
    {
        try
        {
            return c_inputDate.parse( date );
        }
        catch( ParseException e )
        {
            //final String message = REZ.getString( "changelog.bat-date.error", date );
            //getContext().error( message );
            return null;
        }
    }
}
