
/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasks.core;

import org.apache.ant.AntException;
import org.apache.ant.tasklet.AbstractTasklet;

/**
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class PrimitiveTypesTest 
    extends AbstractTasklet
{
    public void setInteger( final Integer value )
    {
        getLogger().info( "setInteger( " + value + " );" );
    }
    
    public void setInteger2( final int value )
    {
        getLogger().info( "setInteger2( " + value + " );" );
    }

    public void setShort( final Short value )
    {
        getLogger().info( "setShort( " + value + " );" );
    }
    
    public void setShort2( final short value )
    {
        getLogger().info( "setShort2( " + value + " );" );
    }

    public void setByte( final Byte value )
    {
        getLogger().info( "setByte( " + value + " );" );
    }
    
    public void setByte2( final byte value )
    {
        getLogger().info( "setByte2( " + value + " );" );
    }

    public void setLong( final Long value )
    {
        getLogger().info( "setLong( " + value + " );" );
    }
    
    public void setLong2( final long value )
    {
        getLogger().info( "setLong2( " + value + " );" );
    }
    
    public void setFloat( final Float value )
    {
        getLogger().info( "setFloat( " + value + " );" );
    }
    
    public void setFloat2( final float value )
    {
        getLogger().info( "setFloat2( " + value + " );" );
    }
    
    public void setDouble( final Double value )
    {
        getLogger().info( "setDouble( " + value + " );" );
    }
    
    public void setDouble2( final double value )
    {
        getLogger().info( "setDouble2( " + value + " );" );
    }

    public void setString( final String value )
    {
        getLogger().info( "setString( " + value + " );" );
    }

    public void run()
        throws AntException
    {
    }
}
