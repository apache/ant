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
 * @ant.task name="prim-test"
 */
public class PrimitiveTypesTest
    extends AbstractTask
{
    public void setInteger( final Integer value )
    {
        getContext().info( "setInteger( " + value + " );" );
    }

    public void setInteger2( final int value )
    {
        getContext().info( "setInteger2( " + value + " );" );
    }

    public void setShort( final Short value )
    {
        getContext().info( "setShort( " + value + " );" );
    }

    public void setShort2( final short value )
    {
        getContext().info( "setShort2( " + value + " );" );
    }

    public void setByte( final Byte value )
    {
        getContext().info( "setByte( " + value + " );" );
    }

    public void setByte2( final byte value )
    {
        getContext().info( "setByte2( " + value + " );" );
    }

    public void setLong( final Long value )
    {
        getContext().info( "setLong( " + value + " );" );
    }

    public void setLong2( final long value )
    {
        getContext().info( "setLong2( " + value + " );" );
    }

    public void setFloat( final Float value )
    {
        getContext().info( "setFloat( " + value + " );" );
    }

    public void setFloat2( final float value )
    {
        getContext().info( "setFloat2( " + value + " );" );
    }

    public void setDouble( final Double value )
    {
        getContext().info( "setDouble( " + value + " );" );
    }

    public void setDouble2( final double value )
    {
        getContext().info( "setDouble2( " + value + " );" );
    }

    public void setString( final String value )
    {
        getContext().info( "setString( " + value + " );" );
    }

    public void execute()
        throws TaskException
    {
    }
}
