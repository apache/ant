/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;
import org.apache.myrmidon.api.TaskException;
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

    private ArrayList customFormats = new ArrayList();
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
        customFormats.add( cts );
        return cts;
    }

    public void execute()
        throws TaskException
    {
        try
        {
            Date d = new Date();

            SimpleDateFormat dstamp = new SimpleDateFormat( "yyyyMMdd" );
            setProperty( prefix + "DSTAMP", dstamp.format( d ) );

            SimpleDateFormat tstamp = new SimpleDateFormat( "HHmm" );
            setProperty( prefix + "TSTAMP", tstamp.format( d ) );

            SimpleDateFormat today = new SimpleDateFormat( "MMMM d yyyy", Locale.US );
            setProperty( prefix + "TODAY", today.format( d ) );

            Iterator i = customFormats.iterator();
            while( i.hasNext() )
            {
                CustomFormat cts = (CustomFormat)i.next();
                cts.execute( getProject(), d );
            }

        }
        catch( Exception e )
        {
            throw new TaskException( "Error", e );
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
            Integer i = (Integer)calendarFields.get( key );
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
            throws TaskException
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
                            throw new TaskException( "bad locale format" );
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
                throw new TaskException( "bad locale format", e );
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

        public void setUnit( Unit unit )
        {
            field = unit.getCalendarField();
        }

        public void execute( Project project, Date date )
            throws TaskException
        {
            if( propertyName == null )
            {
                throw new TaskException( "property attribute must be provided" );
            }

            if( pattern == null )
            {
                throw new TaskException( "pattern attribute must be provided" );
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
            getContext().setProperty( propertyName, sdf.format( date ) );
        }
    }
}
