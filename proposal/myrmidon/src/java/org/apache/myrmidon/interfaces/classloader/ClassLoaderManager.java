/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.classloader;

import java.io.File;

/**
 * Manages a classloader hierarchy.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public interface ClassLoaderManager
{
    String ROLE = ClassLoaderManager.class.getName();

    /**
     * Builds the ClassLoader for a Jar file, resolving dependencies.
     */
    ClassLoader createClassLoader( File jar ) throws ClassLoaderException;

    /**
     * Builds the ClassLoader for a set of files, resolving dependencies.
     *
     * @param jars The Jar/zip files to create the classloader for.  Use null
     *             or an empty array to use the common classloader.
     */
    ClassLoader createClassLoader( File[] jars ) throws ClassLoaderException;

    /**
     * Returns the common ClassLoader.  This is the parent of all classloaders
     * built by this ClassLoaderManager.
     */
    ClassLoader getCommonClassLoader();
}
