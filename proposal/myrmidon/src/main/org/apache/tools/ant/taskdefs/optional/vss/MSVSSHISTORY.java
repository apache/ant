/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.vss;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Task to perform HISTORY commands to Microsoft Visual Source Safe.
 *
 * @author Balazs Fejes 2
 * @author Glenn_Twiggs@bmc.com
 */

public class MSVSSHISTORY extends MSVSS
{

    public final static String VALUE_FROMDATE = "~d";
    public final static String VALUE_FROMLABEL = "~L";

    public final static String FLAG_OUTPUT = "-O";
    public final static String FLAG_USER = "-U";

    private String m_FromDate = null;
    private String m_ToDate = null;
    private DateFormat m_DateFormat =
        DateFormat.getDateInstance( DateFormat.SHORT );

    private String m_FromLabel = null;
    private String m_ToLabel = null;
    private String m_OutputFileName = null;
    private String m_User = null;
    private int m_NumDays = Integer.MIN_VALUE;
    private String m_Style = "";
    private boolean m_Recursive = false;

    /**
     * Set the Start Date for the Comparison of two versions in SourceSafe
     * History
     *
     * @param dateFormat The new DateFormat value
     */
    public void setDateFormat( String dateFormat )
    {
        if( !( dateFormat.equals( "" ) || dateFormat == null ) )
        {
            m_DateFormat = new SimpleDateFormat( dateFormat );
        }
    }

    /**
     * Set the Start Date for the Comparison of two versions in SourceSafe
     * History
     *
     * @param fromDate The new FromDate value
     */
    public void setFromDate( String fromDate )
    {
        if( fromDate.equals( "" ) || fromDate == null )
        {
            m_FromDate = null;
        }
        else
        {
            m_FromDate = fromDate;
        }
    }

    /**
     * Set the Start Label
     *
     * @param fromLabel The new FromLabel value
     */
    public void setFromLabel( String fromLabel )
    {
        if( fromLabel.equals( "" ) || fromLabel == null )
        {
            m_FromLabel = null;
        }
        else
        {
            m_FromLabel = fromLabel;
        }
    }

    /**
     * Set the number of days to go back for Comparison <p>
     *
     * The default value is 2 days.
     *
     * @param numd The new Numdays value
     */
    public void setNumdays( int numd )
    {
        m_NumDays = numd;
    }

    /**
     * Set the output file name for SourceSafe output
     *
     * @param outfile The new Output value
     */
    public void setOutput( File outfile )
    {
        if( outfile == null )
        {
            m_OutputFileName = null;
        }
        else
        {
            m_OutputFileName = outfile.getAbsolutePath();
        }
    }

    /**
     * Set behaviour recursive or non-recursive
     *
     * @param recursive The new Recursive value
     */
    public void setRecursive( boolean recursive )
    {
        m_Recursive = recursive;
    }

    /**
     * Specify the detail of output
     *
     * @param attr The new Style value
     */
    public void setStyle( BriefCodediffNofile attr )
        throws TaskException
    {
        String option = attr.getValue();
        if( option.equals( "brief" ) )
        {
            m_Style = "-B";
        }
        else if( option.equals( "codediff" ) )
        {
            m_Style = "-D";
        }
        else if( option.equals( "default" ) )
        {
            m_Style = "";
        }
        else if( option.equals( "nofile" ) )
        {
            m_Style = "-F-";
        }
        else
        {
            throw new TaskException( "Style " + attr + " unknown." );
        }
    }

    /**
     * Set the End Date for the Comparison of two versions in SourceSafe History
     *
     * @param toDate The new ToDate value
     */
    public void setToDate( String toDate )
    {
        if( toDate.equals( "" ) || toDate == null )
        {
            m_ToDate = null;
        }
        else
        {
            m_ToDate = toDate;
        }
    }

    /**
     * Set the End Label
     *
     * @param toLabel The new ToLabel value
     */
    public void setToLabel( String toLabel )
    {
        if( toLabel.equals( "" ) || toLabel == null )
        {
            m_ToLabel = null;
        }
        else
        {
            m_ToLabel = toLabel;
        }
    }

    /**
     * Set the Username of the user whose changes we would like to see.
     *
     * @param user The new User value
     */
    public void setUser( String user )
    {
        m_User = user;
    }

    /**
     * Executes the task. <p>
     *
     * Builds a command line to execute ss and then calls Exec's run method to
     * execute the command line.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        Commandline commandLine = new Commandline();
        int result = 0;

        // first off, make sure that we've got a command and a vssdir and a label ...
        if( getVsspath() == null )
        {
            String msg = "vsspath attribute must be set!";
            throw new TaskException( msg );
        }

        // now look for illegal combinations of things ...

        // build the command line from what we got the format is
        // ss History elements [-H] [-L] [-N] [-O] [-V] [-Y] [-#] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable( getSSCommand() );
        commandLine.createArgument().setValue( COMMAND_HISTORY );

        // VSS items
        commandLine.createArgument().setValue( getVsspath() );

        // -I-
        commandLine.createArgument().setValue( "-I-" );// ignore all errors

        // -V
        // Label an existing file or project version
        getVersionDateCommand( commandLine );
        getVersionLabelCommand( commandLine );

        // -R
        if( m_Recursive )
        {
            commandLine.createArgument().setValue( FLAG_RECURSION );
        }

        // -B / -D / -F-
        if( m_Style.length() > 0 )
        {
            commandLine.createArgument().setValue( m_Style );
        }

        // -Y
        getLoginCommand( commandLine );

        // -O
        getOutputCommand( commandLine );

        System.out.println( "***: " + commandLine );

        result = run( commandLine );
        if( result != 0 )
        {
            String msg = "Failed executing: " + commandLine.toString();
            throw new TaskException( msg );
        }

    }

    /**
     * Builds the version date command.
     *
     * @param cmd the commandline the command is to be added to
     */
    private void getOutputCommand( Commandline cmd )
    {
        if( m_OutputFileName != null )
        {
            cmd.createArgument().setValue( FLAG_OUTPUT + m_OutputFileName );
        }
    }

    /**
     * @param cmd Description of Parameter
     */
    private void getRecursiveCommand( Commandline cmd )
    {
        if( !m_Recursive )
        {
            return;
        }
        else
        {
            cmd.createArgument().setValue( FLAG_RECURSION );
        }
    }

    /**
     * Builds the User command.
     *
     * @param cmd the commandline the command is to be added to
     */
    private void getUserCommand( Commandline cmd )
    {
        if( m_User != null )
        {
            cmd.createArgument().setValue( FLAG_USER + m_User );
        }
    }

    /**
     * Builds the version date command.
     *
     * @param cmd the commandline the command is to be added to
     * @exception TaskException Description of Exception
     */
    private void getVersionDateCommand( Commandline cmd )
        throws TaskException
    {
        if( m_FromDate == null && m_ToDate == null && m_NumDays == Integer.MIN_VALUE )
        {
            return;
        }

        if( m_FromDate != null && m_ToDate != null )
        {
            cmd.createArgument().setValue( FLAG_VERSION_DATE + m_ToDate + VALUE_FROMDATE + m_FromDate );
        }
        else if( m_ToDate != null && m_NumDays != Integer.MIN_VALUE )
        {
            String startDate = null;
            try
            {
                startDate = calcDate( m_ToDate, m_NumDays );
            }
            catch( ParseException ex )
            {
                String msg = "Error parsing date: " + m_ToDate;
                throw new TaskException( msg );
            }
            cmd.createArgument().setValue( FLAG_VERSION_DATE + m_ToDate + VALUE_FROMDATE + startDate );
        }
        else if( m_FromDate != null && m_NumDays != Integer.MIN_VALUE )
        {
            String endDate = null;
            try
            {
                endDate = calcDate( m_FromDate, m_NumDays );
            }
            catch( ParseException ex )
            {
                String msg = "Error parsing date: " + m_FromDate;
                throw new TaskException( msg );
            }
            cmd.createArgument().setValue( FLAG_VERSION_DATE + endDate + VALUE_FROMDATE + m_FromDate );
        }
        else
        {
            if( m_FromDate != null )
            {
                cmd.createArgument().setValue( FLAG_VERSION + VALUE_FROMDATE + m_FromDate );
            }
            else
            {
                cmd.createArgument().setValue( FLAG_VERSION_DATE + m_ToDate );
            }
        }
    }

    /**
     * Builds the version date command.
     *
     * @param cmd the commandline the command is to be added to
     * @exception TaskException Description of Exception
     */
    private void getVersionLabelCommand( Commandline cmd )
        throws TaskException
    {
        if( m_FromLabel == null && m_ToLabel == null )
        {
            return;
        }

        if( m_FromLabel != null && m_ToLabel != null )
        {
            cmd.createArgument().setValue( FLAG_VERSION_LABEL + m_ToLabel + VALUE_FROMLABEL + m_FromLabel );
        }
        else if( m_FromLabel != null )
        {
            cmd.createArgument().setValue( FLAG_VERSION + VALUE_FROMLABEL + m_FromLabel );
        }
        else
        {
            cmd.createArgument().setValue( FLAG_VERSION_LABEL + m_ToLabel );
        }
    }

    /**
     * Calculates the start date for version comparison. <p>
     *
     * Calculates the date numDay days earlier than startdate.
     *
     * @param fromDate Description of Parameter
     * @param numDays Description of Parameter
     * @return Description of the Returned Value
     * @exception ParseException Description of Exception
     */
    private String calcDate( String fromDate, int numDays )
        throws ParseException
    {
        String toDate = null;
        Date currdate = new Date();
        Calendar calend = new GregorianCalendar();
        currdate = m_DateFormat.parse( fromDate );
        calend.setTime( currdate );
        calend.add( Calendar.DATE, numDays );
        toDate = m_DateFormat.format( calend.getTime() );
        return toDate;
    }

    public static class BriefCodediffNofile extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"brief", "codediff", "nofile", "default"};
        }
    }

}
