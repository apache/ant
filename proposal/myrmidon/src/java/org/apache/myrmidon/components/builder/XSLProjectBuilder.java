/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.builder;

import java.net.URL;
import java.io.InputStream;
import java.util.Properties;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.avalon.framework.configuration.SAXConfigurationHandler;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Default implementation to construct project from a build file.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class XSLProjectBuilder
    extends DefaultProjectBuilder
    implements Parameterizable
{
    private final static String PARAM_EXCEPTION = 
        "Malformed PI: expected <?xsl-param name=\"foo\" value=\"bar\"?>";

    private final static String PARAMS_EXCEPTION = 
        "Malformed PI: expected <?xsl-params location=\"myparams.properties\"?>";

    private final static String STYLE_EXCEPTION = 
        "Malformed PI: expected <?xsl-params href=\"mystylesheet.xsl\"?>";

    private Parameters     m_parameters;
    private URL            m_systemID;

    public void parameterize( final Parameters parameters )
    {
        m_parameters = parameters;
    }

    protected void process( final URL sourceID,
                            final SAXConfigurationHandler handler )
        throws Exception
    {
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        final SAXParser saxParser = saxParserFactory.newSAXParser();
        final XMLReader reader = saxParser.getXMLReader();
        reader.setFeature( "http://xml.org/sax/features/validation", false );
        reader.setErrorHandler( handler );

        final ReactorPIHandler reactorHandler = new ReactorPIHandler();
        reader.setContentHandler( reactorHandler );

        try { reader.parse( sourceID.toString() ); }
        catch( final StopParsingException spe )
        {
            //Ignore me
        }

        Transformer transformer = null;

        final int size = reactorHandler.getPICount();
        for( int i = 0; i < size; i++ )
        {
            final String target = reactorHandler.getTarget( i );
            final String data = reactorHandler.getData( i );

            if( target.equals( "xsl-param" ) ) handleParameter( data );
            else if( target.equals( "xsl-params" ) ) handleParameters( data, sourceID );
            else if( target.equals( "xsl-stylesheet" ) ) 
            {
                if( null != transformer )
                {
                    throw new SAXException( "Build file can not contain " + 
                                            "two xsl-stylesheet PIs" );
                }

                final TransformerFactory factory = TransformerFactory.newInstance();
                final String stylesheet = getStylesheet( data, sourceID );
                transformer = factory.newTransformer( new StreamSource( stylesheet ) );
            }
        }

        if( null == transformer )
        {
            reader.setContentHandler( handler );
            reader.parse( sourceID.toString() );
        }
        else
        {
            final SAXResult result = new SAXResult( handler );
            transformer.transform( new StreamSource( sourceID.toString() ), result );
        }        
    }

    private void handleParameter( final String data )
        throws SAXException
    {
        int index = data.indexOf( '\"' );
        if( -1 == index )
        {
            throw new SAXException( PARAM_EXCEPTION );
        }

        index = data.indexOf( '\"', index + 1 );
        if( -1 == index )
        {
            throw new SAXException( PARAM_EXCEPTION );
        }

        //split between two "attributes" occurs on index
        final String[] name = parseAttribute( data.substring( 0, index + 1 ) );
        final String[] value = parseAttribute( data.substring( index + 1 ).trim() );
        
        if( !name[ 0 ].equals( "name" ) || !value[ 0 ].equals( "value" ) )
        {
            throw new SAXException( PARAM_EXCEPTION );
        }

        m_parameters.setParameter( name[ 1 ], value[ 1 ] );
    }

    private void handleParameters( final String data, final URL baseSource )
        throws SAXException
    {
        final String[] params = parseAttribute( data );
        if( !params[ 0 ].equals( "location" ) )
        {
            throw new SAXException( PARAMS_EXCEPTION );
        }

        try
        {
            final Properties properties = new Properties();
            final URL url = new URL( baseSource, params[ 1 ] );
            final InputStream input = url.openStream();
            properties.load( input );
            final Parameters parameters = Parameters.fromProperties( properties );
            m_parameters.merge( parameters );
        }
        catch( final Exception e )
        {
            throw new SAXException( "Error loading parameters: " + e );
        }
    }

    private String getStylesheet( final String data, final URL baseSource )
        throws SAXException
    {
        final String[] stylesheet = parseAttribute( data );
        if( !stylesheet[ 0 ].equals( "href" ) )
        {
            throw new SAXException( STYLE_EXCEPTION );
        }

        try { return new URL( baseSource, stylesheet[ 1 ] ).toString(); }
        catch( final Exception e )
        {
            throw new SAXException( "Error locating stylesheet '" + stylesheet[ 1 ] + 
                                    "' due to " + e );
        }
    }

    private String[] parseAttribute( final String data )
        throws SAXException
    {
        //name="value"
        int index = data.indexOf( '=' );
        if( -1 == index )
        {
            throw new SAXException( "Expecting an attribute but received '" + 
                                    data + "'" );
        }

        final int size = data.length();
        if( '\"' != data.charAt( index + 1 ) || 
            '\"' != data.charAt( size - 1 ) ||
            size - 1 == index )
        {
            throw new SAXException( "Expecting the value of attribute " + 
                                    data.substring( 0, index ) +
                                    " to be enclosed in quotes" );
        }
        
        final String[] result = new String[ 2 ];
        result[ 0 ] = data.substring( 0, index );
        result[ 1 ] = data.substring( index + 2, size - 1 );

        return result;
    }
}
