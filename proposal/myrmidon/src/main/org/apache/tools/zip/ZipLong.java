/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.zip;

/**
 * Utility class that represents a four byte integer with conversion rules for
 * the big endian byte order of ZIP files.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public class ZipLong implements Cloneable
{

    private long value;

    /**
     * Create instance from a number.
     *
     * @param value Description of Parameter
     * @since 1.1
     */
    public ZipLong( long value )
    {
        this.value = value;
    }

    /**
     * Create instance from bytes.
     *
     * @param bytes Description of Parameter
     * @since 1.1
     */
    public ZipLong( byte[] bytes )
    {
        this( bytes, 0 );
    }

    /**
     * Create instance from the four bytes starting at offset.
     *
     * @param bytes Description of Parameter
     * @param offset Description of Parameter
     * @since 1.1
     */
    public ZipLong( byte[] bytes, int offset )
    {
        value = ( bytes[offset + 3] << 24 ) & 0xFF000000l;
        value += ( bytes[offset + 2] << 16 ) & 0xFF0000;
        value += ( bytes[offset + 1] << 8 ) & 0xFF00;
        value += ( bytes[offset] & 0xFF );
    }

    /**
     * Get value as two bytes in big endian byte order.
     *
     * @return The Bytes value
     * @since 1.1
     */
    public byte[] getBytes()
    {
        byte[] result = new byte[4];
        result[0] = ( byte )( ( value & 0xFF ) );
        result[1] = ( byte )( ( value & 0xFF00 ) >> 8 );
        result[2] = ( byte )( ( value & 0xFF0000 ) >> 16 );
        result[3] = ( byte )( ( value & 0xFF000000l ) >> 24 );
        return result;
    }

    /**
     * Get value as Java int.
     *
     * @return The Value value
     * @since 1.1
     */
    public long getValue()
    {
        return value;
    }

    /**
     * Override to make two instances with same value equal.
     *
     * @param o Description of Parameter
     * @return Description of the Returned Value
     * @since 1.1
     */
    public boolean equals( Object o )
    {
        if( o == null || !( o instanceof ZipLong ) )
        {
            return false;
        }
        return value == ( ( ZipLong )o ).getValue();
    }

    /**
     * Override to make two instances with same value equal.
     *
     * @return Description of the Returned Value
     * @since 1.1
     */
    public int hashCode()
    {
        return ( int )value;
    }

}// ZipLong
