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
    /** Role name for this interface. */
    String ROLE = ClassLoaderManager.class.getName();

    /**
     * Builds the ClassLoader for a Jar file, resolving dependencies.
     * @param jar the jar file containing the classes to load
     * @return the created classloader
     * @throws ClassLoaderException on error
     */
    ClassLoader createClassLoader( File jar ) throws ClassLoaderException;

    /**
     * Builds the ClassLoader for a set of files, resolving dependencies.
     *
     * @param jars The Jar/zip files to create the classloader for.  Use null
     *             or an empty array to use the common classloader.
     * @return the created ClassLoader
     * @throws ClassLoaderException on error
     */
    ClassLoader createClassLoader( File[] jars ) throws ClassLoaderException;

    /**
     * Provides the common ClassLoader, which is the parent of all classloaders
     * built by this ClassLoaderManager.
     * @return the common ClassLoader
     */
    ClassLoader getCommonClassLoader();
}
