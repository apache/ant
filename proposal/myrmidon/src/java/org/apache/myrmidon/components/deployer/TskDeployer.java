/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.deployer;

import java.io.File;
import org.apache.avalon.framework.component.Component;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface TskDeployer
    extends Component
{
    String ROLE = "org.apache.myrmidon.components.deployer.TskDeployer";

    /**
     * Deploy a library.
     *
     * @param file the file deployment
     * @exception DeploymentException if an error occurs
     */
    void deploy( File file )
        throws DeploymentException;

    void deployConverter( String name, File file )
        throws DeploymentException;

    void deployDataType( String name, File file )
        throws DeploymentException;

    void deployTask( String name, File file )
        throws DeploymentException;
}

