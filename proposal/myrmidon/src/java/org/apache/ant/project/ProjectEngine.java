/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import org.apache.ant.AntException;
import org.apache.avalon.camelot.Deployer;
import org.apache.avalon.camelot.Registry;
import org.apache.log.Logger;

public interface ProjectEngine
{
    Deployer getDeployer();
    void setLogger( Logger logger );
    void execute( Project project, String target )
        throws AntException;
}
