/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;

/**
 * Builds a descriptor.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
interface DescriptorBuilder
{
    /**
     * Builds a descriptor from a set of configuration.
     */
    TypelibDescriptor createDescriptor( Configuration config,
                                        String descriptorUrl )
        throws DeploymentException;
}
