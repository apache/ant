/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.zip;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.zip.ZipException;

/**
 * ZipExtraField related methods
 *
 * @author <a href="stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public class ExtraFieldUtils
{

    /**
     * Static registry of known extra fields.
     *
     * @since 1.1
     */
    private static Hashtable implementations;

    static
    {
        implementations = new Hashtable();
        register( AsiExtraField.class );
    }

    /**
     * Create an instance of the approriate ExtraField, falls back to {@link
     * UnrecognizedExtraField UnrecognizedExtraField}.
     *
     * @param headerId Description of Parameter
     * @return Description of the Returned Value
     * @exception InstantiationException Description of Exception
     * @exception IllegalAccessException Description of Exception
     * @since 1.1
     */
    public static ZipExtraField createExtraField( ZipShort headerId )
        throws InstantiationException, IllegalAccessException
    {
        Class c = (Class)implementations.get( headerId );
        if( c != null )
        {
            return (ZipExtraField)c.newInstance();
        }
        UnrecognizedExtraField u = new UnrecognizedExtraField();
        u.setHeaderId( headerId );
        return u;
    }

    /**
     * Merges the central directory fields of the given ZipExtraFields.
     *
     * @param data Description of Parameter
     * @return Description of the Returned Value
     * @since 1.1
     */
    public static byte[] mergeCentralDirectoryData( ZipExtraField[] data )
    {
        int sum = 4 * data.length;
        for( int i = 0; i < data.length; i++ )
        {
            sum += data[ i ].getCentralDirectoryLength().getValue();
        }
        byte[] result = new byte[ sum ];
        int start = 0;
        for( int i = 0; i < data.length; i++ )
        {
            System.arraycopy( data[ i ].getHeaderId().getBytes(),
                              0, result, start, 2 );
            System.arraycopy( data[ i ].getCentralDirectoryLength().getBytes(),
                              0, result, start + 2, 2 );
            byte[] local = data[ i ].getCentralDirectoryData();
            System.arraycopy( local, 0, result, start + 4, local.length );
            start += ( local.length + 4 );
        }
        return result;
    }

    /**
     * Merges the local file data fields of the given ZipExtraFields.
     *
     * @param data Description of Parameter
     * @return Description of the Returned Value
     * @since 1.1
     */
    public static byte[] mergeLocalFileDataData( ZipExtraField[] data )
    {
        int sum = 4 * data.length;
        for( int i = 0; i < data.length; i++ )
        {
            sum += data[ i ].getLocalFileDataLength().getValue();
        }
        byte[] result = new byte[ sum ];
        int start = 0;
        for( int i = 0; i < data.length; i++ )
        {
            System.arraycopy( data[ i ].getHeaderId().getBytes(),
                              0, result, start, 2 );
            System.arraycopy( data[ i ].getLocalFileDataLength().getBytes(),
                              0, result, start + 2, 2 );
            byte[] local = data[ i ].getLocalFileDataData();
            System.arraycopy( local, 0, result, start + 4, local.length );
            start += ( local.length + 4 );
        }
        return result;
    }

    /**
     * Split the array into ExtraFields and populate them with the give data.
     *
     * @param data Description of Parameter
     * @return Description of the Returned Value
     * @exception ZipException Description of Exception
     * @since 1.1
     */
    public static ZipExtraField[] parse( byte[] data )
        throws ZipException
    {
        ArrayList v = new ArrayList();
        int start = 0;
        while( start <= data.length - 4 )
        {
            ZipShort headerId = new ZipShort( data, start );
            int length = ( new ZipShort( data, start + 2 ) ).getValue();
            if( start + 4 + length > data.length )
            {
                throw new ZipException( "data starting at " + start + " is in unknown format" );
            }
            try
            {
                ZipExtraField ze = createExtraField( headerId );
                ze.parseFromLocalFileData( data, start + 4, length );
                v.add( ze );
            }
            catch( InstantiationException ie )
            {
                throw new ZipException( ie.getMessage() );
            }
            catch( IllegalAccessException iae )
            {
                throw new ZipException( iae.getMessage() );
            }
            start += ( length + 4 );
        }
        if( start != data.length )
        {// array not exhausted
            throw new ZipException( "data starting at " + start + " is in unknown format" );
        }

        final ZipExtraField[] result = new ZipExtraField[ v.size() ];
        return (ZipExtraField[])v.toArray( result );
    }

    /**
     * Register a ZipExtraField implementation. <p>
     *
     * The given class must have a no-arg constructor and implement the {@link
     * ZipExtraField ZipExtraField interface}.</p>
     *
     * @param c Description of Parameter
     * @since 1.1
     */
    public static void register( Class c )
    {
        try
        {
            ZipExtraField ze = (ZipExtraField)c.newInstance();
            implementations.put( ze.getHeaderId(), c );
        }
        catch( ClassCastException cc )
        {
            throw new RuntimeException( c +
                                        " doesn\'t implement ZipExtraField" );
        }
        catch( InstantiationException ie )
        {
            throw new RuntimeException( c + " is not a concrete class" );
        }
        catch( IllegalAccessException ie )
        {
            throw new RuntimeException( c +
                                        "\'s no-arg constructor is not public" );
        }
    }
}
