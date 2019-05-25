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
package org.apache.tools.ant.taskdefs.optional.depend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.types.resources.ZipResource;
import org.apache.tools.ant.util.depend.AbstractAnalyzer;
import org.apache.tools.zip.ZipFile;

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
        Set<String> dependencies = new HashSet<>();
        Set<File> containers = new HashSet<>();
        Set<String> toAnalyze = new HashSet<>(Collections.list(getRootClasses()));
        Set<String> analyzedDeps = new HashSet<>();

        int count = 0;
        int maxCount = isClosureRequired() ? MAX_LOOPS : 1;
        while (!toAnalyze.isEmpty() && count++ < maxCount) {
            analyzedDeps.clear();
            for (String classname : toAnalyze) {
                dependencies.add(classname);
                File container = null;
                try {
                    container = getClassContainer(classname);
                } catch (IOException ioe) {
                    // ignore
                }
                if (container != null) {
                    containers.add(container);

                    try (InputStream inStream = container.getName().endsWith(".class")
                            ? Files.newInputStream(Paths.get(container.getPath()))
                            : ZipResource.getZipEntryStream(new ZipFile(container.getPath(), "UTF-8"),
                            classname.replace('.', '/') + ".class")) {
                        ClassFile classFile = new ClassFile();
                        classFile.read(inStream);
                        analyzedDeps.addAll(classFile.getClassRefs());
                    } catch (IOException ioe) {
                        // ignore
                    }
                }
            }

            toAnalyze.clear();
            // now recover all the dependencies collected and add to the list.
            analyzedDeps.stream().filter(className -> !dependencies.contains(className))
                    .forEach(toAnalyze::add);
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
