/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.deployer;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Deployment
{
    private final static String   TSKDEF_FILE     = "TASK-LIB/taskdefs.xml";

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
            throw new DeploymentException( "Unable to form url", ioe );
        }
    }

    private Configuration buildDescriptor()
        throws DeploymentException
    {
        final String systemID = "jar:" + getURL() + "!/" + TSKDEF_FILE;

        final DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();

        try
        {
            return builder.build( new InputSource( systemID ) ); 
        }
        catch( final SAXException se )
        {
            throw new DeploymentException( "Malformed configuration data", se );
        }
        catch( final ConfigurationException ce )
        {
            throw new DeploymentException( "Error building configuration", ce );
        }
        catch( final IOException ioe )
        {
            throw new DeploymentException( "Error reading configuration", ioe );
        }
    }
}

