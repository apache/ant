/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Sets TSTAMP, DSTAMP and TODAY
 *
 * @author costin@dnt.ro
 * @author stefano@apache.org
 * @author roxspring@yahoo.com
 * @author conor@cognet.com.au
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public class Tstamp extends Task
{

    private Vector customFormats = new Vector();
    private String prefix = "";

    public void setPrefix( String prefix )
    {
        this.prefix = prefix;
        if( !this.prefix.endsWith( "." ) )
        {
            this.prefix += ".";
        }
    }

    public CustomFormat createFormat()
    {
        CustomFormat cts = new CustomFormat( prefix );
        customFormats.addElement( cts );
        return cts;
    }

    public void execute()
        throws BuildException
    {
        try
        {
            Date d = new Date();

            SimpleDateFormat dstamp = new SimpleDateFormat( "yyyyMMdd" );
            project.setNewProperty( prefix + "DSTAMP", dstamp.format( d ) );

            SimpleDateFormat tstamp = new SimpleDateFormat( "HHmm" );
            project.setNewProperty( prefix + "TSTAMP", tstamp.format( d ) );

            SimpleDateFormat today = new SimpleDateFormat( "MMMM d yyyy", Locale.US );
            project.setNewProperty( prefix + "TODAY", today.format( d ) );

            Enumeration i = customFormats.elements();
            while( i.hasMoreElements() )
            {
                CustomFormat cts = ( CustomFormat )i.nextElement();
                cts.execute( project, d, location );
            }

        }
        catch( Exception e )
        {
            throw new BuildException( e );
        }
    }

    public static class Unit extends EnumeratedAttribute
    {

        private final static String MILLISECOND = "millisecond";
        private final static String SECOND = "second";
        private final static String MINUTE = "minute";
        private final static String HOUR = "hour";
        private final static String DAY = "day";
        private final static String WEEK = "week";
        private final static String MONTH = "month";
        private final static String YEAR = "year";

        private final static String[] units = {
            MILLISECOND,
            SECOND,
            MINUTE,
            HOUR,
            DAY,
            WEEK,
            MONTH,
            YEAR
            };

        private Hashtable calendarFields = new Hashtable();

        public Unit()
        {
            calendarFields.put( MILLISECOND,
                new Integer( Calendar.MILLISECOND ) );
            calendarFields.put( SECOND, new Integer( Calendar.SECOND ) );
            calendarFields.put( MINUTE, new Integer( Calendar.MINUTE ) );
            calendarFields.put( HOUR, new Integer( Calendar.HOUR_OF_DAY ) );
            calendarFields.put( DAY, new Integer( Calendar.DATE ) );
            calendarFields.put( WEEK, new Integer( Calendar.WEEK_OF_YEAR ) );
            calendarFields.put( MONTH, new Integer( Calendar.MONTH ) );
            calendarFields.put( YEAR, new Integer( Calendar.YEAR ) );
        }

        public int getCalendarField()
        {
            String key = getValue().toLowerCase();
            Integer i = ( Integer )calendarFields.get( key );
            return i.intValue();
        }

        public String[] getValues()
        {
            return units;
        }
    }

    public class CustomFormat
    {
        private int offset = 0;
        private int field = Calendar.DATE;
        private String prefix = "";
        private String country;
        private String language;
        private String pattern;
        private String propertyName;
        private TimeZone timeZone;
        private String variant;

        public CustomFormat( String prefix )
        {
            this.prefix = prefix;
        }

        public void setLocale( String locale )
        {
            StringTokenizer st = new StringTokenizer( locale, " \t\n\r\f," );
            try
            {
                language = st.nextToken();
                if( st.hasMoreElements() )
                {
                    country = st.nextToken();
                    if( st.hasMoreElements() )
                    {
                        country = st.nextToken();
                        if( st.hasMoreElements() )
                        {
                            throw new BuildException( "bad locale format", getLocation() );
                        }
                    }
                }
                else
                {
                    country = "";
                }
            }
            catch( NoSuchElementException e )
            {
                throw new BuildException( "bad locale format", e, getLocation() );
            }
        }

        public void setOffset( int offset )
        {
            this.offset = offset;
        }

        public void setPattern( String pattern )
        {
            this.pattern = pattern;
        }

        public void setProperty( String propertyName )
        {
            this.propertyName = prefix + propertyName;
        }

        public void setTimezone( String id )
        {
            timeZone = TimeZone.getTimeZone( id );
        }

        /**
         * @param unit The new Unit value
         * @deprecated setUnit(String) is deprecated and is replaced with
         *      setUnit(Tstamp.Unit) to make Ant's Introspection mechanism do
         *      the work and also to encapsulate operations on the unit in its
         *      own class.
         */
        public void setUnit( String unit )
        {
            log( "DEPRECATED - The setUnit(String) method has been deprecated."
                 + " Use setUnit(Tstamp.Unit) instead." );
            Unit u = new Unit();
            u.setValue( unit );
            field = u.getCalendarField();
        }

        public void setUnit( Unit unit )
        {
            field = unit.getCalendarField();
        }

        public void execute( Project project, Date date, Location location )
        {
            if( propertyName == null )
            {
                throw new BuildException( "property attribute must be provided", location );
            }

            if( pattern == null )
            {
                throw new BuildException( "pattern attribute must be provided", location );
            }

            SimpleDateFormat sdf;
            if( language == null )
            {
                sdf = new SimpleDateFormat( pattern );
            }
            else if( variant == null )
            {
                sdf = new SimpleDateFormat( pattern, new Locale( language, country ) );
            }
            else
            {
                sdf = new SimpleDateFormat( pattern, new Locale( language, country, variant ) );
            }
            if( offset != 0 )
            {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime( date );
                calendar.add( field, offset );
                date = calendar.getTime();
            }
            if( timeZone != null )
            {
                sdf.setTimeZone( timeZone );
            }
            project.setNewProperty( propertyName, sdf.format( date ) );
        }
    }
}
