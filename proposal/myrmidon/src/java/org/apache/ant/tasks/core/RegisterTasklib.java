/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasks.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.ant.AntException;
import org.apache.ant.tasklet.AbstractTasklet;
import org.apache.ant.tasklet.engine.TaskletEngine;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentNotAccessibleException;
import org.apache.avalon.ComponentNotFoundException;
import org.apache.avalon.Composer;
import org.apache.avalon.camelot.DeploymentException;

/**
 * Method to register a tasklib.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class RegisterTasklib 
    extends AbstractTasklet
    implements Composer
{
    protected String              m_lib;
    protected TaskletEngine     m_engine;
    
    public void compose( final ComponentManager componentManager )
        throws ComponentNotFoundException, ComponentNotAccessibleException
    {
        m_engine = (TaskletEngine)componentManager.
            lookup( "org.apache.ant.tasklet.engine.TaskletEngine" );
    }

    public void setLib( final String lib )
    {
        m_lib = lib;
    }

    public void run()
        throws AntException
    {
        if( null == m_lib )
        {
            throw new AntException( "Must specify lib parameter" );
        }
        
        URL url = null;

        final File lib = new File( getContext().resolveFilename( m_lib ) );
        try { url = lib.toURL(); }
        catch( final MalformedURLException mue )
        {
            throw new AntException( "Malformed task-lib parameter " + m_lib, mue );
        }

        try
        {
            m_engine.getTskDeployer().deploy( url.toString(), url );
        }
        catch( final DeploymentException de )
        {
            throw new AntException( "Error registering resource", de );
        }
    }
}
