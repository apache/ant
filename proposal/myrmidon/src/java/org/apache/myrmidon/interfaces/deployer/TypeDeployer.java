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
 * @author Adam Murdoch
 */
public interface TypeDeployer
{
    /**
     * Deploys everything in the type library.
     */
    void deployAll()
        throws DeploymentException;

    /**
     * Deploys a single type from the type library.  The type definition is
     * read from the type library descriptor.
     *
     * @param roleShorthand
     *          The <em>shorthand</em> for the role.
     * @param typeName
     *          The type name.
     */
    void deployType( String roleShorthand, String typeName )
        throws DeploymentException;

    /**
     * Deploys a single type from the type library.
     */
    void deployType( String roleShorthand, String typeName, String className )
        throws DeploymentException;

    /**
     * Deploys a converter from the type library.  The converter definition
     * is read from the type library descriptor.
     */
    void deployConverter( String className )
        throws DeploymentException;

    /**
     * Deploys a converter from the type library.
     */
    void deployConverter( String className, String srcClass, String destClass )
        throws DeploymentException;
}
