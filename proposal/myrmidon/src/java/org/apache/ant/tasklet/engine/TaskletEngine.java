/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import org.apache.ant.AntException;
import org.apache.ant.configuration.Configuration;
import org.apache.ant.convert.ConverterEngine;
import org.apache.ant.tasklet.engine.DataTypeEngine;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.avalon.Component;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.Loggable;
import org.apache.avalon.camelot.LocatorRegistry;
import org.apache.log.Logger;
 
/**
 * Engine inteface that should be implemented by all tasklet engines.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface TaskletEngine
    extends Component
{
    /**
     * Retrieve deployer for engine.
     *
     * @return the deployer
     */
    TskDeployer getTskDeployer();
    
    /**
     * Retrieve locator registry associated with engine.
     *
     * @return the LocatorRegistry
     */
    LocatorRegistry getRegistry();

    /**
     * Retrieve converter engine.
     *
     * @return the ConverterEngine
     */
    ConverterEngine getConverterEngine();

    /**
     * Retrieve datatype engine.
     *
     * @return the DataTypeEngine
     */
    DataTypeEngine getDataTypeEngine();
    
    /**
     * execute a task.
     *
     * @param task the configruation data for task
     * @exception AntException if an error occurs
     */
    void execute( Configuration task, TaskletContext context )
        throws AntException;
}
