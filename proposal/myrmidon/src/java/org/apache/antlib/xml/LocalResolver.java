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
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.myrmidon.api.TaskContext;

class LocalResolver
    implements EntityResolver
{
    private final Hashtable m_fileDTDs = new Hashtable();
    private final Hashtable m_resourceDTDs = new Hashtable();
    private final Hashtable m_urlDTDs = new Hashtable();
    private final TaskContext m_context;

    public LocalResolver( final TaskContext context )
    {
        m_context = context;
    }

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
                m_context.debug( message );
            }
            return;
        }

        if( getClass().getResource( location ) != null )
        {
            if( publicId != null )
            {
                m_resourceDTDs.put( publicId, location );
                final String message = "Mapped publicId " + publicId +
                    " to resource " + location;
                m_context.debug( message );
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
                m_context.debug( message );
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
                m_context.debug( "Resolved " + publicId + " to local resource " + dtdResourceName );
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
                m_context.debug( message );
                return new InputSource( is );
            }
            catch( IOException ioe )
            {
                //ignore
            }
        }

        final String message = "Could not resolve ( publicId: " + publicId +
            ", systemId: " + systemId + ") to a local entity";
        m_context.info( message );

        return null;
    }
}
