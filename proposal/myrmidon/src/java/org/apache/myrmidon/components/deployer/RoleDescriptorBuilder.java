/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.Version;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;

/**
 * Builds typelib role descriptors.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class RoleDescriptorBuilder
    implements DescriptorBuilder
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( RoleDescriptorBuilder.class );

    private final static Version ROLE_DESCRIPTOR_VERSION = new Version( 1, 0, 0 );

    /**
     * Builds a descriptor from a set of configuration.
     */
    public TypelibDescriptor createDescriptor( final Configuration config,
                                               final String url )
        throws DeploymentException
    {
        try
        {
            // Check version
            final String versionString = config.getAttribute( "version" );
            final Version version = Version.getVersion( versionString );
            if( !ROLE_DESCRIPTOR_VERSION.complies( version ) )
            {
                final String message = REZ.getString( "role-descriptor-version.error", version, ROLE_DESCRIPTOR_VERSION );
                throw new DeploymentException( message );
            }

            // Assemble the descriptor
            final RoleDescriptor descriptor = new RoleDescriptor( url );

            // Extract each of the role elements
            final Configuration[] types = config.getChildren( "role" );
            for( int i = 0; i < types.length; i++ )
            {
                final String name = types[ i ].getAttribute( "shorthand" );
                final String role = types[ i ].getAttribute( "name" );
                final RoleDefinition roleDef = new RoleDefinition( role, name );
                descriptor.addDefinition( roleDef );
            }

            return descriptor;
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "build-role-descriptor.error", url );
            throw new DeploymentException( message, e );
        }
    }
}
