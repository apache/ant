/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.zip;
import java.util.zip.CRC32;
import java.util.zip.ZipException;

/**
 * Adds Unix file permission and UID/GID fields as well as symbolic link
 * handling. <p>
 *
 * This class uses the ASi extra field in the format: <pre>
 *         Value         Size            Description
 *         -----         ----            -----------
 * (Unix3) 0x756e        Short           tag for this extra block type
 *         TSize         Short           total data size for this block
 *         CRC           Long            CRC-32 of the remaining data
 *         Mode          Short           file permissions
 *         SizDev        Long            symlink'd size OR major/minor dev num
 *         UID           Short           user ID
 *         GID           Short           group ID
 *         (var.)        variable        symbolic link filename
 * </pre> taken from appnote.iz (Info-ZIP note, 981119) found at <a
 * href="ftp://ftp.uu.net/pub/archiving/zip/doc/">
 * ftp://ftp.uu.net/pub/archiving/zip/doc/</a> </p> <p>
 *
 * Short is two bytes and Long is four bytes in big endian byte and word order,
 * device numbers are currently not supported.</p>
 *
 * @author <a href="stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public class AsiExtraField implements ZipExtraField, UnixStat, Cloneable
{

    private final static ZipShort HEADER_ID = new ZipShort( 0x756E );

    /**
     * Standard Unix stat(2) file mode.
     *
     * @since 1.1
     */
    private int mode = 0;
    /**
     * User ID.
     *
     * @since 1.1
     */
    private int uid = 0;
    /**
     * Group ID.
     *
     * @since 1.1
     */
    private int gid = 0;
    /**
     * File this entry points to, if it is a symbolic link. <p>
     *
     * empty string - if entry is not a symbolic link.</p>
     *
     * @since 1.1
     */
    private String link = "";
    /**
     * Is this an entry for a directory?
     *
     * @since 1.1
     */
    private boolean dirFlag = false;

    /**
     * Instance used to calculate checksums.
     *
     * @since 1.1
     */
    private CRC32 crc = new CRC32();

    public AsiExtraField() { }

    /**
     * Indicate whether this entry is a directory.
     *
     * @param dirFlag The new Directory value
     * @since 1.1
     */
    public void setDirectory( boolean dirFlag )
    {
        this.dirFlag = dirFlag;
        mode = getMode( mode );
    }

    /**
     * Set the group id.
     *
     * @param gid The new GroupId value
     * @since 1.1
     */
    public void setGroupId( int gid )
    {
        this.gid = gid;
    }

    /**
     * Indicate that this entry is a symbolic link to the given filename.
     *
     * @param name Name of the file this entry links to, empty String if it is
     *      not a symbolic link.
     * @since 1.1
     */
    public void setLinkedFile( String name )
    {
        link = name;
        mode = getMode( mode );
    }

    /**
     * File mode of this file.
     *
     * @param mode The new Mode value
     * @since 1.1
     */
    public void setMode( int mode )
    {
        this.mode = getMode( mode );
    }

    /**
     * Set the user id.
     *
     * @param uid The new UserId value
     * @since 1.1
     */
    public void setUserId( int uid )
    {
        this.uid = uid;
    }

    /**
     * Delegate to local file data.
     *
     * @return The CentralDirectoryData value
     * @since 1.1
     */
    public byte[] getCentralDirectoryData()
    {
        return getLocalFileDataData();
    }

    /**
     * Delegate to local file data.
     *
     * @return The CentralDirectoryLength value
     * @since 1.1
     */
    public ZipShort getCentralDirectoryLength()
    {
        return getLocalFileDataLength();
    }

    /**
     * Get the group id.
     *
     * @return The GroupId value
     * @since 1.1
     */
    public int getGroupId()
    {
        return gid;
    }

    /**
     * The Header-ID.
     *
     * @return The HeaderId value
     * @since 1.1
     */
    public ZipShort getHeaderId()
    {
        return HEADER_ID;
    }

    /**
     * Name of linked file
     *
     * @return name of the file this entry links to if it is a symbolic link,
     *      the empty string otherwise.
     * @since 1.1
     */
    public String getLinkedFile()
    {
        return link;
    }

    /**
     * The actual data to put into local file data - without Header-ID or length
     * specifier.
     *
     * @return The LocalFileDataData value
     * @since 1.1
     */
    public byte[] getLocalFileDataData()
    {
        // CRC will be added later
        byte[] data = new byte[getLocalFileDataLength().getValue() - 4];
        System.arraycopy( ( new ZipShort( getMode() ) ).getBytes(), 0, data, 0, 2 );

        byte[] linkArray = getLinkedFile().getBytes();
        System.arraycopy( ( new ZipLong( linkArray.length ) ).getBytes(),
            0, data, 2, 4 );

        System.arraycopy( ( new ZipShort( getUserId() ) ).getBytes(),
            0, data, 6, 2 );
        System.arraycopy( ( new ZipShort( getGroupId() ) ).getBytes(),
            0, data, 8, 2 );

        System.arraycopy( linkArray, 0, data, 10, linkArray.length );

        crc.reset();
        crc.update( data );
        long checksum = crc.getValue();

        byte[] result = new byte[data.length + 4];
        System.arraycopy( ( new ZipLong( checksum ) ).getBytes(), 0, result, 0, 4 );
        System.arraycopy( data, 0, result, 4, data.length );
        return result;
    }

    /**
     * Length of the extra field in the local file data - without Header-ID or
     * length specifier.
     *
     * @return The LocalFileDataLength value
     * @since 1.1
     */
    public ZipShort getLocalFileDataLength()
    {
        return new ZipShort( 4// CRC
         + 2// Mode
         + 4// SizDev
         + 2// UID
         + 2// GID
         + getLinkedFile().getBytes().length );
    }

    /**
     * File mode of this file.
     *
     * @return The Mode value
     * @since 1.1
     */
    public int getMode()
    {
        return mode;
    }

    /**
     * Get the user id.
     *
     * @return The UserId value
     * @since 1.1
     */
    public int getUserId()
    {
        return uid;
    }

    /**
     * Is this entry a directory?
     *
     * @return The Directory value
     * @since 1.1
     */
    public boolean isDirectory()
    {
        return dirFlag && !isLink();
    }

    /**
     * Is this entry a symbolic link?
     *
     * @return The Link value
     * @since 1.1
     */
    public boolean isLink()
    {
        return getLinkedFile().length() != 0;
    }

    /**
     * Populate data from this array as if it was in local file data.
     *
     * @param data Description of Parameter
     * @param offset Description of Parameter
     * @param length Description of Parameter
     * @exception ZipException Description of Exception
     * @since 1.1
     */
    public void parseFromLocalFileData( byte[] data, int offset, int length )
        throws ZipException
    {

        long givenChecksum = ( new ZipLong( data, offset ) ).getValue();
        byte[] tmp = new byte[length - 4];
        System.arraycopy( data, offset + 4, tmp, 0, length - 4 );
        crc.reset();
        crc.update( tmp );
        long realChecksum = crc.getValue();
        if( givenChecksum != realChecksum )
        {
            throw new ZipException( "bad CRC checksum "
                 + Long.toHexString( givenChecksum )
                 + " instead of "
                 + Long.toHexString( realChecksum ) );
        }

        int newMode = ( new ZipShort( tmp, 0 ) ).getValue();
        byte[] linkArray = new byte[( int )( new ZipLong( tmp, 2 ) ).getValue()];
        uid = ( new ZipShort( tmp, 6 ) ).getValue();
        gid = ( new ZipShort( tmp, 8 ) ).getValue();

        if( linkArray.length == 0 )
        {
            link = "";
        }
        else
        {
            System.arraycopy( tmp, 10, linkArray, 0, linkArray.length );
            link = new String( linkArray );
        }
        setDirectory( ( newMode & DIR_FLAG ) != 0 );
        setMode( newMode );
    }

    /**
     * Get the file mode for given permissions with the correct file type.
     *
     * @param mode Description of Parameter
     * @return The Mode value
     * @since 1.1
     */
    protected int getMode( int mode )
    {
        int type = FILE_FLAG;
        if( isLink() )
        {
            type = LINK_FLAG;
        }
        else if( isDirectory() )
        {
            type = DIR_FLAG;
        }
        return type | ( mode & PERM_MASK );
    }

}
