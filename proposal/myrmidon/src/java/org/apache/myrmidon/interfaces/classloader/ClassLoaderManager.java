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
 * @version $Revision$ $Date$
 */
public interface ClassLoaderManager
{
    /** Role name for this interface. */
    String ROLE = ClassLoaderManager.class.getName();

    /**
     * Returns the ClassLoader for a Jar file.  The ClassLoader is created,
     * if necessary.  The ClassLoader's parent will include the common
     * ClassLoader, along with any extensions required by the Jar file.
     * It is guaranteed that each extension will appear at most once in the
     * ClassLoader hierarchy, so that classes from the extension can be
     * shared across the ClassLoaders returned by this method.
     *
     * @param jar the jar file containing the classes to load
     * @return the classloader
     * @throws ClassLoaderException on error
     */
    ClassLoader getClassLoader( File jar ) throws ClassLoaderException;

    /**
     * Creates a ClassLoader for a set of files.  See {@link #getClassLoader}
     * for details.
     *
     * @param jars The Jar/zip files to create the classloader for.  Use null
     *             or an empty array to use the common classloader.
     * @return the created ClassLoader
     * @throws ClassLoaderException on error
     */
    ClassLoader createClassLoader( File[] jars ) throws ClassLoaderException;

    /**
     * Returns the common ClassLoader, which is the parent of all classloaders
     * built by this ClassLoaderManager.
     *
     * @return the common ClassLoader.
     */
    ClassLoader getCommonClassLoader();
}
