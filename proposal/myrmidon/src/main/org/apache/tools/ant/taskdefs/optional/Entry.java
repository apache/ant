/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Instance of this class represents nested elements of a task propertyfile.
 */
public class Entry
{
    final static String NOW_VALUE_ = "now";
    final static String NULL_VALUE_ = "never";

    private final static int DEFAULT_INT_VALUE = 1;
    private final static GregorianCalendar
        DEFAULT_DATE_VALUE = new GregorianCalendar();

    private String m_key;
    private int m_type = Type.STRING_TYPE;
    private int m_operation = Operation.EQUALS_OPER;
    private String m_value = "";
    private String m_default;
    private String m_pattern;

    public void setDefault( String value )
    {
        this.m_default = value;
    }

    public void setKey( String value )
    {
        this.m_key = value;
    }

    public void setOperation( Operation value )
    {
        int newOperation = Operation.toOperation( value.getValue() );
        if( newOperation == Operation.NOW_VALUE )
        {
            this.m_operation = Operation.EQUALS_OPER;
            this.setValue( this.NOW_VALUE_ );
        }
        else if( newOperation == Operation.NULL_VALUE )
        {
            this.m_operation = Operation.EQUALS_OPER;
            this.setValue( this.NULL_VALUE_ );
        }
        else
        {
            this.m_operation = newOperation;
        }
    }

    public void setPattern( String value )
    {
        this.m_pattern = value;
    }

    public void setType( Type value )
    {
        this.m_type = Type.toType( value.getValue() );
    }

    public void setValue( String value )
    {
        this.m_value = value;
    }

    protected void executeOn( Properties props )
        throws TaskException
    {
        checkParameters();

        // m_type may be null because it wasn't set
        try
        {
            if( m_type == Type.INTEGER_TYPE )
            {
                executeInteger( (String)props.get( m_key ) );
            }
            else if( m_type == Type.DATE_TYPE )
            {
                executeDate( (String)props.get( m_key ) );
            }
            else if( m_type == Type.STRING_TYPE )
            {
                executeString( (String)props.get( m_key ) );
            }
            else
            {
                throw new TaskException( "Unknown operation type: " + m_type + "" );
            }
        }
        catch( NullPointerException npe )
        {
            // Default to string type
            // which means do nothing
            npe.printStackTrace();
        }
        // Insert as a string by default
        props.put( m_key, m_value );

    }

    /**
     * Check if parameter combinations can be supported
     *
     * @exception TaskException Description of Exception
     */
    private void checkParameters()
        throws TaskException
    {
        if( m_type == Type.STRING_TYPE &&
            m_operation == Operation.DECREMENT_OPER )
        {
            throw new TaskException( "- is not suported for string properties (key:" + m_key + ")" );
        }
        if( m_value == null && m_default == null )
        {
            throw new TaskException( "value and/or default must be specified (key:" + m_key + ")" );
        }
        if( m_key == null )
        {
            throw new TaskException( "key is mandatory" );
        }
        if( m_type == Type.STRING_TYPE &&
            m_pattern != null )
        {
            throw new TaskException( "pattern is not suported for string properties (key:" + m_key + ")" );
        }
    }

    /**
     * Handle operations for type <code>date</code>.
     *
     * @param oldValue the current value read from the property file or
     *      <code>null</code> if the <code>key</code> was not contained in
     *      the property file.
     * @exception TaskException Description of Exception
     */
    private void executeDate( String oldValue )
        throws TaskException
    {
        GregorianCalendar value = new GregorianCalendar();
        GregorianCalendar newValue = new GregorianCalendar();

        if( m_pattern == null ) {
            m_pattern = "yyyy/MM/dd HH:mm";
        }
        DateFormat fmt = new SimpleDateFormat( m_pattern );

        // special case
        if( m_default != null &&
            NOW_VALUE_.equals( m_default.toLowerCase() ) &&
            ( m_operation == Operation.INCREMENT_OPER ||
            m_operation == Operation.DECREMENT_OPER ) )
        {
            oldValue = null;
        }

        if( oldValue != null )
        {
            try
            {
                value.setTime( fmt.parse( oldValue ) );
            }
            catch( ParseException pe )
            {
                /*
                 * swollow
                 */
            }
        }

        if( m_value != null )
        {
            if( NOW_VALUE_.equals( m_value.toLowerCase() ) )
            {
                value.setTime( new Date() );
            }
            else if( NULL_VALUE_.equals( m_value.toLowerCase() ) )
            {
                value = null;
            }
            else
            {
                try
                {
                    value.setTime( fmt.parse( m_value ) );
                }
                catch( Exception ex )
                {
                    // obviously not a date, try a simple int
                    try
                    {
                        int offset = Integer.parseInt( m_value );
                        value.clear();
                        value.set( Calendar.DAY_OF_YEAR, offset );
                    }
                    catch( Exception ex_ )
                    {
                        value.clear();
                        value.set( Calendar.DAY_OF_YEAR, 1 );
                    }
                }

            }
        }

        if( m_default != null && oldValue == null )
        {
            if( NOW_VALUE_.equals( m_default.toLowerCase() ) )
            {
                value.setTime( new Date() );
            }
            else if( NULL_VALUE_.equals( m_default.toLowerCase() ) )
            {
                value = null;
            }
            else
            {
                try
                {
                    value.setTime( fmt.parse( m_default ) );
                }
                catch( ParseException pe )
                {
                    /*
                     * swollow
                     */
                }
            }
        }

        if( m_operation == Operation.EQUALS_OPER )
        {
            newValue = value;
        }
        else if( m_operation == Operation.INCREMENT_OPER )
        {
            newValue.add( Calendar.SECOND, value.get( Calendar.SECOND ) );
            newValue.add( Calendar.MINUTE, value.get( Calendar.MINUTE ) );
            newValue.add( Calendar.HOUR_OF_DAY, value.get( Calendar.HOUR_OF_DAY ) );
            newValue.add( Calendar.DAY_OF_YEAR, value.get( Calendar.DAY_OF_YEAR ) );
        }
        else if( m_operation == Operation.DECREMENT_OPER )
        {
            newValue.add( Calendar.SECOND, -1 * value.get( Calendar.SECOND ) );
            newValue.add( Calendar.MINUTE, -1 * value.get( Calendar.MINUTE ) );
            newValue.add( Calendar.HOUR_OF_DAY, -1 * value.get( Calendar.HOUR_OF_DAY ) );
            newValue.add( Calendar.DAY_OF_YEAR, -1 * value.get( Calendar.DAY_OF_YEAR ) );
        }
        if( newValue != null )
        {
            m_value = fmt.format( newValue.getTime() );
        }
        else
        {
            m_value = "";
        }
    }

    /**
     * Handle operations for type <code>int</code>.
     *
     * @param oldValue the current value read from the property file or
     *      <code>null</code> if the <code>key</code> was not contained in
     *      the property file.
     * @exception TaskException Description of Exception
     */
    private void executeInteger( String oldValue )
        throws TaskException
    {
        int value = 0;
        int newValue = 0;

        DecimalFormat fmt = ( m_pattern != null ) ? new DecimalFormat( m_pattern )
            : new DecimalFormat();

        if( oldValue != null )
        {
            try
            {
                value = fmt.parse( oldValue ).intValue();
            }
            catch( NumberFormatException nfe )
            {
                /*
                 * swollow
                 */
            }
            catch( ParseException pe )
            {
                /*
                 * swollow
                 */
            }
        }
        if( m_value != null )
        {
            try
            {
                value = fmt.parse( m_value ).intValue();
            }
            catch( NumberFormatException nfe )
            {
                /*
                 * swollow
                 */
            }
            catch( ParseException pe )
            {
                /*
                 * swollow
                 */
            }
        }
        if( m_default != null && oldValue == null )
        {
            try
            {
                value = fmt.parse( m_default ).intValue();
            }
            catch( NumberFormatException nfe )
            {
                /*
                 * swollow
                 */
            }
            catch( ParseException pe )
            {
                /*
                 * swollow
                 */
            }
        }

        if( m_operation == Operation.EQUALS_OPER )
        {
            newValue = value;
        }
        else if( m_operation == Operation.INCREMENT_OPER )
        {
            newValue = ++value;
        }
        else if( m_operation == Operation.DECREMENT_OPER )
        {
            newValue = --value;
        }
        m_value = fmt.format( newValue );
    }

    /**
     * Handle operations for type <code>string</code>.
     *
     * @param oldValue the current value read from the property file or
     *      <code>null</code> if the <code>key</code> was not contained in
     *      the property file.
     * @exception TaskException Description of Exception
     */
    private void executeString( String oldValue )
        throws TaskException
    {
        String value = "";
        String newValue = "";

        // the order of events is, of course, very important here
        // default initially to the old value
        if( oldValue != null )
        {
            value = oldValue;
        }
        // but if a value is specified, use it
        if( m_value != null )
        {
            value = m_value;
        }
        // even if value is specified, ignore it and set to the default
        // value if it is specified and there is no previous value
        if( m_default != null && oldValue == null )
        {
            value = m_default;
        }

        if( m_operation == Operation.EQUALS_OPER )
        {
            newValue = value;
        }
        else if( m_operation == Operation.INCREMENT_OPER )
        {
            newValue += value;
        }
        m_value = newValue;
    }

    /**
     * Enumerated attribute with the values "+", "-", "=", "now" and
     * "never".
     *
     * @author RT
     */
    public static class Operation extends EnumeratedAttribute
    {

        // Property type operations
        public final static int INCREMENT_OPER = 0;
        public final static int DECREMENT_OPER = 1;
        public final static int EQUALS_OPER = 2;

        // Special values
        public final static int NOW_VALUE = 3;
        public final static int NULL_VALUE = 4;

        public static int toOperation( String oper )
        {
            if( "+".equals( oper ) )
            {
                return INCREMENT_OPER;
            }
            else if( "-".equals( oper ) )
            {
                return DECREMENT_OPER;
            }
            else if( NOW_VALUE_.equals( oper ) )
            {
                return NOW_VALUE;
            }
            else if( NULL_VALUE_.equals( oper ) )
            {
                return NULL_VALUE;
            }
            return EQUALS_OPER;
        }

        public String[] getValues()
        {
            return new String[]{"+", "-", "=", NOW_VALUE_, NULL_VALUE_};
        }
    }

    /**
     * Enumerated attribute with the values "int", "date" and "string".
     *
     * @author RT
     */
    public static class Type extends EnumeratedAttribute
    {

        // Property types
        public final static int INTEGER_TYPE = 0;
        public final static int DATE_TYPE = 1;
        public final static int STRING_TYPE = 2;

        public static int toType( String type )
        {
            if( "int".equals( type ) )
            {
                return INTEGER_TYPE;
            }
            else if( "date".equals( type ) )
            {
                return DATE_TYPE;
            }
            return STRING_TYPE;
        }

        public String[] getValues()
        {
            return new String[]{"int", "date", "string"};
        }
    }
}
