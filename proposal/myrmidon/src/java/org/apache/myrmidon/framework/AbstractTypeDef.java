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
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
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
 */
public abstract class AbstractTypeDef
    extends AbstractContainerTask
    implements Configurable
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( AbstractTypeDef.class );

    // TODO - replace lib with class-path
    private File m_lib;
    private TypeDefinition m_typeDef;

    /**
     * Configures this task.
     */
    public void configure( Configuration configuration ) throws ConfigurationException
    {
        m_typeDef = createTypeDefinition();

        // Configure attributes
        final String[] attrs = configuration.getAttributeNames();
        for( int i = 0; i < attrs.length; i++ )
        {
            final String name = attrs[ i ];
            final String value = configuration.getAttribute( name );
            if( name.equalsIgnoreCase( "lib" ) )
            {
                m_lib = (File)convert( File.class, value );
            }
            else
            {
                configure( m_typeDef, name, value );
            }
        }

        // Configure nested elements
        final Configuration[] elements = configuration.getChildren();
        for( int i = 0; i < elements.length; i++ )
        {
            Configuration element = elements[ i ];
            configure( m_typeDef, element );
        }
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
            typeDeployer.deployType( m_typeDef );
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
