/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.myrmidon.AntException;
import org.apache.ant.convert.engine.ConverterEngine;
import org.apache.ant.convert.engine.DefaultConverterInfo;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.components.deployer.TskDeployer;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.camelot.DefaultLocator;
import org.apache.avalon.framework.camelot.DeploymentException;
import org.apache.avalon.framework.camelot.RegistryException;

/**
 * Method to register a single converter.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class RegisterConverter 
    extends AbstractTask
    implements Composable
{
    protected String              m_sourceType;
    protected String              m_destinationType;
    protected String              m_lib;
    protected String              m_classname;
    protected TskDeployer         m_tskDeployer;
    protected ConverterEngine     m_converterEngine;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_tskDeployer = (TskDeployer)componentManager.
            lookup( "org.apache.myrmidon.components.deployer.TskDeployer" );

        m_converterEngine = (ConverterEngine)componentManager.
            lookup( "org.apache.ant.convert.engine.ConverterEngine" );
    }

    public void setLib( final String lib )
    {
        m_lib = lib;
    }
    
    public void setClassname( final String classname )
    {
        m_classname = classname;
    }

    public void setSourceType( final String sourceType )
    {
        m_sourceType = sourceType;
    }
    
    public void setDestinationType( final String destinationType )
    {
        m_destinationType = destinationType;
    }
    
    public void execute()
        throws AntException
    {
        if( null == m_classname )
        {
            throw new AntException( "Must specify classname parameter" );
        }
        
        final URL url = getURL( m_lib );

        boolean isFullyDefined = true;
        
        if( null == m_sourceType && null == m_destinationType )
        {
            isFullyDefined = false;
        }
        else if( null == m_sourceType || null == m_destinationType )
        {
            throw new AntException( "Must specify the source-type and destination-type " +
                                    "parameters when supplying a name" );
        }

        if( !isFullyDefined && null == url )
        {
            throw new AntException( "Must supply parameter if not fully specifying converter" );
        }

        if( !isFullyDefined )
        {
            try 
            { 
                m_tskDeployer.deployConverter( m_classname, url.toString(), url ); 
            }
            catch( final DeploymentException de )
            {
                throw new AntException( "Failed deploying " + m_classname + 
                                        " from " + url, de );
            }
        }
        else
        {
            final DefaultConverterInfo info = 
                new DefaultConverterInfo( m_sourceType, m_destinationType );
            final DefaultLocator locator = new DefaultLocator( m_classname, url );

            try
            {
                m_converterEngine.getInfoRegistry().register( m_classname, info ); 
                m_converterEngine.getRegistry().register( m_classname, locator ); 
            }
            catch( final RegistryException re )
            {
                throw new AntException( "Error registering resource", re );
            }
        }
    }
    
    protected URL getURL( final String libName )
        throws AntException
    {
        if( null != libName )
        {
            final File lib = getContext().resolveFile( libName );
            try { return lib.toURL(); }
            catch( final MalformedURLException mue )
            {
                throw new AntException( "Malformed task-lib parameter " + m_lib, mue );
            }
        }
        else
        {
            return null;
        }
    }
}
