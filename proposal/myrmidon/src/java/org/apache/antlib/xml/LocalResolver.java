/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class LocalResolver
    extends AbstractLogEnabled
    implements EntityResolver
{
    private Hashtable m_fileDTDs = new Hashtable();
    private Hashtable m_resourceDTDs = new Hashtable();
    private Hashtable m_urlDTDs = new Hashtable();

    public void registerDTD( String publicId, String location )
    {
        if( location == null )
        {
            return;
        }

        File fileDTD = new File( location );
        if( fileDTD.exists() )
        {
            if( publicId != null )
            {
                m_fileDTDs.put( publicId, fileDTD );
                final String message = "Mapped publicId " + publicId + " to file " + fileDTD;
                getLogger().debug( message );
            }
            return;
        }

        if( LocalResolver.this.getClass().getResource( location ) != null )
        {
            if( publicId != null )
            {
                m_resourceDTDs.put( publicId, location );
                final String message = "Mapped publicId " + publicId +
                    " to resource " + location;
                getLogger().debug( message );
            }
        }

        try
        {
            if( publicId != null )
            {
                URL urldtd = new URL( location );
                m_urlDTDs.put( publicId, urldtd );
            }
        }
        catch( MalformedURLException e )
        {
            //ignored
        }
    }

    public void registerDTD( DTDLocation location )
    {
        registerDTD( location.getPublicId(), location.getLocation() );
    }

    public InputSource resolveEntity( String publicId, String systemId )
        throws SAXException
    {
        File dtdFile = (File)m_fileDTDs.get( publicId );
        if( dtdFile != null )
        {
            try
            {
                final String message = "Resolved " + publicId + " to local file " + dtdFile;
                getLogger().debug( message );
                return new InputSource( new FileInputStream( dtdFile ) );
            }
            catch( FileNotFoundException ex )
            {
                // ignore
            }
        }

        String dtdResourceName = (String)m_resourceDTDs.get( publicId );
        if( dtdResourceName != null )
        {
            InputStream is = getClass().getResourceAsStream( dtdResourceName );
            if( is != null )
            {
                getLogger().debug( "Resolved " + publicId + " to local resource " + dtdResourceName );
                return new InputSource( is );
            }
        }

        URL dtdUrl = (URL)m_urlDTDs.get( publicId );
        if( dtdUrl != null )
        {
            try
            {
                InputStream is = dtdUrl.openStream();
                final String message = "Resolved " + publicId + " to url " + dtdUrl;
                getLogger().debug( message );
                return new InputSource( is );
            }
            catch( IOException ioe )
            {
                //ignore
            }
        }

        final String message = "Could not resolve ( publicId: " + publicId +
            ", systemId: " + systemId + ") to a local entity";
        getLogger().info( message );

        return null;
    }
}
