/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.classloader;

import java.io.File;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;

/**
 * Manages a classloader hierarchy.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public interface ClassLoaderManager
{
    String ROLE = ClassLoaderManager.class.getName();

    /**
     * Builds the ClassLoader for a Jar file.
     */
    ClassLoader createClassLoader( File jar ) throws DeploymentException;
}
