/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.cvslib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import org.apache.aut.nativelib.ExecOutputHandler;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.todo.types.Commandline;

/**
 * Change log task.
 * The task will examine the output of cvs log and group related changes together.
 * It produces an XML output representing the list of changes.
 * <PRE>
 * <FONT color=#0000ff>&lt;!-- Root element --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> changelog <FONT color=#ff00ff>(entry</FONT><FONT color=#ff00ff>+</FONT><FONT color=#ff00ff>)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- CVS Entry --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> entry <FONT color=#ff00ff>(date,author,file</FONT><FONT color=#ff00ff>+</FONT><FONT color=#ff00ff>,msg)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- Date of cvs entry --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> date <FONT color=#ff00ff>(#PCDATA)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- Author of change --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> author <FONT color=#ff00ff>(#PCDATA)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- List of files affected --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> msg <FONT color=#ff00ff>(#PCDATA)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- File changed --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> file <FONT color=#ff00ff>(name,revision,prevrevision</FONT><FONT color=#ff00ff>?</FONT><FONT color=#ff00ff>)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- Name of the file --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> name <FONT color=#ff00ff>(#PCDATA)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- Revision number --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> revision <FONT color=#ff00ff>(#PCDATA)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * <FONT color=#0000ff>&lt;!-- Previous revision number --&gt;</FONT>
 * <FONT color=#6a5acd>&lt;!ELEMENT</FONT> prevrevision <FONT color=#ff00ff>(#PCDATA)</FONT><FONT color=#6a5acd>&gt;</FONT>
 * </PRE>
 *
 * @author <a href="mailto:jeff.martin@synamic.co.uk">Jeff Martin</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant.task name="changelog"
 */
public class ChangeLog
    extends AbstractTask
    implements ExecOutputHandler
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( ChangeLog.class );

    //private static final int GET_ENTRY = 0;
    private static final int GET_FILE = 1;
    private static final int GET_DATE = 2;
    private static final int GET_COMMENT = 3;
    private static final int GET_REVISION = 4;
    private static final int GET_PREVIOUS_REV = 5;

    /** input format for dates read in from cvs log */
    private static final SimpleDateFormat c_inputDate = new SimpleDateFormat( "yyyy/MM/dd" );
    /** output format for dates writtn to xml file */
    private static final SimpleDateFormat c_outputDate = new SimpleDateFormat( "yyyy-MM-dd" );
    /** output format for times writtn to xml file */
    private static final SimpleDateFormat c_outputTime = new SimpleDateFormat( "hh:mm" );

    /** User list */
    private File m_users;

    /** User list */
    private final Properties m_userList = new Properties();

    /** User list */
    private Vector m_ulist = new Vector();

    /** Input dir */
    private File m_basedir;

    /** Output file */
    private File m_destfile;

    private int m_status = GET_FILE;

    /** rcs entries */
    private final Hashtable m_entries = new Hashtable();
    private String m_workingFile;
    private String m_workingDate;
    private String m_workingAuthor;
    private String m_workingComment;
    private String m_workingRevision;
    private String m_workingPreviousRevision;

    /**
     * Set the base dir for cvs.
     */
    public void setBasedir( final File basedir )
    {
        m_basedir = basedir;
    }

    /**
     * Set the output file for the log.
     */
    public void setDestfile( final File destfile )
    {
        m_destfile = destfile;
    }

    /**
     * Set a lookup list of user names & addresses
     */
    public void setUserlist( final File users )
    {
        m_users = users;
    }

    /**
     * Add a user to list changelog knows about.
     *
     * @param user the user
     */
    public void addUser( final CvsUser user )
    {
        m_ulist.addElement( user );
    }

    /**
     * Execute task
     */
    public void execute() throws TaskException
    {
        validate();

        loadUserlist();

        for( Enumeration e = m_ulist.elements(); e.hasMoreElements(); )
        {
            final CvsUser user = (CvsUser)e.nextElement();
            user.validate();
            m_userList.put( user.getUserID(), user.getDisplayname() );
        }

        final Commandline command = new Commandline();
        command.setExecutable( "cvs" );
        command.addArgument( "log" );

        final Execute exe = new Execute();
        exe.setWorkingDirectory( m_basedir );
        exe.setCommandline( command );
        exe.setExecOutputHandler( this );
        exe.execute( getContext() );

        writeChangeLog();
    }

    /**
     * Validate the parameters specified for task.
     *
     * @throws TaskException if fails validation checks
     */
    private void validate()
        throws TaskException
    {
        if( null == m_basedir )
        {
            final String message = REZ.getString( "changelog.missing-basedir.error" );
            throw new TaskException( message );
        }
        if( null == m_destfile )
        {
            final String message = REZ.getString( "changelog.missing-destfile.error" );
            throw new TaskException( message );
        }
        if( !m_basedir.exists() )
        {
            final String message =
                REZ.getString( "changelog.bad-basedir.error", m_basedir.getAbsolutePath() );
            throw new TaskException( message );
        }
        if( null != m_users && !m_users.exists() )
        {
            final String message =
                REZ.getString( "changelog.bad-userlist.error", m_users.getAbsolutePath() );
            throw new TaskException( message );
        }
    }

    /**
     * Load the userlist from the userList file (if specified) and
     * add to list of users.
     *
     * @throws TaskException if file can not be loaded for some reason
     */
    private void loadUserlist()
        throws TaskException
    {
        if( null != m_users )
        {
            try
            {
                m_userList.load( new FileInputStream( m_users ) );
            }
            catch( final IOException ioe )
            {
                throw new TaskException( ioe.toString(), ioe );
            }
        }
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
                //Was a fall through ....
                //break;
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
        if( line.startsWith( "======" ) || line.startsWith( "------" ) )
        {
            final int end = m_workingComment.length() - lineSeparator.length(); //was -1
            m_workingComment = m_workingComment.substring( 0, end );
            m_workingComment = "<![CDATA[" + m_workingComment + "]]>";
            m_status = GET_PREVIOUS_REV;
        }
        else
        {
            m_workingComment += line + lineSeparator;
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
            m_workingFile = line.substring( 14, line.length() );
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
            m_workingRevision = line.substring( 9 );
            m_status = GET_DATE;
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
            m_workingDate = line.substring( 6, 16 );
            String lineData = line.substring( line.indexOf( ";" ) + 1 );
            m_workingAuthor = lineData.substring( 10, lineData.indexOf( ";" ) );

            if( m_userList.containsKey( m_workingAuthor ) )
            {
                m_workingAuthor = "<![CDATA[" + m_userList.getProperty( m_workingAuthor ) + "]]>";
            }

            m_status = GET_COMMENT;

            //Reset comment to empty here as we can accumulate multiple lines
            //in the processComment method
            m_workingComment = "";
        }
    }

    /**
     * Process a line while in "GET_PREVIOUS_REVISION" state.
     *
     * @param line the line
     */
    private void processGetPreviousRevision( final String line )
    {
        final String entryKey = m_workingDate + m_workingAuthor + m_workingComment;
        if( line.startsWith( "revision" ) )
        {
            m_workingPreviousRevision = line.substring( 9 );
            m_status = GET_FILE;

            CVSEntry entry;
            if( !m_entries.containsKey( entryKey ) )
            {
                entry = new CVSEntry( parseDate( m_workingDate ), m_workingAuthor, m_workingComment );
                m_entries.put( entryKey, entry );
            }
            else
            {
                entry = (CVSEntry)m_entries.get( entryKey );
            }
            entry.addFile( m_workingFile, m_workingRevision, m_workingPreviousRevision );
        }
        else if( line.startsWith( "======" ) )
        {
            m_status = GET_FILE;
            CVSEntry entry;
            if( !m_entries.containsKey( entryKey ) )
            {
                entry = new CVSEntry( parseDate( m_workingDate ), m_workingAuthor, m_workingComment );
                m_entries.put( entryKey, entry );
            }
            else
            {
                entry = (CVSEntry)m_entries.get( entryKey );
            }
            entry.addFile( m_workingFile, m_workingRevision );
        }
    }

    /**
     * Receive notification about the process writing
     * to standard error.
     */
    public void stderr( String line )
    {
        //ignored
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
            final String message = REZ.getString( "changelog.bat-date.error", date );
            getContext().error( message );
            return null;
        }
    }

    /**
     * Print changelog to file specified in task.
     *
     * @throws TaskException if theres an error writing changelog
     */
    private void writeChangeLog()
        throws TaskException
    {
        FileOutputStream output = null;
        try
        {
            output = new FileOutputStream( m_destfile );
            final PrintWriter writer =
                new PrintWriter( new OutputStreamWriter( output, "UTF-8" ) );
            printChangeLog( writer );
        }
        catch( final UnsupportedEncodingException uee )
        {
            getContext().error( uee.toString(), uee );
        }
        catch( final IOException ioe )
        {
            throw new TaskException( ioe.toString(), ioe );
        }
        finally
        {
            IOUtil.shutdownStream( output );
        }
    }

    /**
     * Print out the full changelog.
     */
    private void printChangeLog( final PrintWriter output )
    {
        output.println( "<changelog>" );
        for( Enumeration en = m_entries.elements(); en.hasMoreElements(); )
        {
            final CVSEntry entry = (CVSEntry)en.nextElement();
            printEntry( output, entry );
        }
        output.println( "</changelog>" );
        output.flush();
        output.close();
    }

    /**
     * Print out an individual entry in changelog.
     *
     * @param entry the entry to print
     */
    private void printEntry( final PrintWriter output, final CVSEntry entry )
    {
        output.println( "\t<entry>" );
        output.println( "\t\t<date>" + c_outputDate.format( entry.getDate() ) + "</date>" );
        output.println( "\t\t<time>" + c_outputTime.format( entry.getDate() ) + "</time>" );
        output.println( "\t\t<author>" + entry.getAuthor() + "</author>" );

        final Iterator iterator = entry.getFiles().iterator();
        while( iterator.hasNext() )
        {
            final RCSFile file = (RCSFile)iterator.next();
            output.println( "\t\t<file>" );
            output.println( "\t\t\t<name>" + file.getName() + "</name>" );
            output.println( "\t\t\t<revision>" + file.getRevision() + "</revision>" );

            final String previousRevision = file.getPreviousRevision();
            if( previousRevision != null )
            {
                output.println( "\t\t\t<prevrevision>" + previousRevision + "</prevrevision>" );
            }

            output.println( "\t\t</file>" );
        }
        output.println( "\t\t<msg>" + entry.getComment() + "</msg>" );
        output.println( "\t</entry>" );
    }
}
