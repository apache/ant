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
 *
 *
 * struct header {
 *		char	name[NAMSIZ];
 *		char	mode[8];
 *		char	uid[8];
 *		char	gid[8];
 *		char	size[12];
 *		char	mtime[12];
 *		char	chksum[8];
 *		char	linkflag;
 *		char	linkname[NAMSIZ];
 *		char	magic[8];
 *		char	uname[TUNMLEN];
 *		char	gname[TGNMLEN];
 *		char	devmajor[8];
 *		char	devminor[8];
 *	} header;
 *
 */

public class
TarEntry extends Object
	{
	protected TarArchive	archive;
	protected TarHeader		header;
	protected File			file;


	public
	TarEntry( TarArchive archive, File file )
		{
		this.archive = archive;
		this.file = file;
		this.header = this.getFileTarHeader( file );
		}

	public
	TarEntry( TarArchive archive, byte[] headerBuf )
		throws InvalidHeaderException
		{
		this.archive = archive;
		this.file = null;
		this.header = this.parseTarHeader( headerBuf );
		}

	public TarArchive
	getArchive()
		{
		return this.archive;
		}

	public File
	getFile()
		{
		return this.file;
		}

	public TarHeader
	getHeader()
		{
		return this.header;
		}

	public String
	getName()
		{
		return this.header.name.toString();
		}

	public long
	getSize()
		{
		return this.header.size;
		}

	public TarHeader
	getFileTarHeader( File file )
		{
		TarHeader hdr = new TarHeader();

		String name = file.getPath();
		String osname = System.getProperty( "os.name" );
		
		if ( osname != null )
			{
			if ( osname.startsWith( "macos" ) )
				{
				// UNDONE
				}
			else if ( osname.startsWith( "Windows" ) )
				{
				if ( name.length() > 2 )
					{
					char ch1 = name.charAt(0);
					char ch2 = name.charAt(1);
					if ( ch2 == File.separatorChar
						&& ( (ch1 >= 'a' && ch1 <= 'z')
							|| (ch1 >= 'a' && ch1 <= 'z') ) )
						{
						name = name.substring( 2 );
						}
					}
				}
			}

		hdr.name =
			new StringBuffer
				( name.replace( File.separatorChar, '/' ) );

		if ( file.isDirectory() )
			{
			hdr.mode = 040755;
			hdr.linkFlag = TarHeader.LF_DIR;
			hdr.name.append( "/" );
			}
		else
			{
			hdr.mode = 0100644;
			hdr.linkFlag = TarHeader.LF_NORMAL;
			}

		hdr.userId = this.archive.getUserId();
		hdr.groupId = this.archive.getGroupId();
		hdr.size = file.length();
		hdr.modTime = file.lastModified() / 1000;
		hdr.checkSum = 0;

		hdr.linkName = new StringBuffer( "" );

		hdr.magic = new StringBuffer( TarHeader.TMAGIC );

		String userName = this.archive.getUserName();

		if ( userName == null )
			userName = System.getProperty( "user.name", "" );

		if ( userName.length() > 31 )
			userName = userName.substring( 0, 32 );

		hdr.userName = new StringBuffer( userName );

		String grpName = this.archive.getGroupName();

		if ( grpName == null )
			grpName = "";

		if ( grpName.length() > 31 )
			grpName = grpName.substring( 0, 32 );

		hdr.groupName = new StringBuffer( grpName );

		hdr.devMajor = 0;
		hdr.devMinor = 0;

		return hdr;
		}

	public boolean
	isDirectory()
		{
		if ( this.file != null )
			return this.file.isDirectory();

		if ( this.header != null )
			{
			if ( this.header.linkFlag == TarHeader.LF_DIR )
				return true;

			if ( this.header.name.toString().endsWith( "/" ) )
				return true;
			}

		return false;
		}

	public TarEntry[]
	getDirectoryEntries()
		{
		if ( this.file == null
				|| ! this.file.isDirectory() )
			{
			return new TarEntry[0];
			}

		String[] list = this.file.list();

		TarEntry[] result = new TarEntry[ list.length ];

		for ( int i = 0 ; i < list.length ; ++i )
			{
			result[i] =
				new TarEntry
					( this.archive,
						new File( this.file, list[i] ) );
			}

		return result;
		}

	public long
	computeCheckSum( byte[] buf )
		{
		long sum = 0;

		for ( int i = 0 ; i < buf.length ; ++i )
			{
			sum += 255 & buf[ i ];
			}

		return sum;
		}

	public void
	writeEntryHeader( byte[] outbuf )
		{
		int offset = 0;

		offset = TarHeader.getNameBytes
			( this.header.name, outbuf, offset, TarHeader.NAMELEN );

		offset = TarHeader.getOctalBytes
			( this.header.mode, outbuf, offset, TarHeader.MODELEN );

		offset = TarHeader.getOctalBytes
			( this.header.userId, outbuf, offset, TarHeader.UIDLEN );

		offset = TarHeader.getOctalBytes
			( this.header.groupId, outbuf, offset, TarHeader.GIDLEN );

		offset = TarHeader.getLongOctalBytes
			( this.header.size, outbuf, offset, TarHeader.SIZELEN );

		offset = TarHeader.getLongOctalBytes
			( this.header.modTime, outbuf, offset, TarHeader.MODTIMELEN );

		int csOffset = offset;
		for ( int c = 0 ; c < TarHeader.CHKSUMLEN ; ++c )
			outbuf[ offset++ ] = new Byte(" ").byteValue();

    outbuf[ offset++ ] = this.header.linkFlag;

		offset = TarHeader.getNameBytes
			( this.header.linkName, outbuf, offset, TarHeader.NAMELEN );

		offset = TarHeader.getNameBytes
			( this.header.magic, outbuf, offset, TarHeader.MAGICLEN );

		offset = TarHeader.getNameBytes
			( this.header.userName, outbuf, offset, TarHeader.UNAMELEN );

		offset = TarHeader.getNameBytes
			( this.header.groupName, outbuf, offset, TarHeader.GNAMELEN );

		offset = TarHeader.getOctalBytes
			( this.header.devMajor, outbuf, offset, TarHeader.DEVLEN );

		offset = TarHeader.getOctalBytes
			( this.header.devMinor, outbuf, offset, TarHeader.DEVLEN );

		long checkSum = this.computeCheckSum( outbuf );

		TarHeader.getCheckSumOctalBytes
			( checkSum, outbuf, csOffset, TarHeader.CHKSUMLEN );
		}

	public void
	writeEntryContents( TarBuffer buffer )
		throws IOException
		{
		if ( this.file == null )
			throw new IOException( "file is null" );

		if ( ! this.file.exists() )
			throw new IOException
				( "file '" + this.file.getPath()
					+ "' does not exist" );

		// UNDONE - handle ASCII line termination translation!!!!

		FileInputStream in =
			new FileInputStream( this.file );

		int recSize = this.archive.getRecordSize();

		byte[] recbuf = new byte[ recSize ];

		for ( ; ; )
			{
			int num = in.read( recbuf, 0, recSize );
			if ( num == -1 )
				break;

			if ( num < recSize )
				{
				for ( int j = num ; j < recSize ; ++j )
					recbuf[j] = 0;
				}

			buffer.writeRecord( recbuf );
			}

		in.close();
		}

	public TarHeader
	parseTarHeader( byte[] header )
		throws InvalidHeaderException
		{
		TarHeader hdr = new TarHeader();

		int offset = 0;

		hdr.name =
			TarHeader.parseName( header, offset, TarHeader.NAMELEN );

		offset += TarHeader.NAMELEN;

		hdr.mode = (int)
			TarHeader.parseOctal( header, offset, TarHeader.MODELEN );

		offset += TarHeader.MODELEN;

		hdr.userId = (int)
			TarHeader.parseOctal( header, offset, TarHeader.UIDLEN );

		offset += TarHeader.UIDLEN;

		hdr.groupId = (int)
			TarHeader.parseOctal( header, offset, TarHeader.GIDLEN );

		offset += TarHeader.GIDLEN;

		hdr.size =
			TarHeader.parseOctal( header, offset, TarHeader.SIZELEN );

		offset += TarHeader.SIZELEN;

		hdr.modTime =
			TarHeader.parseOctal( header, offset, TarHeader.MODTIMELEN );

		offset += TarHeader.MODTIMELEN;

		hdr.checkSum = (int)
			TarHeader.parseOctal( header, offset, TarHeader.CHKSUMLEN );

		offset += TarHeader.CHKSUMLEN;

		hdr.linkFlag = header[ offset++ ];

		hdr.linkName =
			TarHeader.parseName( header, offset, TarHeader.NAMELEN );

		offset += TarHeader.NAMELEN;

		hdr.magic =
			TarHeader.parseName( header, offset, TarHeader.MAGICLEN );

		offset += TarHeader.MAGICLEN;

		hdr.userName =
			TarHeader.parseName( header, offset, TarHeader.UNAMELEN );

		offset += TarHeader.UNAMELEN;

		hdr.groupName =
			TarHeader.parseName( header, offset, TarHeader.GNAMELEN );

		offset += TarHeader.GNAMELEN;

		hdr.devMajor = (int)
			TarHeader.parseOctal( header, offset, TarHeader.DEVLEN );

		offset += TarHeader.DEVLEN;

		hdr.devMinor = (int)
			TarHeader.parseOctal( header, offset, TarHeader.DEVLEN );

		return hdr;
		}

	}

