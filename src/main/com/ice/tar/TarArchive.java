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
 * The TarArchive class implements the concept of a
 * tar archive. A tar archive is a series of entries, each of
 * which represents a file system object. Each entry in
 * the archive consists of a header record. Directory entries
 * consist only of the header record, and are followed by entries
 * for the directory's contents. File entries consist of a
 * header record followed by the number of records needed to
 * contain the file's contents. All entries are written on
 * record boundaries. Records are 512 bytes long.
 *
 * TarArchives are instantiated in either read or write mode,
 * based upon whether they are instantiated with an InputStream
 * or an OutputStream. Once instantiated TarArchives read/write
 * mode can not be changed.
 *
 * There is currently no support for random access to tar archives.
 * However, it seems that subclassing TarArchive, and using the
 * TarBuffer.getCurrentRecordNum() and TarBuffer.getCurrentBlockNum()
 * methods, this would be rather trvial.
 *
 * @version $Revision$
 * @author Timothy Gerard Endres,
 *  <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see TarBuffer
 * @see TarHeader
 * @see TarEntry
 */


public class
TarArchive extends Object
	{
	public static final int		RECORDSIZE = 512;

	protected boolean			verbose;
	protected boolean			debug;
	protected boolean			keepOldFiles;

	protected int				userId;
	protected String			userName;
	protected int				groupId;
	protected String			groupName;

	protected String			pathPrefix;

	protected int				recordSize;
	protected byte[]			recordBuf;

	protected TarBuffer			buffer;

	protected TarProgressDisplay	progressDisplay;


	public
	TarArchive( InputStream inStream )
		{
		this( inStream, TarBuffer.DEFAULT_BLKSIZE );
		}

	public
	TarArchive( InputStream inStream, int blockSize )
		{
		this( inStream, blockSize, TarArchive.RECORDSIZE );
		}

	public
	TarArchive( InputStream inStream, int blockSize, int recordSize )
		{
		this.initialize( recordSize );
		this.buffer = new TarBuffer( this, inStream, blockSize );
		}

	public
	TarArchive( OutputStream outStream )
		{
		this( outStream, TarBuffer.DEFAULT_BLKSIZE );
		}

	public
	TarArchive( OutputStream outStream, int blockSize )
		{
		this( outStream, blockSize, TarArchive.RECORDSIZE );
		}

	public
	TarArchive( OutputStream outStream, int blockSize, int recordSize )
		{
		this.initialize( recordSize );
		this.buffer = new TarBuffer( this, outStream, blockSize );
		}

	public void
	initialize( int recordSize )
		{
		this.pathPrefix = null;
		this.recordSize = recordSize;
		this.recordBuf = new byte[ recordSize ];

		this.userId = 0;
		this.userName = "";
		this.groupId = 0;
		this.groupName = "";

		this.debug = false;
		this.verbose = false;
		this.keepOldFiles = false;
		this.progressDisplay = null;
		}

	public void
	setDebug( boolean debugF )
		{
		this.debug = debugF;
		}

	public void
	setBufferDebug( boolean debug )
		{
		this.buffer.setDebug( debug );
		}

	public boolean
	isVerbose()
		{
		return this.verbose;
		}

	public void
	setVerbose( boolean verbose )
		{
		this.verbose = verbose;
		}

	public void
	setTarProgressDisplay( TarProgressDisplay display )
		{
		this.progressDisplay = display;
		}

	public void
	setKeepOldFiles( boolean keepOldFiles )
		{
		this.keepOldFiles = keepOldFiles;
		}

	public void
	setUserInfo(
			int userId, String userName,
			int groupId, String groupName )
		{
		this.userId = userId;
		this.userName = userName;
		this.groupId = groupId;
		this.groupName = groupName;
		}

	public int
	getUserId()
		{
		return this.userId;
		}

	public String
	getUserName()
		{
		return this.userName;
		}

	public int
	getGroupId()
		{
		return this.groupId;
		}

	public String
	getGroupName()
		{
		return this.groupName;
		}

	public void
	closeArchive()
		throws IOException
		{
		this.buffer.flushBlock();
		this.buffer.closeBuffer();
		}

	public int
	getRecordSize()
		{
		return this.recordSize;
		}

	public TarEntry
	parseArchive()
		{
		return null;
		}

	public TarEntry
	parseEntry()
		{
		return null;
		}

	public void
	extractArchive()
		{
		}

	public void
	listContents()
		throws IOException, InvalidHeaderException
		{
		TarEntry	entry;
		byte[]		headerBuf;

		for ( ; ; )
			{
			headerBuf = this.buffer.readRecord();
			if ( headerBuf == null )
				{
				if ( this.debug )
					{
					System.err.println( "READ NULL RECORD" );
					}
				break;
				}

			if ( this.isEOFRecord( headerBuf ) )
				{
				if ( this.debug )
					{
					System.err.println( "READ EOF RECORD" );
					}
				break;
				}

			try {
				entry = new TarEntry( this, headerBuf );
				}
			catch ( InvalidHeaderException ex )
				{
				throw new InvalidHeaderException
					( "bad header in block "
						+ this.buffer.getCurrentBlockNum()
						+ " record "
						+ this.buffer.getCurrentRecordNum() );
				}

			if ( this.progressDisplay != null )
				this.progressDisplay.showTarProgressMessage
					( entry.getName() );

			this.buffer.skipBytes( (int)entry.getSize() );
			}
		}

	public void
	extractContents( File destDir )
		throws IOException, InvalidHeaderException
		{
		TarEntry	entry;
		byte[]		headerBuf;

		for ( ; ; )
			{
			headerBuf = this.buffer.readRecord();
			if ( headerBuf == null )
				{
				if ( this.debug )
					{
					System.err.println( "READ NULL RECORD" );
					}
				break;
				}

			if ( this.isEOFRecord( headerBuf ) )
				{
				if ( this.debug )
					{
					System.err.println( "READ EOF RECORD" );
					}
				break;
				}

			try {
				entry = new TarEntry( this, headerBuf );
				}
			catch ( InvalidHeaderException ex )
				{
				throw new InvalidHeaderException
					( "bad header in block "
						+ this.buffer.getCurrentBlockNum()
						+ " record "
						+ this.buffer.getCurrentRecordNum() );
				}

			this.extractEntry( destDir, entry );
			}
		}

	public void
	extractEntry( File destDir, TarEntry entry )
		throws IOException
		{
		if ( this.verbose )
			{
			if ( this.progressDisplay != null )
				this.progressDisplay.showTarProgressMessage
					( entry.getName() );
			}

		File subDir =
			new File( destDir, entry.getName() );

		if ( entry.isDirectory() )
			{
			if ( ! subDir.exists() )
				{
				if ( ! subDir.mkdirs() )
					{
					throw new IOException
						( "error making directory path '"
							+ subDir.getPath() + "'" );
					}
				}
			}
		else
			{
			String name = entry.getName().toString();
			name = name.replace( '/', File.separatorChar );

			File destFile = new File( destDir, name );

			if ( this.keepOldFiles && destFile.exists() )
				{
				if ( this.verbose )
					{
					if ( this.progressDisplay != null )
						this.progressDisplay.showTarProgressMessage
							( "not overwriting " + entry.getName() );
					}
				}
			else
				{
				FileOutputStream out =
					new FileOutputStream( destFile );

				for ( int num = (int)entry.getSize() ; num > 0 ; )
					{
					byte[] record = this.buffer.readRecord();

					int wNum =
						( num < record.length )
							? num : record.length;

					out.write( record, 0, wNum );

					num -= wNum;
					}

				out.close();
				}
			}
		}

	public boolean
	isEOFRecord( byte[] record )
		{
		for ( int i = 0 ; i < this.recordSize ; ++i )
			if ( record[i] != 0 )
				return false;

		return true;
		}

	public void
	writeEOFRecord()
		throws IOException
		{
		for ( int i = 0 ; i < this.recordSize ; ++i )
			this.recordBuf[i] = 0;
		this.buffer.writeRecord( this.recordBuf );
		}

	public void
	writeEntry( TarEntry entry, boolean recurse )
		throws IOException
		{
		if ( this.verbose )
			{
			if ( this.progressDisplay != null )
				this.progressDisplay.showTarProgressMessage
					( entry.getName() );
			}

		entry.writeEntryHeader( this.recordBuf );
		this.buffer.writeRecord( this.recordBuf );

		if ( entry.isDirectory() )
			{
			TarEntry[] list = entry.getDirectoryEntries();

			for ( int i = 0 ; i < list.length ; ++i )
				{
				this.writeEntry( list[i], recurse );
				}
			}
		else
			{
			entry.writeEntryContents( this.buffer );
			}
		}

	public TarEntry
	readEntry()
		throws IOException, InvalidHeaderException
		{
		TarEntry result = null;

		byte[] header = this.readRecord();

		TarEntry entry = new TarEntry( this, header );

		return entry;
		}

	public byte[]
	readRecord()
		throws IOException
		{
		return this.buffer.readRecord();
		}

	public void
	writeRecord( byte[] record )
		throws IOException
		{
		this.buffer.writeRecord( record );
		}

	}

