/*
** Copyright (c) 1998 by Timothy Gerard Endres
** <mailto:time@ice.com>  <http://www.ice.com>
** 
** This package is free software.
** 
** You may redistribute it and/or modify it under the terms of the GNU
** General Public License as published by the Free Software Foundation.
** Version 2 of the license should be included with this distribution in
** the file LICENSE, as well as License.html. If the license is not
** included	with this distribution, you may find a copy at the FSF web
** site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
** Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
**
** THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
** NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
** OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
** CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
** REDISTRIBUTION OF THIS SOFTWARE. 
** 
*/

package com.ice.tar;

import java.io.*;


/**
 * The TarBuffer class implements the tar archive concept
 * of a buffered input stream. This concept goes back to the
 * days of blocked tape drives and special io devices. In the
 * Java universe, the only real function that this class
 * performs is to ensure that files have the correct "block"
 * size, or other tars will complain.
 * <p>
 * You should never have a need to access this class directly.
 * TarBuffers are created by TarArchives, which in turn provide
 * several methods to allow you access to the buffer.
 *
 * @version $Revision$
 * @author Timothy Gerard Endres,
 *  <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see TarArchive
 */

public class
TarBuffer extends Object
	{
	public static final int		DEFAULT_BLKSIZE = ( 512 * 20 );

	private InputStream		inStream;
	private OutputStream	outStream;

	private byte[]	blockBuffer;
	private int		currBlkIdx;
	private int		currRecIdx;
	private int		blockSize;
	private int		recordSize;
	private int		recsPerBlock;

	private boolean	debug;


	public
	TarBuffer( TarArchive archive, InputStream inStream )
		{
		this( archive, inStream, TarBuffer.DEFAULT_BLKSIZE );
		}

	public
	TarBuffer( TarArchive archive, InputStream inStream, int blockSize )
		{
		this.inStream = inStream;
		this.outStream = null;
		this.initialize( archive, blockSize );
		}

	public
	TarBuffer( TarArchive archive, OutputStream outStream )
		{
		this( archive, outStream, TarBuffer.DEFAULT_BLKSIZE );
		}

	public
	TarBuffer( TarArchive archive, OutputStream outStream, int blockSize )
		{
		this.inStream = null;
		this.outStream = outStream;
		this.initialize( archive, blockSize );
		}

	public void
	initialize( TarArchive archive, int blockSize )
		{
		this.debug = false;
		this.blockSize = blockSize;
		this.recordSize = archive.getRecordSize();
		this.recsPerBlock = ( this.blockSize / this.recordSize );
		this.blockBuffer = new byte[ this.blockSize ];

		if ( inStream != null )
			{
			this.currBlkIdx = -1;
			this.currRecIdx = this.recsPerBlock;
			}
		else
			{
			this.currBlkIdx = 0;
			this.currRecIdx = 0;
			}
		}

	public void
	setDebug( boolean debug )
		{
		this.debug = debug;
		}

	public void
	skipBytes( int bytes )
		{
		for ( int num = bytes ; num > 0 ; )
			{
			try { this.skipRecord(); }
			catch ( IOException ex )
				{
				break;
				}
			num -= this.recordSize;
			}
		}

	public void
	skipRecord()
		throws IOException
		{
		if ( this.debug )
			{
			System.err.println
				( "SkipRecord: recIdx = " + this.currRecIdx
					+ " blkIdx = " + this.currBlkIdx );
			}

		if ( this.currRecIdx >= this.recsPerBlock )
			{
			if ( ! this.readBlock() )
				return; // UNDONE
			}

		this.currRecIdx++;
		}

	public byte[]
	readRecord()
		throws IOException
		{
		if ( this.debug )
			{
			System.err.println
				( "ReadRecord: recIdx = " + this.currRecIdx
					+ " blkIdx = " + this.currBlkIdx );
			}

		if ( this.currRecIdx >= this.recsPerBlock )
			{
			if ( ! this.readBlock() )
				return null;
			}

		byte[] result = new byte[ this.recordSize ];

		System.arraycopy(
			this.blockBuffer, (this.currRecIdx * this.recordSize),
			result, 0, this.recordSize );

		this.currRecIdx++;

		return result;
		}

	/**
	 * @return false if End-Of-File, else true
	 */

	public boolean
	readBlock()
		throws IOException
		{
		if ( this.debug )
			{
			System.err.println
				( "ReadBlock: blkIdx = " + this.currBlkIdx );
			}

		if ( this.inStream == null )
			throw new IOException( "input stream is null" );

		this.currRecIdx = 0;

		int offset = 0;
		int bytesNeeded = this.blockSize;
		for ( ; bytesNeeded > 0 ; )
			{
			long numBytes =
				this.inStream.read
					( this.blockBuffer, offset, bytesNeeded );

			if ( numBytes == -1 )
				return false;

			offset += numBytes;
			bytesNeeded -= numBytes;
			if ( numBytes != this.blockSize )
				{
				if ( this.debug )
					{
					System.err.println
						( "ReadBlock: INCOMPLETE READ " + numBytes
							+ " of " + this.blockSize + " bytes read." );
					}
				}
			}

		this.currBlkIdx++;

		return true;
		}

	public int
	getCurrentBlockNum()
		{
		return this.currBlkIdx;
		}

	public int
	getCurrentRecordNum()
		{
		return this.currRecIdx - 1;
		}

	public void
	writeRecord( byte[] record )
		throws IOException
		{
		if ( this.debug )
			{
			System.err.println
				( "WriteRecord: recIdx = " + this.currRecIdx
					+ " blkIdx = " + this.currBlkIdx );
			}

		if ( this.currRecIdx >= this.recsPerBlock )
			{
			this.writeBlock();
			}

		System.arraycopy(
			record, 0,
			this.blockBuffer, (this.currRecIdx * this.recordSize),
			this.recordSize );

		this.currRecIdx++;
		}

	public void
	writeBlock()
		throws IOException
		{
		if ( this.debug )
			{
			System.err.println
				( "WriteBlock: blkIdx = " + this.currBlkIdx );
			}

		if ( this.outStream == null )
			throw new IOException( "output stream is null" );

		this.outStream.write( this.blockBuffer, 0, this.blockSize );

		this.currRecIdx = 0;
		this.currBlkIdx++;
		}

	public void
	flushBlock()
		throws IOException
		{
		if ( this.debug )
			{
			System.err.println( "TarBuffer.flushBlock() called." );
			}

		if ( this.outStream != null )
			{
			if ( this.currRecIdx > 0 )
				{
				this.writeBlock();
				}
			}
		}

	public void
	closeBuffer()
		throws IOException
		{
		if ( this.debug )
			{
			System.err.println( "TarBuffer.closeBuffer()." );
			}

		if ( this.outStream != null )
			{
			if ( this.outStream != System.out
					&& this.outStream != System.err )
				{
				this.outStream.close();
				this.outStream = null;
				}
			}
		else if ( this.inStream != null )
			{
			if ( this.inStream != System.in )
				{
				this.inStream.close();
				this.inStream = null;
				}
			}
		}

	}

