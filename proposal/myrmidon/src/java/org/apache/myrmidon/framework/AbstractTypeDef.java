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
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.deployer.TypeDefinition;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;

/**
 * Abstract task to extend to define a type.
 *
 * TODO: Make this support classpath sub-element in future
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractTypeDef
    extends AbstractContainerTask
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( AbstractTypeDef.class );

    // TODO - replace lib with class-path
    private File m_lib;
    private String m_name;
    private String m_classname;

    protected void setName( final String name )
    {
        m_name = name;
    }

    public void setClassname( final String classname )
    {
        m_classname = classname;
    }

    public void setLib( final File lib )
    {
        m_lib = lib;
    }

    protected final String getName()
    {
        return m_name;
    }

    protected final String getClassname()
    {
        return m_classname;
    }

    /**
     * Executes the task.
     */
    public void execute()
        throws TaskException
    {
        if( null == m_lib )
        {
            final String message = REZ.getString( "typedef.no-lib.error" );
            throw new TaskException( message );
        }

        try
        {
            // Locate the deployer, and use it to deploy the type
            final Deployer deployer = (Deployer)getService( Deployer.class );
            final TypeDeployer typeDeployer = deployer.createDeployer( m_lib );
            final TypeDefinition typeDef = createTypeDefinition();
            typeDeployer.deployType( typeDef );
        }
        catch( DeploymentException e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }

    /**
     * Creates the definition for the type to be deployed.
     */
    protected abstract TypeDefinition createTypeDefinition();
}
