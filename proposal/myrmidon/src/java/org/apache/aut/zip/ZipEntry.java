/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.zip;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.zip.ZipException;

/**
 * Extension that adds better handling of extra fields and provides access to
 * the internal and external file attributes.
 *
 * @author <a href="stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public class ZipEntry
    extends java.util.zip.ZipEntry
{
    /**
     * Helper for JDK 1.1
     *
     * @since 1.2
     */
    private static Method setCompressedSizeMethod = null;
    /**
     * Helper for JDK 1.1
     *
     * @since 1.2
     */
    private static Object lockReflection = new Object();
    /**
     * Helper for JDK 1.1
     *
     * @since 1.2
     */
    private static boolean triedToGetMethod = false;

    private int internalAttributes = 0;
    private long externalAttributes = 0;
    private ArrayList extraFields = new ArrayList();

    /**
     * Helper for JDK 1.1 <-> 1.2 incompatibility.
     *
     * @since 1.2
     */
    private Long compressedSize = null;

    /**
     * Creates a new zip entry with the specified name.
     *
     * @param name Description of Parameter
     * @since 1.1
     */
    public ZipEntry( String name )
    {
        super( name );
    }

    /**
     * Creates a new zip entry with fields taken from the specified zip entry.
     *
     * @param entry Description of Parameter
     * @exception ZipException Description of Exception
     * @since 1.1
     */
    public ZipEntry( java.util.zip.ZipEntry entry )
        throws ZipException
    {
        /*
         * REVISIT: call super(entry) instead of this stuff in Ant2,
         * "copy constructor" has not been available in JDK 1.1
         */
        super( entry.getName() );

        setComment( entry.getComment() );
        setMethod( entry.getMethod() );
        setTime( entry.getTime() );

        long size = entry.getSize();
        if( size > 0 )
        {
            setSize( size );
        }
        long cSize = entry.getCompressedSize();
        if( cSize > 0 )
        {
            setComprSize( cSize );
        }
        long crc = entry.getCrc();
        if( crc > 0 )
        {
            setCrc( crc );
        }

        byte[] extra = entry.getExtra();
        if( extra != null )
        {
            setExtraFields( ExtraFieldUtils.parse( extra ) );
        }
        else
        {
            // initializes extra data to an empty byte array
            setExtra();
        }
    }

    /**
     * Creates a new zip entry with fields taken from the specified zip entry.
     *
     * @param entry Description of Parameter
     * @exception ZipException Description of Exception
     * @since 1.1
     */
    public ZipEntry( ZipEntry entry )
        throws ZipException
    {
        this( (java.util.zip.ZipEntry)entry );
        setInternalAttributes( entry.getInternalAttributes() );
        setExternalAttributes( entry.getExternalAttributes() );
        setExtraFields( entry.getExtraFields() );
    }

    /**
     * Try to get a handle to the setCompressedSize method.
     *
     * @since 1.2
     */
    private static void checkSCS()
    {
        if( !triedToGetMethod )
        {
            synchronized( lockReflection )
            {
                triedToGetMethod = true;
                try
                {
                    setCompressedSizeMethod =
                        java.util.zip.ZipEntry.class.getMethod( "setCompressedSize",
                                                                new Class[]{Long.TYPE} );
                }
                catch( NoSuchMethodException nse )
                {
                }
            }
        }
    }

    /**
     * Are we running JDK 1.2 or higher?
     *
     * @return Description of the Returned Value
     * @since 1.2
     */
    private static boolean haveSetCompressedSize()
    {
        checkSCS();
        return setCompressedSizeMethod != null;
    }

    /**
     * Invoke setCompressedSize via reflection.
     *
     * @param ze Description of Parameter
     * @param size Description of Parameter
     * @since 1.2
     */
    private static void performSetCompressedSize( ZipEntry ze, long size )
    {
        Long[] s = {new Long( size )};
        try
        {
            setCompressedSizeMethod.invoke( ze, s );
        }
        catch( InvocationTargetException ite )
        {
            Throwable nested = ite.getTargetException();
            throw new RuntimeException( "Exception setting the compressed size "
                                        + "of " + ze + ": "
                                        + nested.getMessage() );
        }
        catch( Throwable other )
        {
            throw new RuntimeException( "Exception setting the compressed size "
                                        + "of " + ze + ": "
                                        + other.getMessage() );
        }
    }

    /**
     * Make this class work in JDK 1.1 like a 1.2 class. <p>
     *
     * This either stores the size for later usage or invokes setCompressedSize
     * via reflection.</p>
     *
     * @param size The new ComprSize value
     * @since 1.2
     */
    public void setComprSize( long size )
    {
        if( haveSetCompressedSize() )
        {
            performSetCompressedSize( this, size );
        }
        else
        {
            compressedSize = new Long( size );
        }
    }

    /**
     * Sets the external file attributes.
     *
     * @param value The new ExternalAttributes value
     * @since 1.1
     */
    public void setExternalAttributes( long value )
    {
        externalAttributes = value;
    }

    /**
     * Throws an Exception if extra data cannot be parsed into extra fields.
     *
     * @param extra The new Extra value
     * @exception RuntimeException Description of Exception
     * @since 1.1
     */
    public void setExtra( byte[] extra )
        throws RuntimeException
    {
        try
        {
            setExtraFields( ExtraFieldUtils.parse( extra ) );
        }
        catch( Exception e )
        {
            throw new RuntimeException( e.getMessage() );
        }
    }

    /**
     * Replaces all currently attached extra fields with the new array.
     *
     * @param fields The new ExtraFields value
     * @since 1.1
     */
    public void setExtraFields( ZipExtraField[] fields )
    {
        extraFields.clear();
        for( int i = 0; i < fields.length; i++ )
        {
            extraFields.add( fields[ i ] );
        }
        setExtra();
    }

    /**
     * Sets the internal file attributes.
     *
     * @param value The new InternalAttributes value
     * @since 1.1
     */
    public void setInternalAttributes( int value )
    {
        internalAttributes = value;
    }

    /**
     * Retrieves the extra data for the central directory.
     *
     * @return The CentralDirectoryExtra value
     * @since 1.1
     */
    public byte[] getCentralDirectoryExtra()
    {
        return ExtraFieldUtils.mergeCentralDirectoryData( getExtraFields() );
    }

    /**
     * Override to make this class work in JDK 1.1 like a 1.2 class.
     *
     * @return The CompressedSize value
     * @since 1.2
     */
    public long getCompressedSize()
    {
        if( compressedSize != null )
        {
            // has been set explicitly and we are running in a 1.1 VM
            return compressedSize.longValue();
        }
        return super.getCompressedSize();
    }

    /**
     * Retrieves the external file attributes.
     *
     * @return The ExternalAttributes value
     * @since 1.1
     */
    public long getExternalAttributes()
    {
        return externalAttributes;
    }

    /**
     * Retrieves extra fields.
     *
     * @return The ExtraFields value
     * @since 1.1
     */
    public ZipExtraField[] getExtraFields()
    {
        final ZipExtraField[] result = new ZipExtraField[ extraFields.size() ];
        return (ZipExtraField[])extraFields.toArray( result );
    }

    /**
     * Retrieves the internal file attributes.
     *
     * @return The InternalAttributes value
     * @since 1.1
     */
    public int getInternalAttributes()
    {
        return internalAttributes;
    }

    /**
     * Retrieves the extra data for the local file data.
     *
     * @return The LocalFileDataExtra value
     * @since 1.1
     */
    public byte[] getLocalFileDataExtra()
    {
        byte[] extra = getExtra();
        return extra != null ? extra : new byte[ 0 ];
    }

    /**
     * Adds an extra fields - replacing an already present extra field of the
     * same type.
     *
     * @param ze The feature to be added to the ExtraField attribute
     * @since 1.1
     */
    public void addExtraField( ZipExtraField ze )
    {
        ZipShort type = ze.getHeaderId();
        boolean done = false;
        for( int i = 0; !done && i < extraFields.size(); i++ )
        {
            if( ( (ZipExtraField)extraFields.get( i ) ).getHeaderId().equals( type ) )
            {
                extraFields.set( i, ze );
                done = true;
            }
        }
        if( !done )
        {
            extraFields.add( ze );
        }
        setExtra();
    }

    /**
     * Overwrite clone
     *
     * @return Description of the Returned Value
     * @since 1.1
     */
    public Object clone()
    {
        ZipEntry e = null;
        try
        {
            e = new ZipEntry( (java.util.zip.ZipEntry)super.clone() );
        }
        catch( Exception ex )
        {
            // impossible as extra data is in correct format
            ex.printStackTrace();
        }
        e.setInternalAttributes( getInternalAttributes() );
        e.setExternalAttributes( getExternalAttributes() );
        e.setExtraFields( getExtraFields() );
        return e;
    }

    /**
     * Remove an extra fields.
     *
     * @param type Description of Parameter
     * @since 1.1
     */
    public void removeExtraField( ZipShort type )
    {
        boolean done = false;
        for( int i = 0; !done && i < extraFields.size(); i++ )
        {
            if( ( (ZipExtraField)extraFields.get( i ) ).getHeaderId().equals( type ) )
            {
                extraFields.remove( i );
                done = true;
            }
        }
        if( !done )
        {
            throw new java.util.NoSuchElementException();
        }
        setExtra();
    }

    /**
     * Unfortunately {@link java.util.zip.ZipOutputStream
     * java.util.zip.ZipOutputStream} seems to access the extra data directly,
     * so overriding getExtra doesn't help - we need to modify super's data
     * directly.
     *
     * @since 1.1
     */
    protected void setExtra()
    {
        super.setExtra( ExtraFieldUtils.mergeLocalFileDataData( getExtraFields() ) );
    }

}
