/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.selftest;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * Test conversion of all the primitive types.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant:task name="prim-test"
 */
public class PrimitiveTypesTest
    extends AbstractTask
{
    public void setInteger( final Integer value )
    {
        getContext().warn( "setInteger( " + value + " );" );
    }

    public void setInteger2( final int value )
    {
        getContext().warn( "setInteger2( " + value + " );" );
    }

    public void setShort( final Short value )
    {
        getContext().warn( "setShort( " + value + " );" );
    }

    public void setShort2( final short value )
    {
        getContext().warn( "setShort2( " + value + " );" );
    }

    public void setByte( final Byte value )
    {
        getContext().warn( "setByte( " + value + " );" );
    }

    public void setByte2( final byte value )
    {
        getContext().warn( "setByte2( " + value + " );" );
    }

    public void setLong( final Long value )
    {
        getContext().warn( "setLong( " + value + " );" );
    }

    public void setLong2( final long value )
    {
        getContext().warn( "setLong2( " + value + " );" );
    }

    public void setFloat( final Float value )
    {
        getContext().warn( "setFloat( " + value + " );" );
    }

    public void setFloat2( final float value )
    {
        getContext().warn( "setFloat2( " + value + " );" );
    }

    public void setDouble( final Double value )
    {
        getContext().warn( "setDouble( " + value + " );" );
    }

    public void setDouble2( final double value )
    {
        getContext().warn( "setDouble2( " + value + " );" );
    }

    public void setString( final String value )
    {
        getContext().warn( "setString( " + value + " );" );
    }

    public void execute()
        throws TaskException
    {
    }
}
