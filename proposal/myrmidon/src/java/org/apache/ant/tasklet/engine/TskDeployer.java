/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import java.net.URL;
import org.apache.avalon.Loggable;
import org.apache.avalon.camelot.Deployer;
import org.apache.avalon.camelot.DeploymentException;
import org.apache.log.Logger;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface TskDeployer
    extends Deployer, Loggable
{
    void deployConverter( String name, String location, URL url )
        throws DeploymentException;

    void deployDataType( String name, String location, URL url )
        throws DeploymentException;    

    void deployTasklet( String name, String location, URL url )
        throws DeploymentException;
}

