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
package org.apache.tools.ant.util.depend.bcel;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.DescendingVisitor;
import org.apache.bcel.classfile.JavaClass;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.StreamUtils;
import org.apache.tools.ant.util.depend.AbstractAnalyzer;

/**
 * An analyzer capable fo traversing all class - class relationships.
 *
 */
public class FullAnalyzer extends AbstractAnalyzer {
    /**
     * Default constructor
     *
     * Causes the BCEL classes to load to ensure BCEL dependencies can
     * be satisfied
     */
    public FullAnalyzer() {
        // force BCEL classes to load now
        try {
            new ClassParser("force"); //NOSONAR
        } catch (Exception e) {
            // all released versions of BCEL may throw an IOException
            // here, but BCEL's trunk does no longer declare to do so
            if (!(e instanceof IOException)) {
                throw new BuildException(e);
            }
            // ignore IOException like we've always done
        }
    }

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

        int count = 0;
        int maxCount = isClosureRequired() ? MAX_LOOPS : 2;
        while (!toAnalyze.isEmpty() && count++ < maxCount) {
            DependencyVisitor dependencyVisitor = new DependencyVisitor();
            for (String classname : toAnalyze) {
                dependencies.add(classname);
                try {
                    File container = getClassContainer(classname);
                    if (container == null) {
                        continue;
                    }
                    containers.add(container);

                    ClassParser parser;
                    if (container.getName().endsWith(".class")) {
                        parser = new ClassParser(container.getPath());
                    } else {
                        parser = new ClassParser(container.getPath(),
                            classname.replace('.', '/') + ".class");
                    }

                    JavaClass javaClass = parser.parse();
                    DescendingVisitor traverser
                         = new DescendingVisitor(javaClass, dependencyVisitor);
                    traverser.visit();
                } catch (IOException ioe) {
                    // ignore
                }
            }

            toAnalyze.clear();
            // now recover all the dependencies collected and add to the list.
            StreamUtils.enumerationAsStream(dependencyVisitor.getDependencies())
                    .filter(className -> !dependencies.contains(className))
                    .forEach(toAnalyze::add);
        }

        files.clear();
        files.addAll(containers);

        classes.clear();
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

