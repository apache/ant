/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional.javah;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Creates the JavahAdapter based on the user choice and
 * potentially the VM vendor.
 *
 * @since Ant 1.6.3
 */
// CheckStyle:HideUtilityClassConstructorCheck OFF (bc)
public class JavahAdapterFactory {

    /**
     * Determines the default choice of adapter based on the VM
     * vendor.
     *
     * @return the default choice of adapter based on the VM
     * vendor
     */
    public static String getDefault() {
        if (JavaEnvUtils.isKaffe()) {
            return Kaffeh.IMPLEMENTATION_NAME;
        }
        if (JavaEnvUtils.isGij()) {
            return Gcjh.IMPLEMENTATION_NAME;
        }
        return ForkingJavah.IMPLEMENTATION_NAME;
    }

    /**
     * Creates the JavahAdapter based on the user choice and
     * potentially the VM vendor.
     *
     * @param choice the user choice (if any).
     * @param log a ProjectComponent instance used to access Ant's
     * logging system.
     * @return The adapter to use.
     * @throws BuildException if there is an error.
     */
    public static JavahAdapter getAdapter(String choice,
                                          ProjectComponent log)
        throws BuildException {
        return getAdapter(choice, log, null);
    }

    /**
     * Creates the JavahAdapter based on the user choice and
     * potentially the VM vendor.
     *
     * @param choice the user choice (if any).
     * @param log a ProjectComponent instance used to access Ant's
     * logging system.
     * @param classpath the classpath to use when looking up an
     * adapter class
     * @return The adapter to use.
     * @throws BuildException if there is an error.
     * @since Ant 1.8.0
     */
    public static JavahAdapter getAdapter(String choice,
                                          ProjectComponent log,
                                          Path classpath)
        throws BuildException {
        if ((JavaEnvUtils.isKaffe() && choice == null)
            || Kaffeh.IMPLEMENTATION_NAME.equals(choice)) {
            return new Kaffeh();
        }
        if ((JavaEnvUtils.isGij() && choice == null)
            || Gcjh.IMPLEMENTATION_NAME.equals(choice)) {
            return new Gcjh();
        }
        if (JavaEnvUtils.isAtLeastJavaVersion("10") &&
            (choice == null || ForkingJavah.IMPLEMENTATION_NAME.equals(choice))) {
            throw new BuildException("javah does not exist under Java 10 and higher,"
                + " use the javac task with nativeHeaderDir instead");
        }
        if (ForkingJavah.IMPLEMENTATION_NAME.equals(choice)) {
            return new ForkingJavah();
        }
        if (SunJavah.IMPLEMENTATION_NAME.equals(choice)) {
            return new SunJavah();
        }
        if (choice != null) {
            return resolveClassName(choice,
                                    // Memory leak in line below
                                    log.getProject()
                                    .createClassLoader(classpath));
        }
        return new ForkingJavah();
    }

    /**
     * Tries to resolve the given classname into a javah adapter.
     * Throws a fit if it can't.
     *
     * @param className The fully qualified classname to be created.
     * @param loader the classloader to use
     * @throws BuildException This is the fit that is thrown if className
     * isn't an instance of JavahAdapter.
     */
    private static JavahAdapter resolveClassName(String className,
                                                 ClassLoader loader)
            throws BuildException {
        return ClasspathUtils.newInstance(className,
                loader != null ? loader :
                JavahAdapterFactory.class.getClassLoader(), JavahAdapter.class);
    }
}
