/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.zip;

/**
 * Simple placeholder for all those extra fields we don't want to deal with. <p>
 *
 * Assumes local file data and central directory entries are identical - unless
 * told the opposite.</p>
 *
 * @author <a href="stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public class UnrecognizedExtraField implements ZipExtraField
{

    /**
     * Extra field data in central directory - without Header-ID or length
     * specifier.
     *
     * @since 1.1
     */
    private byte[] centralData;

    /**
     * The Header-ID.
     *
     * @since 1.1
     */
    private ZipShort headerId;

    /**
     * Extra field data in local file data - without Header-ID or length
     * specifier.
     *
     * @since 1.1
     */
    private byte[] localData;

    public void setCentralDirectoryData( byte[] data )
    {
        centralData = data;
    }

    public void setHeaderId( ZipShort headerId )
    {
        this.headerId = headerId;
    }

    public void setLocalFileDataData( byte[] data )
    {
        localData = data;
    }

    public byte[] getCentralDirectoryData()
    {
        if( centralData != null )
        {
            return centralData;
        }
        return getLocalFileDataData();
    }

    public ZipShort getCentralDirectoryLength()
    {
        if( centralData != null )
        {
            return new ZipShort( centralData.length );
        }
        return getLocalFileDataLength();
    }

    public ZipShort getHeaderId()
    {
        return headerId;
    }

    public byte[] getLocalFileDataData()
    {
        return localData;
    }

    public ZipShort getLocalFileDataLength()
    {
        return new ZipShort( localData.length );
    }

    public void parseFromLocalFileData( byte[] data, int offset, int length )
    {
        byte[] tmp = new byte[ length ];
        System.arraycopy( data, offset, tmp, 0, length );
        setLocalFileDataData( tmp );
    }
}
