/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.runtime;

import java.io.File;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;

/**
 * Task to import a tasklib.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant:task name="import"
 */
public class Import
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( Import.class );

    private File m_lib;

    public void setLib( final File lib )
    {
        m_lib = lib;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_lib )
        {
            final String message = REZ.getString( "import.no-lib.error" );
            throw new TaskException( message );
        }

        try
        {
            final Deployer deployer = (Deployer)getService( Deployer.class );
            final TypeDeployer typeDeployer = deployer.createDeployer( m_lib );
            typeDeployer.deployAll();
        }
        catch( final DeploymentException de )
        {
            final String message = REZ.getString( "import.no-deploy.error" );
            throw new TaskException( message, de );
        }
    }
}
