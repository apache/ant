/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import java.io.File;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;

/**
 * Abstract task to extend to define a type.
 *
 * TODO: Make this support classpath sub-element in future
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public abstract class AbstractTypeDef
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( AbstractTypeDef.class );

    private File m_lib;
    private String m_name;
    private String m_className;

    public void setLib( final File lib )
    {
        //In the future this would be replaced by ClassPath sub-element
        m_lib = lib;
    }

    public void setName( final String name )
    {
        m_name = name;
    }

    public void setClassname( final String className )
    {
        m_className = className;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_name )
        {
            final String message = REZ.getString( "typedef.no-name.error" );
            throw new TaskException( message );
        }
        else if( null == m_className )
        {
            final String message = REZ.getString( "typedef.no-classname.error" );
            throw new TaskException( message );
        }

        final String shorthand = getRoleShorthand();

        try
        {
            // Locate the deployer, and use it to deploy the type
            final Deployer deployer = (Deployer)getService( Deployer.class );
            final TypeDeployer typeDeployer = deployer.createDeployer( m_lib );
            typeDeployer.deployType( shorthand, m_name, m_className );
        }
        catch( DeploymentException e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }

    protected abstract String getRoleShorthand();
}
