/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.tar;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The TarOutputStream writes a UNIX tar archive as an OutputStream. Methods are
 * provided to put entries, and then write their contents by writing to this
 * stream using write().
 *
 * @author Timothy Gerard Endres <a href="mailto:time@ice.com">time@ice.com</a>
 */
public class TarOutputStream extends FilterOutputStream
{
    public final static int LONGFILE_ERROR = 0;
    public final static int LONGFILE_TRUNCATE = 1;
    public final static int LONGFILE_GNU = 2;
    protected int longFileMode = LONGFILE_ERROR;
    protected byte[] assemBuf;
    protected int assemLen;
    protected TarBuffer buffer;
    protected int currBytes;
    protected int currSize;

    protected boolean debug;
    protected byte[] oneBuf;
    protected byte[] recordBuf;

    public TarOutputStream( OutputStream os )
    {
        this( os, TarBuffer.DEFAULT_BLKSIZE, TarBuffer.DEFAULT_RCDSIZE );
    }

    public TarOutputStream( OutputStream os, int blockSize )
    {
        this( os, blockSize, TarBuffer.DEFAULT_RCDSIZE );
    }

    public TarOutputStream( OutputStream os, int blockSize, int recordSize )
    {
        super( os );

        this.buffer = new TarBuffer( os, blockSize, recordSize );
        this.debug = false;
        this.assemLen = 0;
        this.assemBuf = new byte[ recordSize ];
        this.recordBuf = new byte[ recordSize ];
        this.oneBuf = new byte[ 1 ];
    }

    /**
     * Sets the debugging flag in this stream's TarBuffer.
     *
     * @param debug The new BufferDebug value
     */
    public void setBufferDebug( boolean debug )
    {
        this.buffer.setDebug( debug );
    }

    /**
     * Sets the debugging flag.
     *
     * @param debugF True to turn on debugging.
     */
    public void setDebug( boolean debugF )
    {
        this.debug = debugF;
    }

    public void setLongFileMode( int longFileMode )
    {
        this.longFileMode = longFileMode;
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
     * Ends the TAR archive and closes the underlying OutputStream. This means
     * that finish() is called followed by calling the TarBuffer's close().
     *
     * @exception IOException Description of Exception
     */
    public void close()
        throws IOException
    {
        this.finish();
        this.buffer.close();
    }

    /**
     * Close an entry. This method MUST be called for all file entries that
     * contain data. The reason is that we must buffer data written to the
     * stream in order to satisfy the buffer's record based writes. Thus, there
     * may be data fragments still being assembled that must be written to the
     * output stream before this entry is closed and the next entry written.
     *
     * @exception IOException Description of Exception
     */
    public void closeEntry()
        throws IOException
    {
        if( this.assemLen > 0 )
        {
            for( int i = this.assemLen; i < this.assemBuf.length; ++i )
            {
                this.assemBuf[ i ] = 0;
            }

            this.buffer.writeRecord( this.assemBuf );

            this.currBytes += this.assemLen;
            this.assemLen = 0;
        }

        if( this.currBytes < this.currSize )
        {
            throw new IOException( "entry closed at '" + this.currBytes
                                   + "' before the '" + this.currSize
                                   + "' bytes specified in the header were written" );
        }
    }

    /**
     * Ends the TAR archive without closing the underlying OutputStream. The
     * result is that the EOF record of nulls is written.
     *
     * @exception IOException Description of Exception
     */
    public void finish()
        throws IOException
    {
        this.writeEOFRecord();
    }

    /**
     * Put an entry on the output stream. This writes the entry's header record
     * and positions the output stream for writing the contents of the entry.
     * Once this method is called, the stream is ready for calls to write() to
     * write the entry's contents. Once the contents are written, closeEntry()
     * <B>MUST</B> be called to ensure that all buffered data is completely
     * written to the output stream.
     *
     * @param entry The TarEntry to be written to the archive.
     * @exception IOException Description of Exception
     */
    public void putNextEntry( TarEntry entry )
        throws IOException
    {
        if( entry.getName().length() >= TarConstants.NAMELEN )
        {

            if( longFileMode == LONGFILE_GNU )
            {
                // create a TarEntry for the LongLink, the contents
                // of which are the entry's name
                TarEntry longLinkEntry = new TarEntry( TarConstants.GNU_LONGLINK,
                                                       TarConstants.LF_GNUTYPE_LONGNAME );

                longLinkEntry.setSize( entry.getName().length() + 1 );
                putNextEntry( longLinkEntry );
                write( entry.getName().getBytes() );
                write( 0 );
                closeEntry();
            }
            else if( longFileMode != LONGFILE_TRUNCATE )
            {
                throw new RuntimeException( "file name '" + entry.getName()
                                            + "' is too long ( > "
                                            + TarConstants.NAMELEN + " bytes)" );
            }
        }

        entry.writeEntryHeader( this.recordBuf );
        this.buffer.writeRecord( this.recordBuf );

        this.currBytes = 0;

        if( entry.isDirectory() )
        {
            this.currSize = 0;
        }
        else
        {
            this.currSize = (int)entry.getSize();
        }
    }

    /**
     * Writes a byte to the current tar archive entry. This method simply calls
     * read( byte[], int, int ).
     *
     * @param b The byte written.
     * @exception IOException Description of Exception
     */
    public void write( int b )
        throws IOException
    {
        this.oneBuf[ 0 ] = (byte)b;

        this.write( this.oneBuf, 0, 1 );
    }

    /**
     * Writes bytes to the current tar archive entry. This method simply calls
     * write( byte[], int, int ).
     *
     * @param wBuf The buffer to write to the archive.
     * @exception IOException Description of Exception
     */
    public void write( byte[] wBuf )
        throws IOException
    {
        this.write( wBuf, 0, wBuf.length );
    }

    /**
     * Writes bytes to the current tar archive entry. This method is aware of
     * the current entry and will throw an exception if you attempt to write
     * bytes past the length specified for the current entry. The method is also
     * (painfully) aware of the record buffering required by TarBuffer, and
     * manages buffers that are not a multiple of recordsize in length,
     * including assembling records from small buffers.
     *
     * @param wBuf The buffer to write to the archive.
     * @param wOffset The offset in the buffer from which to get bytes.
     * @param numToWrite The number of bytes to write.
     * @exception IOException Description of Exception
     */
    public void write( byte[] wBuf, int wOffset, int numToWrite )
        throws IOException
    {
        if( ( this.currBytes + numToWrite ) > this.currSize )
        {
            throw new IOException( "request to write '" + numToWrite
                                   + "' bytes exceeds size in header of '"
                                   + this.currSize + "' bytes" );
            //
            // We have to deal with assembly!!!
            // The programmer can be writing little 32 byte chunks for all
            // we know, and we must assemble complete records for writing.
            // REVIEW Maybe this should be in TarBuffer? Could that help to
            // eliminate some of the buffer copying.
            //
        }

        if( this.assemLen > 0 )
        {
            if( ( this.assemLen + numToWrite ) >= this.recordBuf.length )
            {
                int aLen = this.recordBuf.length - this.assemLen;

                System.arraycopy( this.assemBuf, 0, this.recordBuf, 0,
                                  this.assemLen );
                System.arraycopy( wBuf, wOffset, this.recordBuf,
                                  this.assemLen, aLen );
                this.buffer.writeRecord( this.recordBuf );

                this.currBytes += this.recordBuf.length;
                wOffset += aLen;
                numToWrite -= aLen;
                this.assemLen = 0;
            }
            else
            {
                System.arraycopy( wBuf, wOffset, this.assemBuf, this.assemLen,
                                  numToWrite );

                wOffset += numToWrite;
                this.assemLen += numToWrite;
                numToWrite -= numToWrite;
            }
        }

        //
        // When we get here we have EITHER:
        // o An empty "assemble" buffer.
        // o No bytes to write (numToWrite == 0)
        //
        while( numToWrite > 0 )
        {
            if( numToWrite < this.recordBuf.length )
            {
                System.arraycopy( wBuf, wOffset, this.assemBuf, this.assemLen,
                                  numToWrite );

                this.assemLen += numToWrite;

                break;
            }

            this.buffer.writeRecord( wBuf, wOffset );

            int num = this.recordBuf.length;

            this.currBytes += num;
            numToWrite -= num;
            wOffset += num;
        }
    }

    /**
     * Write an EOF (end of archive) record to the tar archive. An EOF record
     * consists of a record of all zeros.
     *
     * @exception IOException Description of Exception
     */
    private void writeEOFRecord()
        throws IOException
    {
        for( int i = 0; i < this.recordBuf.length; ++i )
        {
            this.recordBuf[ i ] = 0;
        }

        this.buffer.writeRecord( this.recordBuf );
    }
}

