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

package org.apache.tools.ant.taskdefs.optional.junitlauncher.confined;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.launch.AntMain;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.LoaderUtils;

import java.io.File;

/**
 *
 */
final class JUnitLauncherClassPathUtil {

    private static final String RESOURCE_IN_PLATFORM_ENGINE = "org/junit/platform/engine/TestEngine.class";
    private static final String RESOURCE_IN_PLATFORM_LAUNCHER = "org/junit/platform/launcher/core/LauncherFactory.class";
    private static final String RESOURCE_IN_PLATFORM_COMMON = "org/junit/platform/commons/annotation/Testable.class";
    private static final String RESOURCE_NAME_LAUNCHER_SUPPORT = "org/apache/tools/ant/taskdefs/optional/junitlauncher/LauncherSupport.class";


    static void addAntRuntimeResourceLocations(final Path path, final ClassLoader classLoader) {
        addResourceLocationToPath(path, classLoader, toResourceName(AntMain.class));
        addResourceLocationToPath(path, classLoader, toResourceName(Task.class));
        addResourceLocationToPath(path, classLoader, RESOURCE_NAME_LAUNCHER_SUPPORT);
    }

    static void addLauncherSupportResourceLocation(final Path path, final ClassLoader classLoader) {
        addResourceLocationToPath(path, classLoader, RESOURCE_NAME_LAUNCHER_SUPPORT);
    }

    static void addJUnitPlatformResourceLocations(final Path path, final ClassLoader classLoader) {
        // platform-engine
        addResourceLocationToPath(path, classLoader, RESOURCE_IN_PLATFORM_ENGINE);
        // platform-launcher
        addResourceLocationToPath(path, classLoader, RESOURCE_IN_PLATFORM_LAUNCHER);
        // platform-commons
        addResourceLocationToPath(path, classLoader, RESOURCE_IN_PLATFORM_COMMON);
    }

    static boolean addResourceLocationToPath(final Path path, final ClassLoader classLoader, final String resource) {
        final File f = LoaderUtils.getResourceSource(classLoader, resource);
        if (f == null) {
            return false;
        }
        path.createPath().setLocation(f);
        return true;
    }

    static boolean hasJUnitPlatformResources(final ClassLoader cl) {
        final File f = LoaderUtils.getResourceSource(cl, RESOURCE_IN_PLATFORM_ENGINE);
        return f != null;
    }

    private static String toResourceName(final Class klass) {
        final String name = klass.getName();
        return name.replaceAll("\\.", "/") + ".class";
    }
}
