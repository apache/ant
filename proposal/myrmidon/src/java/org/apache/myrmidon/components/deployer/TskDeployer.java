/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.deployer;

import java.net.URL;
import org.apache.avalon.framework.camelot.Deployer;
import org.apache.avalon.framework.camelot.DeploymentException;
import org.apache.avalon.framework.logger.Loggable;
import org.apache.log.Logger;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface TskDeployer
    extends Deployer
{
    void deployConverter( String name, String location, URL url )
        throws DeploymentException;

    void deployDataType( String name, String location, URL url )
        throws DeploymentException;

    void deployTask( String name, String location, URL url )
        throws DeploymentException;
}

