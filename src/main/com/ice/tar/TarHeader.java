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


public class
TarHeader extends Object
	{
	public static final int		NAMELEN = 100;
	public static final int		MODELEN = 8;
	public static final int		UIDLEN = 8;
	public static final int		GIDLEN = 8;
	public static final int		CHKSUMLEN = 8;
	public static final int		SIZELEN = 12;
	public static final int		MAGICLEN = 8;
	public static final int		MODTIMELEN = 12;
	public static final int		UNAMELEN = 32;
	public static final int		GNAMELEN = 32;
	public static final int		DEVLEN = 8;

	public static final byte	LF_OLDNORM	= 0;
	public static final byte	LF_NORMAL	= new Byte("0").byteValue();
	public static final byte	LF_LINK		= new Byte("1").byteValue();
	public static final byte	LF_SYMLINK	= new Byte("2").byteValue();
	public static final byte	LF_CHR		= new Byte("3").byteValue();
	public static final byte	LF_BLK		= new Byte("4").byteValue();
	public static final byte	LF_DIR		= new Byte("5").byteValue();
	public static final byte	LF_FIFO		= new Byte("6").byteValue();
	public static final byte	LF_CONTIG	= new Byte("7").byteValue();

	public static final String	TMAGIC		= "ustar  ";

	public StringBuffer		name;
	public int				mode;
	public int				userId;
	public int				groupId;
	public long				size;
	public long				modTime;
	public int				checkSum;
	public byte				linkFlag;
	public StringBuffer		linkName;
	public StringBuffer		magic;
	public StringBuffer		userName;
	public StringBuffer		groupName;
	public int				devMajor;
	public int				devMinor;

	public
	TarHeader()
		{
		}

	public static long
	parseOctal( byte[] header, int offset, int length )
		throws InvalidHeaderException
		{
		long result = 0;
		boolean stillPadding = true;

		int end = offset + length;
		for ( int i = offset ; i < end ; ++i )
			{
			if ( header[i] == 0 )
				break;

			if ( header[i] == ' ' || header[i] == '0' )
				{
				if ( stillPadding )
					continue;

				if ( header[i] == ' ' )
					break;
				}
			
			stillPadding = false;

			result =
				(result << 3)
					+ (header[i] - '0');
			}

		return result;
		}

	public static StringBuffer
	parseName( byte[] header, int offset, int length )
		throws InvalidHeaderException
		{
		StringBuffer result = new StringBuffer( length );

		int end = offset + length;
		for ( int i = offset ; i < end ; ++i )
			{
			if ( header[i] == 0 )
				break;
			result.append( (char)header[i] );
			}

		return result;
		}

	public static int
	getNameBytes( StringBuffer name, byte[] buf, int offset, int length )
		{
		int i;

		for ( i = 0 ; i < length && i < name.length() ; ++i )
			{
			buf[ offset + i ] = (byte)name.charAt( i );
			}

		for ( ; i < length ; ++i )
			{
			buf[ offset + i ] = 0;
			}

		return offset + length;
		}

	public static int
	getOctalBytes( long value, byte[] buf, int offset, int length )
		{
		byte[] result = new byte[ length ];

		int idx = length - 1;

		buf[ offset + idx ] = 0;
		--idx;
		buf[ offset + idx ] = new Byte(" ").byteValue();
		--idx;

		if ( value == 0 )
			{
			buf[ offset + idx ] = new Byte("0").byteValue();
			--idx;
			}
		else
			{
			for ( long val = value ; idx >= 0 && val > 0 ; --idx )
				{
				buf[ offset + idx ] =
					(byte) ( '0' + (val & 7) );
				val = val >> 3;
				}
			}

		for ( ; idx >= 0 ; --idx )
			{
			buf[ offset + idx ] = new Byte(" ").byteValue();
			}

		return offset + length;
		}

	public static int
	getLongOctalBytes( long value, byte[] buf, int offset, int length )
		{
		byte[] temp = new byte[ length + 1 ];
		TarHeader.getOctalBytes( value, temp, 0, length + 1 );
		System.arraycopy( temp, 0, buf, offset, length );
		return offset + length;
		}

	public static int
	getCheckSumOctalBytes( long value, byte[] buf, int offset, int length )
		{
		TarHeader.getOctalBytes( value, buf, offset, length );
		buf[ offset + length - 1 ] = new Byte(" ").byteValue();
		buf[ offset + length - 2 ] = 0;
		return offset + length;
		}

	public String
	getName()
		{
		return this.name.toString();
		}

	}
 
