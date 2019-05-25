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
package org.apache.tools.ant.taskdefs.optional.native2ascii;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Creates the Native2AsciiAdapter based on the user choice and
 * potentially the VM vendor.
 *
 * @since Ant 1.6.3
 */
// CheckStyle:HideUtilityClassConstructorCheck OFF (bc)
public class Native2AsciiAdapterFactory {

    /**
     * Determines the default choice of adapter based on the VM
     * vendor.
     *
     * @return the default choice of adapter based on the VM
     * vendor
     */
    public static String getDefault() {
        if (shouldUseKaffe()) {
            return KaffeNative2Ascii.IMPLEMENTATION_NAME;
        }
        return BuiltinNative2Ascii.IMPLEMENTATION_NAME;
    }

    /**
     * Creates the Native2AsciiAdapter based on the user choice and
     * potentially the VM vendor.
     *
     * @param choice the user choice (if any).
     * @param log a ProjectComponent instance used to access Ant's
     * logging system.
     * @return The adapter to use.
     * @throws BuildException if there was a problem.
     */
    public static Native2AsciiAdapter getAdapter(String choice,
                                                 ProjectComponent log)
        throws BuildException {
        return getAdapter(choice, log, null);
    }

    /**
     * Creates the Native2AsciiAdapter based on the user choice and
     * potentially the VM vendor.
     *
     * @param choice the user choice (if any).
     * @param log a ProjectComponent instance used to access Ant's
     * logging system.
     * @param classpath the classpath to use when looking up an
     * adapter class
     * @return The adapter to use.
     * @throws BuildException if there was a problem.
     * @since Ant 1.8.0
     */
    public static Native2AsciiAdapter getAdapter(String choice,
                                                 ProjectComponent log,
                                                 Path classpath)
        throws BuildException {
        if ((shouldUseKaffe() && choice == null)
            || KaffeNative2Ascii.IMPLEMENTATION_NAME.equals(choice)) {
            return new KaffeNative2Ascii();
        } else if (SunNative2Ascii.IMPLEMENTATION_NAME.equals(choice)) {
            return new SunNative2Ascii();
        } else if (BuiltinNative2Ascii.IMPLEMENTATION_NAME.equals(choice)) {
            return new BuiltinNative2Ascii();
        } else if (choice != null) {
            return resolveClassName(choice,
                                    // Memory leak in line below
                                    log.getProject()
                                    .createClassLoader(classpath));
        }

        return new BuiltinNative2Ascii();
    }

    /**
     * Tries to resolve the given classname into a native2ascii adapter.
     * Throws a fit if it can't.
     *
     * @param className The fully qualified classname to be created.
     * @param loader the classloader to use
     * @throws BuildException This is the fit that is thrown if className
     * isn't an instance of Native2AsciiAdapter.
     */
    private static Native2AsciiAdapter resolveClassName(String className,
                                                        ClassLoader loader)
        throws BuildException {
        return ClasspathUtils.newInstance(className,
            loader != null ? loader :
            Native2AsciiAdapterFactory.class.getClassLoader(),
            Native2AsciiAdapter.class);
    }

    private static final boolean shouldUseKaffe() {
        return JavaEnvUtils.isKaffe() || JavaEnvUtils.isClasspathBased();
    }
}
