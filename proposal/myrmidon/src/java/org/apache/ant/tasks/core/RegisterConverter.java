/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasks.core;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import org.apache.ant.AntException;
import org.apache.avalon.camelot.DefaultLocator;
import org.apache.ant.convert.ConverterEngine;
import org.apache.ant.convert.DefaultConverterInfo;
import org.apache.ant.tasklet.AbstractTasklet;
import org.apache.ant.tasklet.engine.TaskletEngine;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentManagerException;
import org.apache.avalon.Composer;
import org.apache.avalon.camelot.DeploymentException;
import org.apache.avalon.camelot.RegistryException;

/**
 * Method to register a single converter.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class RegisterConverter 
    extends AbstractTasklet
    implements Composer
{
    protected String              m_sourceType;
    protected String              m_destinationType;
    protected String              m_lib;
    protected String              m_classname;
    protected TaskletEngine       m_engine;
    
    public void compose( final ComponentManager componentManager )
        throws ComponentManagerException
    {
        m_engine = (TaskletEngine)componentManager.
            lookup( "org.apache.ant.tasklet.engine.TaskletEngine" );
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
    
    public void run()
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
                m_engine.getTskDeployer().deployConverter( m_classname, url.toString(), url ); 
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
                m_engine.getConverterEngine().getInfoRegistry().register( m_classname, info ); 
                m_engine.getConverterEngine().getRegistry().register( m_classname, locator ); 
            }
            catch( final RegistryException re )
            {
                throw new AntException( "Error registering resource", re );
            }
        }
    }
    
    protected URL getURL( final String libName )
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
