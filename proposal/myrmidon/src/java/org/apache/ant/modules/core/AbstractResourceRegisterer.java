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
import org.apache.ant.AntException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.ant.tasklet.engine.TskDeployer;
import org.apache.ant.tasklet.engine.TaskletEngine;
import org.apache.ant.tasklet.engine.DataTypeEngine;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.camelot.RegistryException;

/**
 * Method to register a single tasklet.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public abstract class AbstractResourceRegisterer 
    extends AbstractTask
    implements Composable
{
    protected String              m_lib;
    protected String              m_name;
    protected String              m_classname;
    protected TskDeployer         m_tskDeployer;
    protected DataTypeEngine      m_dataTypeEngine;
    protected TaskletEngine          m_engine;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_engine = (TaskletEngine)componentManager.
            lookup( "org.apache.ant.tasklet.engine.TaskletEngine" );

        m_tskDeployer = (TskDeployer)componentManager.
            lookup( "org.apache.ant.tasklet.engine.TskDeployer" );

        m_dataTypeEngine = (DataTypeEngine)componentManager.
            lookup( "org.apache.ant.tasklet.engine.DataTypeEngine" );
    }

    public void setLib( final String lib )
    {
        m_lib = lib;
    }
    
    public void setName( final String name )
    {
        m_name = name;
    }
    
    public void setClassname( final String classname )
    {
        m_classname = classname;
    }

    public void execute()
        throws AntException
    {
        if( null == m_name )
        {
            throw new AntException( "Must specify name parameter" );
        }
        else if( null == m_lib && null == m_classname )
        {
            throw new AntException( "Must specify classname if you don't specify " + 
                                    "lib parameter" );
        }
        
        final URL url = getURL( m_lib );

        try
        {
            registerResource( m_name, m_classname, url );
        }
        catch( final RegistryException re )
        {
            throw new AntException( "Error registering resource", re );
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

    protected abstract void registerResource( String name, String classname, URL url )
        throws AntException, RegistryException;
}
