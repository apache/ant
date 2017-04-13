/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.optional.depend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.depend.AbstractAnalyzer;

/**
 * An analyzer which uses the depend task's bytecode classes to analyze
 * dependencies
 *
 */
public class AntAnalyzer extends AbstractAnalyzer {

    /**
     * Determine the dependencies of the configured root classes.
     *
     * @param files a vector to be populated with the files which contain
     *      the dependency classes
     * @param classes a vector to be populated with the names of the
     *      dependency classes.
     */
    @Override
    protected void determineDependencies(Vector<File> files, Vector<String> classes) {
        // we get the root classes and build up a set of
        // classes upon which they depend
        Set<String> toAnalyze = new HashSet<>(Collections.list(getRootClasses()));

        int count = 0;
        int maxCount = isClosureRequired() ? MAX_LOOPS : 1;
        Set<String> dependencies = new HashSet<>();
        Set<File> containers = new HashSet<>();
        Set<String> analyzedDeps = null;
        while (!toAnalyze.isEmpty() && count++ < maxCount) {
            analyzedDeps = new HashSet<>();
            for (String classname : toAnalyze) {
                dependencies.add(classname);
                try {
                    File container = getClassContainer(classname);
                    if (container == null) {
                        continue;
                    }
                    containers.add(container);

                    ZipFile zipFile = null;
                    InputStream inStream = null;
                    try {
                        if (container.getName().endsWith(".class")) {
                            inStream = Files.newInputStream(Paths.get(container.getPath()));
                        } else {
                            zipFile = new ZipFile(container.getPath());
                            String entryName
                                = classname.replace('.', '/') + ".class";
                            ZipEntry entry = new ZipEntry(entryName);
                            inStream
                                = zipFile.getInputStream(entry);
                        }
                        ClassFile classFile = new ClassFile();
                        classFile.read(inStream);
                        for (String dependency : classFile.getClassRefs()) {
                            analyzedDeps.add(dependency);
                        }
                    } finally {
                        FileUtils.close(inStream);
                        FileUtils.close(zipFile);
                    }
                } catch (IOException ioe) {
                    // ignore
                }
            }

            toAnalyze.clear();

            // now recover all the dependencies collected and add to the list.
            for (String className : analyzedDeps) {
                if (!dependencies.contains(className)) {
                    toAnalyze.add(className);
                }
            }
        }

        // pick up the last round of dependencies that were determined
        dependencies.addAll(analyzedDeps);

        files.removeAllElements();
        files.addAll(containers);
        classes.removeAllElements();
        classes.addAll(dependencies);
    }

    /**
     * Indicate if this analyzer can determine dependent files.
     *
     * @return true if the analyzer provides dependency file information.
     */
    @Override
    protected boolean supportsFileDependencies() {
        return true;
    }

}
