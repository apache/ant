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
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.myrmidon.interfaces.deployer.ConverterDefinition;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.deployer.TypeDefinition;

/**
 * Builds typelib type descriptors.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class TypeDescriptorBuilder
    implements DescriptorBuilder
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( TypeDescriptorBuilder.class );

    private static final Version TYPE_DESCRIPTOR_VERSION = new Version( 1, 0, 0 );

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
            if( !TYPE_DESCRIPTOR_VERSION.complies( version ) )
            {
                final String message = REZ.getString( "type-descriptor-version.error", version, TYPE_DESCRIPTOR_VERSION );
                throw new DeploymentException( message );
            }

            // Assemble the descriptor
            final TypeDescriptor descriptor = new TypeDescriptor( url );

            // Extract each of the types elements
            final Configuration[] typeEntries = config.getChild( "types" ).getChildren();
            for( int i = 0; i < typeEntries.length; i++ )
            {
                final Configuration typeEntry = typeEntries[ i ];
                final TypeDefinition typeDef = createTypeDefinition( typeEntry );
                descriptor.addDefinition( typeDef );
            }

            return descriptor;
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "build-type-descriptor.error", url );
            throw new DeploymentException( message, e );
        }
    }

    /**
     * Creates a type definition.
     */
    private TypeDefinition createTypeDefinition( final Configuration configuration )
        throws ConfigurationException
    {
        final String roleShorthand = configuration.getName();
        if( roleShorthand.equals( "converter" ) )
        {
            // A converter definition
            final String className = configuration.getAttribute( "classname" );
            final String source = configuration.getAttribute( "source" );
            final String destination = configuration.getAttribute( "destination" );
            return new ConverterDefinition( className, source, destination );
        }
        else
        {
            // A type definition
            final String typeName = configuration.getAttribute( "name" );
            final String className = configuration.getAttribute( "classname" );
            return new TypeDefinition( typeName, roleShorthand, className );
        }
    }
}
