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
 * Builds typelib service descriptors.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class ServiceDescriptorBuilder
    implements DescriptorBuilder
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( ServiceDescriptorBuilder.class );

    private final static Version SERVICE_DESCRIPTOR_VERSION = new Version( 1, 0, 0 );

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
            if( ! SERVICE_DESCRIPTOR_VERSION.complies( version ) )
            {
                final String message = REZ.getString( "service-descriptor-version.error", version, SERVICE_DESCRIPTOR_VERSION );
                throw new DeploymentException( message );
            }

            // TODO - populate the descriptor

            return new ServiceDescriptor( url );
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "build-service-descriptor.error", url );
            throw new DeploymentException( message, e );
        }
    }
}
