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
import org.apache.avalon.framework.camelot.DeploymentException;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.components.deployer.TskDeployer;

/**
 * Method to register a tasklib.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class RegisterTasklib
    extends AbstractTask
    implements Composable
{
    protected String              m_lib;
    protected TskDeployer         m_tskDeployer;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_tskDeployer = (TskDeployer)componentManager.
            lookup( "org.apache.ant.tasklet.engine.TskDeployer" );
    }

    public void setLib( final String lib )
    {
        m_lib = lib;
    }

    public void execute()
        throws AntException
    {
        if( null == m_lib )
        {
            throw new AntException( "Must specify lib parameter" );
        }

        URL url = null;

        final File lib = getContext().resolveFile( m_lib );
        try { url = lib.toURL(); }
        catch( final MalformedURLException mue )
        {
            throw new AntException( "Malformed task-lib parameter " + m_lib, mue );
        }

        try
        {
            m_tskDeployer.deploy( url.toString(), url );
        }
        catch( final DeploymentException de )
        {
            throw new AntException( "Error registering resource", de );
        }
    }
}
