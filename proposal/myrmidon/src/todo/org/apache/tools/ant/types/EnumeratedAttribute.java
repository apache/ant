/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import org.apache.myrmidon.api.TaskException;

/**
 * Helper class for attributes that can only take one of a fixed list of values.
 * <p>
 *
 * See {@link org.apache.tools.ant.taskdefs.FixCRLF FixCRLF} for an example.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public abstract class EnumeratedAttribute
{

    protected String value;

    public EnumeratedAttribute()
    {
    }

    /**
     * Invoked by {@link org.apache.tools.ant.IntrospectionHelper
     * IntrospectionHelper}.
     *
     * @param value The new Value value
     * @exception TaskException Description of Exception
     */
    public final void setValue( String value )
        throws TaskException
    {
        if( !containsValue( value ) )
        {
            throw new TaskException( value + " is not a legal value for this attribute" );
        }
        this.value = value;
    }

    /**
     * Retrieves the value.
     *
     * @return The Value value
     */
    public final String getValue()
    {
        return value;
    }

    /**
     * This is the only method a subclass needs to implement.
     *
     * @return an array holding all possible values of the enumeration.
     */
    public abstract String[] getValues();

    /**
     * Is this value included in the enumeration?
     *
     * @param value Description of Parameter
     * @return Description of the Returned Value
     */
    public final boolean containsValue( String value )
    {
        String[] values = getValues();
        if( values == null || value == null )
        {
            return false;
        }

        for( int i = 0; i < values.length; i++ )
        {
            if( value.equals( values[ i ] ) )
            {
                return true;
            }
        }
        return false;
    }
}
