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
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.deployer.DeploymentException;
import org.apache.myrmidon.components.deployer.Deployer;

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
    protected Deployer            m_deployer;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_deployer = (Deployer)componentManager.lookup( Deployer.ROLE );
    }

    public void setLib( final String lib )
    {
        m_lib = lib;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_lib )
        {
            throw new TaskException( "Must specify lib parameter" );
        }

        final File file = getContext().resolveFile( m_lib );

        try
        {
            m_deployer.deploy( file );
        }
        catch( final DeploymentException de )
        {
            throw new TaskException( "Error registering resource", de );
        }
    }
}
