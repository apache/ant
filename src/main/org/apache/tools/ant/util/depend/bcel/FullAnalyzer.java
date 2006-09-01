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
package org.apache.tools.ant.util.depend.bcel;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.DescendingVisitor;
import org.apache.bcel.classfile.JavaClass;
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
            new ClassParser("force");
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Determine the dependencies of the configured root classes.
     *
     * @param files a vector to be populated with the files which contain
     *      the dependency classes
     * @param classes a vector to be populated with the names of the
     *      depencency classes.
     */
    protected void determineDependencies(Vector files, Vector classes) {
        // we get the root classes and build up a set of
        // classes upon which they depend
        Hashtable dependencies = new Hashtable();
        Hashtable containers = new Hashtable();
        Hashtable toAnalyze = new Hashtable();
        for (Enumeration e = getRootClasses(); e.hasMoreElements();) {
            String classname = (String) e.nextElement();
            toAnalyze.put(classname, classname);
        }

        int count = 0;
        int maxCount = isClosureRequired() ? MAX_LOOPS : 2;
        while (toAnalyze.size() != 0 && count++ < maxCount) {
            DependencyVisitor dependencyVisitor = new DependencyVisitor();
            for (Enumeration e = toAnalyze.keys(); e.hasMoreElements();) {
                String classname = (String) e.nextElement();
                dependencies.put(classname, classname);
                try {
                    File container = getClassContainer(classname);
                    if (container == null) {
                        continue;
                    }
                    containers.put(container, container);

                    ClassParser parser = null;
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
            Enumeration depsEnum = dependencyVisitor.getDependencies();
            while (depsEnum.hasMoreElements()) {
                String className = (String) depsEnum.nextElement();
                if (!dependencies.containsKey(className)) {
                    toAnalyze.put(className, className);
                }
            }
        }

        files.removeAllElements();
        for (Enumeration e = containers.keys(); e.hasMoreElements();) {
            files.addElement((File) e.nextElement());
        }

        classes.removeAllElements();
        for (Enumeration e = dependencies.keys(); e.hasMoreElements();) {
            classes.addElement((String) e.nextElement());
        }
    }

    /**
     * Indicate if this analyzer can determine dependent files.
     *
     * @return true if the analyzer provides dependency file information.
     */
    protected boolean supportsFileDependencies() {
        return true;
    }
}

