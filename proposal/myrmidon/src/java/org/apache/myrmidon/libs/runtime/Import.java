/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.libs.runtime;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.deployer.Deployer;
import org.apache.myrmidon.components.deployer.DeploymentException;

/**
 * Task to import a tasklib.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Import
    extends AbstractTask
    implements Composable
{
    private File        m_lib;
    private Deployer    m_deployer;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_deployer = (Deployer)componentManager.lookup( Deployer.ROLE );
    }

    public void setLib( final File lib )
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

        try
        {
            m_deployer.deploy( m_lib );
        }
        catch( final DeploymentException de )
        {
            throw new TaskException( "Error importing tasklib", de );
        }
    }
}
