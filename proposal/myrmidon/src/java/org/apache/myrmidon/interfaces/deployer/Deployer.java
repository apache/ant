/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.deployer;

import java.io.File;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;

/**
 * This class deploys type libraries into a registry.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface Deployer
{
    /** Role name for this interface. */
    String ROLE = Deployer.class.getName();

    /**
     * Returns the deployer for the type libraries contained in a ClassLoader,
     * creating the deployer if necessary.
     *
     * @param loader The ClassLoader to get the deployer for.
     * @return the deployer for this loader.
     * @exception DeploymentException if an error occurs.
     */
    TypeDeployer createDeployer( ClassLoader loader )
        throws DeploymentException;

    /**
     * Returns the deployer for a type library, creating the deployer if
     * necessary.
     *
     * @param file the file containing the type library.
     * @return the deployer for this type library.
     * @exception DeploymentException if an error occurs.
     */
    TypeDeployer createDeployer( File file )
        throws DeploymentException;

    /**
     * Creates a deployer which is a child of this deployer.
     * @param componentManager the ServiceManager for this component.
     * @return a child deployer.
     * @throws ServiceException if an error occurs.
     */
    Deployer createChildDeployer( ServiceManager componentManager )
        throws ServiceException;
}
