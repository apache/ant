/*
 * Copyright  2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.util;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.launch.Locator;

/**
 * ClassLoader utility methods
 *
 * @author Conor MacNeill
 */
public class LoaderUtils {
    /**
     * Set the context classloader
     *
     * @param loader the ClassLoader to be used as the context class loader
     *      on the current thread.
     */
    public static void setContextClassLoader(ClassLoader loader) {
        Thread currentThread = Thread.currentThread();
        currentThread.setContextClassLoader(loader);
    }


    /**
     * JDK1.1 compatible access to set the context class loader.
     *
     * @return the ClassLoader instance being used as the context
     *      classloader on the current thread. Returns null on JDK 1.1
     */
    public static ClassLoader getContextClassLoader() {
        Thread currentThread = Thread.currentThread();
        return currentThread.getContextClassLoader();
    }

    /**
     * Indicates if the context class loader methods are available
     *
     * @return true if the get and set methods dealing with the context
     *      classloader are available.
     */
    public static boolean isContextLoaderAvailable() {
        return true;
    }

    /**
     * Normalize a source location
     *
     * @param source the source location to be normalized.
     *
     * @return the normalized source location.
     */
    private static File normalizeSource(File source) {
        if (source != null) {
            FileUtils fileUtils = FileUtils.newFileUtils();
            try {
                source = fileUtils.normalize(source.getAbsolutePath());
            } catch (BuildException e) {
                // relative path
            }
        }

        return source;
    }

    /**
     * Find the directory or jar file the class has been loaded from.
     *
     * @param c the class whose location is required.
     * @return the file or jar with the class or null if we cannot
     *         determine the location.
     *
     * @since Ant 1.6
     */
    public static File getClassSource(Class c) {
        return normalizeSource(Locator.getClassSource(c));
    }

    /**
     * Find the directory or a give resource has been loaded from.
     *
     * @param c the classloader to be consulted for the source
     * @param resource the resource whose location is required.
     *
     * @return the file with the resource source or null if
     *         we cannot determine the location.
     *
     * @since Ant 1.6
     */
    public static File getResourceSource(ClassLoader c, String resource) {
        if (c == null) {
            c = LoaderUtils.class.getClassLoader();
        }
        return normalizeSource(Locator.getResourceSource(c, resource));
    }
}

