/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.deployer;

/**
 * A deployer for a type library.  Allows individual elements from a type
 * library to be deployed.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface TypeDeployer
{
    /**
     * Deploys everything in the type library.
     * @throws DeploymentException
     *      If the library cannot be deployed.
     */
    void deployAll()
        throws DeploymentException;

    /**
     * Deploys a single type from the type library.  The type definition is
     * read from the type library descriptor.
     *
     * @param roleShorthand
     *      The shorthand name for the role.
     *
     * @param typeName
     *      The type name.
     *
     * @throws DeploymentException
     *      If the type cannot be deployed.
     */
    void deployType( String roleShorthand, String typeName )
        throws DeploymentException;

    /**
     * Deploys a single type from the type library.
     *
     * @param typeDef
     *      The type definition.
     *
     * @throws DeploymentException
     *      If the type cannot be deployed.
     */
    void deployType( TypeDefinition typeDef )
        throws DeploymentException;
}
