
/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.test;

import org.apache.ant.AntException;
import org.apache.myrmidon.api.AbstractTask;

/**
 * Test conversion of all the primitive types.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class PrimitiveTypesTest 
    extends AbstractTask
{
    public void setInteger( final Integer value )
    {
        getLogger().warn( "setInteger( " + value + " );" );
    }
    
    public void setInteger2( final int value )
    {
        getLogger().warn( "setInteger2( " + value + " );" );
    }

    public void setShort( final Short value )
    {
        getLogger().warn( "setShort( " + value + " );" );
    }
    
    public void setShort2( final short value )
    {
        getLogger().warn( "setShort2( " + value + " );" );
    }

    public void setByte( final Byte value )
    {
        getLogger().warn( "setByte( " + value + " );" );
    }
    
    public void setByte2( final byte value )
    {
        getLogger().warn( "setByte2( " + value + " );" );
    }

    public void setLong( final Long value )
    {
        getLogger().warn( "setLong( " + value + " );" );
    }
    
    public void setLong2( final long value )
    {
        getLogger().warn( "setLong2( " + value + " );" );
    }
    
    public void setFloat( final Float value )
    {
        getLogger().warn( "setFloat( " + value + " );" );
    }
    
    public void setFloat2( final float value )
    {
        getLogger().warn( "setFloat2( " + value + " );" );
    }
    
    public void setDouble( final Double value )
    {
        getLogger().warn( "setDouble( " + value + " );" );
    }
    
    public void setDouble2( final double value )
    {
        getLogger().warn( "setDouble2( " + value + " );" );
    }

    public void setString( final String value )
    {
        getLogger().warn( "setString( " + value + " );" );
    }

    public void execute()
        throws AntException
    {
    }
}
