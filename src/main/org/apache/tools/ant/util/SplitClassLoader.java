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
package org.apache.tools.ant.util;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

/**
 * Specialized classloader for tasks that need finer grained control
 * over which classes are to be loaded via Ant's classloader and which
 * should not even if they are available.
 */
public final class SplitClassLoader extends AntClassLoader {

    private final String[] splitClasses;

    /**
     * @param parent ClassLoader
     * @param path Path
     * @param project Project
     * @param splitClasses classes contained herein will not be loaded
     * via Ant's classloader
     */
    public SplitClassLoader(ClassLoader parent, Path path, Project project,
                            String[] splitClasses) {
        super(parent, project, path, true);
        this.splitClasses = splitClasses;
    }

    // forceLoadClass is not convenient here since it would not
    // properly deal with inner classes of these classes.
    @Override
    protected synchronized Class<?> loadClass(String classname, boolean resolve)
        throws ClassNotFoundException {
        Class<?> theClass = findLoadedClass(classname);
        if (theClass != null) {
            return theClass;
        }
        if (isSplit(classname)) {
            theClass = findClass(classname);
            if (resolve) {
                resolveClass(theClass);
            }
            return theClass;
        }
        return super.loadClass(classname, resolve);
    }

    private boolean isSplit(String classname) {
        String simplename = classname.substring(classname.lastIndexOf('.') + 1);
        for (String splitClass : splitClasses) {
            if (simplename.equals(splitClass)
                    || simplename.startsWith(splitClass + '$')) {
                return true;
            }
        }
        return false;
    }

}
