/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.deployer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.SAXConfigurationHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Deployment
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( Deployment.class );

    public final static String   DESCRIPTOR_NAME     = "META-INF/ant-descriptor.xml";

    private File            m_file;

    private Configuration   m_descriptor;

    public Deployment( final File file )
    {
        m_file = file;
    }
    
    public Configuration getDescriptor()
        throws DeploymentException
    {
        if( null == m_descriptor )
        {
            m_descriptor = buildDescriptor();
        }

        return m_descriptor;
    }

    public URL getURL()
        throws DeploymentException
    {
        try { return m_file.getCanonicalFile().toURL(); }
        catch( final IOException ioe )
        {
            final String message = REZ.getString( "bad-url.error", m_file );
            throw new DeploymentException( message, ioe );
        }
    }

    private Configuration buildDescriptor()
        throws DeploymentException
    {
        final String systemID = "jar:" + getURL() + "!/" + DESCRIPTOR_NAME;

        try
        {
            final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            final SAXParser saxParser = saxParserFactory.newSAXParser();
            final XMLReader parser = saxParser.getXMLReader();
            //parser.setFeature( "http://xml.org/sax/features/namespace-prefixes", false );

            final SAXConfigurationHandler handler = new SAXConfigurationHandler();
            parser.setContentHandler( handler );
            parser.setErrorHandler( handler );

            parser.parse( systemID );
            return handler.getConfiguration();
        }
        catch( final SAXException se )
        {
            final String message = REZ.getString( "bad-descriptor.error" );
            throw new DeploymentException( message, se );
        }
        catch( final ParserConfigurationException pce )
        {
            final String message = REZ.getString( "bad-parser.error" );
            throw new DeploymentException( message, pce );
        }
        catch( final IOException ioe )
        {
            final String message = REZ.getString( "bad-read.error" );
            throw new DeploymentException( message, ioe );
        }
    }
}
