/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ejb;

import javax.xml.parsers.SAXParser;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;

public interface EJBDeploymentTool
{
    /**
     * Process a deployment descriptor, generating the necessary vendor specific
     * deployment files.
     *
     * @param descriptorFilename the name of the deployment descriptor
     * @param saxParser a SAX parser which can be used to parse the deployment
     *      descriptor.
     * @exception TaskException Description of Exception
     */
    void processDescriptor( String descriptorFilename, SAXParser saxParser )
        throws TaskException;

    /**
     * Called to validate that the tool parameters have been configured.
     *
     * @exception TaskException Description of Exception
     */
    void validateConfigured()
        throws TaskException;

    /**
     * Set the task which owns this tool
     *
     * @param task The new Task value
     */
    void setTask( Task task );

    /**
     * Configure this tool for use in the ejbjar task.
     *
     * @param config Description of Parameter
     */
    void configure( EjbJar.Config config );
}
