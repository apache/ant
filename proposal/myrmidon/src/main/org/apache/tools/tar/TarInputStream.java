/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.tar;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The TarInputStream reads a UNIX tar archive as an InputStream. methods are
 * provided to position at each successive entry in the archive, and the read
 * each entry as a normal input stream using read().
 *
 * @author Timothy Gerard Endres <a href="mailto:time@ice.com">time@ice.com</a>
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 */
public class TarInputStream extends FilterInputStream
{
    protected TarBuffer buffer;
    protected TarEntry currEntry;

    protected boolean debug;
    protected int entryOffset;
    protected int entrySize;
    protected boolean hasHitEOF;
    protected byte[] oneBuf;
    protected byte[] readBuf;

    public TarInputStream( InputStream is )
    {
        this( is, TarBuffer.DEFAULT_BLKSIZE, TarBuffer.DEFAULT_RCDSIZE );
    }

    public TarInputStream( InputStream is, int blockSize )
    {
        this( is, blockSize, TarBuffer.DEFAULT_RCDSIZE );
    }

    public TarInputStream( InputStream is, int blockSize, int recordSize )
    {
        super( is );

        this.buffer = new TarBuffer( is, blockSize, recordSize );
        this.readBuf = null;
        this.oneBuf = new byte[1];
        this.debug = false;
        this.hasHitEOF = false;
    }

    /**
     * Sets the debugging flag.
     *
     * @param debug The new Debug value
     */
    public void setDebug( boolean debug )
    {
        this.debug = debug;
        this.buffer.setDebug( debug );
    }

    /**
     * Get the next entry in this tar archive. This will skip over any remaining
     * data in the current entry, if there is one, and place the input stream at
     * the header of the next entry, and read the header and instantiate a new
     * TarEntry from the header bytes and return that entry. If there are no
     * more entries in the archive, null will be returned to indicate that the
     * end of the archive has been reached.
     *
     * @return The next TarEntry in the archive, or null.
     * @exception IOException Description of Exception
     */
    public TarEntry getNextEntry()
        throws IOException
    {
        if( this.hasHitEOF )
        {
            return null;
        }

        if( this.currEntry != null )
        {
            int numToSkip = this.entrySize - this.entryOffset;

            if( this.debug )
            {
                System.err.println( "TarInputStream: SKIP currENTRY '"
                     + this.currEntry.getName() + "' SZ "
                     + this.entrySize + " OFF "
                     + this.entryOffset + "  skipping "
                     + numToSkip + " bytes" );
            }

            if( numToSkip > 0 )
            {
                this.skip( numToSkip );
            }

            this.readBuf = null;
        }

        byte[] headerBuf = this.buffer.readRecord();

        if( headerBuf == null )
        {
            if( this.debug )
            {
                System.err.println( "READ NULL RECORD" );
            }
            this.hasHitEOF = true;
        }
        else if( this.buffer.isEOFRecord( headerBuf ) )
        {
            if( this.debug )
            {
                System.err.println( "READ EOF RECORD" );
            }
            this.hasHitEOF = true;
        }

        if( this.hasHitEOF )
        {
            this.currEntry = null;
        }
        else
        {
            this.currEntry = new TarEntry( headerBuf );

            if( !( headerBuf[257] == 'u' && headerBuf[258] == 's'
                 && headerBuf[259] == 't' && headerBuf[260] == 'a'
                 && headerBuf[261] == 'r' ) )
            {
                this.entrySize = 0;
                this.entryOffset = 0;
                this.currEntry = null;

                throw new IOException( "bad header in block "
                     + this.buffer.getCurrentBlockNum()
                     + " record "
                     + this.buffer.getCurrentRecordNum()
                     + ", " +
                    "header magic is not 'ustar', but '"
                     + headerBuf[257]
                     + headerBuf[258]
                     + headerBuf[259]
                     + headerBuf[260]
                     + headerBuf[261]
                     + "', or (dec) "
                     + ( ( int )headerBuf[257] )
                     + ", "
                     + ( ( int )headerBuf[258] )
                     + ", "
                     + ( ( int )headerBuf[259] )
                     + ", "
                     + ( ( int )headerBuf[260] )
                     + ", "
                     + ( ( int )headerBuf[261] ) );
            }

            if( this.debug )
            {
                System.err.println( "TarInputStream: SET CURRENTRY '"
                     + this.currEntry.getName()
                     + "' size = "
                     + this.currEntry.getSize() );
            }

            this.entryOffset = 0;

            // REVIEW How do we resolve this discrepancy?!
            this.entrySize = ( int )this.currEntry.getSize();
        }

        if( this.currEntry != null && this.currEntry.isGNULongNameEntry() )
        {
            // read in the name
            StringBuffer longName = new StringBuffer();
            byte[] buffer = new byte[256];
            int length = 0;
            while( ( length = read( buffer ) ) >= 0 )
            {
                longName.append( new String( buffer, 0, length ) );
            }
            getNextEntry();
            this.currEntry.setName( longName.toString() );
        }

        return this.currEntry;
    }

    /**
     * Get the record size being used by this stream's TarBuffer.
     *
     * @return The TarBuffer record size.
     */
    public int getRecordSize()
    {
        return this.buffer.getRecordSize();
    }

    /**
     * Get the available data that can be read from the current entry in the
     * archive. This does not indicate how much data is left in the entire
     * archive, only in the current entry. This value is determined from the
     * entry's size header field and the amount of data already read from the
     * current entry.
     *
     * @return The number of available bytes for the current entry.
     * @exception IOException Description of Exception
     */
    public int available()
        throws IOException
    {
        return this.entrySize - this.entryOffset;
    }

    /**
     * Closes this stream. Calls the TarBuffer's close() method.
     *
     * @exception IOException Description of Exception
     */
    public void close()
        throws IOException
    {
        this.buffer.close();
    }

    /**
     * Copies the contents of the current tar archive entry directly into an
     * output stream.
     *
     * @param out The OutputStream into which to write the entry's data.
     * @exception IOException Description of Exception
     */
    public void copyEntryContents( OutputStream out )
        throws IOException
    {
        byte[] buf = new byte[32 * 1024];

        while( true )
        {
            int numRead = this.read( buf, 0, buf.length );

            if( numRead == -1 )
            {
                break;
            }

            out.write( buf, 0, numRead );
        }
    }

    /**
     * Since we do not support marking just yet, we do nothing.
     *
     * @param markLimit The limit to mark.
     */
    public void mark( int markLimit ) { }

    /**
     * Since we do not support marking just yet, we return false.
     *
     * @return False.
     */
    public boolean markSupported()
    {
        return false;
    }

    /**
     * Reads a byte from the current tar archive entry. This method simply calls
     * read( byte[], int, int ).
     *
     * @return The byte read, or -1 at EOF.
     * @exception IOException Description of Exception
     */
    public int read()
        throws IOException
    {
        int num = this.read( this.oneBuf, 0, 1 );

        if( num == -1 )
        {
            return num;
        }
        else
        {
            return ( int )this.oneBuf[0];
        }
    }

    /**
     * Reads bytes from the current tar archive entry. This method simply calls
     * read( byte[], int, int ).
     *
     * @param buf The buffer into which to place bytes read.
     * @return The number of bytes read, or -1 at EOF.
     * @exception IOException Description of Exception
     */
    public int read( byte[] buf )
        throws IOException
    {
        return this.read( buf, 0, buf.length );
    }

    /**
     * Reads bytes from the current tar archive entry. This method is aware of
     * the boundaries of the current entry in the archive and will deal with
     * them as if they were this stream's start and EOF.
     *
     * @param buf The buffer into which to place bytes read.
     * @param offset The offset at which to place bytes read.
     * @param numToRead The number of bytes to read.
     * @return The number of bytes read, or -1 at EOF.
     * @exception IOException Description of Exception
     */
    public int read( byte[] buf, int offset, int numToRead )
        throws IOException
    {
        int totalRead = 0;

        if( this.entryOffset >= this.entrySize )
        {
            return -1;
        }

        if( ( numToRead + this.entryOffset ) > this.entrySize )
        {
            numToRead = ( this.entrySize - this.entryOffset );
        }

        if( this.readBuf != null )
        {
            int sz = ( numToRead > this.readBuf.length ) ? this.readBuf.length
                 : numToRead;

            System.arraycopy( this.readBuf, 0, buf, offset, sz );

            if( sz >= this.readBuf.length )
            {
                this.readBuf = null;
            }
            else
            {
                int newLen = this.readBuf.length - sz;
                byte[] newBuf = new byte[newLen];

                System.arraycopy( this.readBuf, sz, newBuf, 0, newLen );

                this.readBuf = newBuf;
            }

            totalRead += sz;
            numToRead -= sz;
            offset += sz;
        }

        while( numToRead > 0 )
        {
            byte[] rec = this.buffer.readRecord();

            if( rec == null )
            {
                // Unexpected EOF!
                throw new IOException( "unexpected EOF with " + numToRead
                     + " bytes unread" );
            }

            int sz = numToRead;
            int recLen = rec.length;

            if( recLen > sz )
            {
                System.arraycopy( rec, 0, buf, offset, sz );

                this.readBuf = new byte[recLen - sz];

                System.arraycopy( rec, sz, this.readBuf, 0, recLen - sz );
            }
            else
            {
                sz = recLen;

                System.arraycopy( rec, 0, buf, offset, recLen );
            }

            totalRead += sz;
            numToRead -= sz;
            offset += sz;
        }

        this.entryOffset += totalRead;

        return totalRead;
    }

    /**
     * Since we do not support marking just yet, we do nothing.
     */
    public void reset() { }

    /**
     * Skip bytes in the input buffer. This skips bytes in the current entry's
     * data, not the entire archive, and will stop at the end of the current
     * entry's data if the number to skip extends beyond that point.
     *
     * @param numToSkip The number of bytes to skip.
     * @exception IOException Description of Exception
     */
    public void skip( int numToSkip )
        throws IOException
    {

        // REVIEW
        // This is horribly inefficient, but it ensures that we
        // properly skip over bytes via the TarBuffer...
        //
        byte[] skipBuf = new byte[8 * 1024];

        for( int num = numToSkip; num > 0;  )
        {
            int numRead = this.read( skipBuf, 0,
                ( num > skipBuf.length ? skipBuf.length
                 : num ) );

            if( numRead == -1 )
            {
                break;
            }

            num -= numRead;
        }
    }
}
