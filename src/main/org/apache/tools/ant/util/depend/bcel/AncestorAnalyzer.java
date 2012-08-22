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
import org.apache.bcel.classfile.JavaClass;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.depend.AbstractAnalyzer;

/**
 * A dependency analyzer which returns superclass and superinterface
 * dependencies.
 *
 */
public class AncestorAnalyzer extends AbstractAnalyzer {

    /**
     * Default constructor
     *
     * Causes the BCEL classes to load to ensure BCEL dependencies can
     * be satisfied
     */
    public AncestorAnalyzer() {
        // force BCEL classes to load now
        try {
            new ClassParser("force");
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
    protected void determineDependencies(Vector<File> files, Vector<String> classes) {
        // we get the root classes and build up a set of
        // classes upon which they depend
        Hashtable<String, String> dependencies = new Hashtable<String, String>();
        Hashtable<File, File> containers = new Hashtable<File, File>();
        Hashtable<String, String> toAnalyze = new Hashtable<String, String>();
        Hashtable<String, String> nextAnalyze = new Hashtable<String, String>();

        for (Enumeration<String> e = getRootClasses(); e.hasMoreElements();) {
            String classname = e.nextElement();
            toAnalyze.put(classname, classname);
        }

        int count = 0;
        int maxCount = isClosureRequired() ? MAX_LOOPS : 2;
        while (toAnalyze.size() != 0 && count++ < maxCount) {
            nextAnalyze.clear();
            for (String classname : toAnalyze.keySet()) {
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
                    String[] interfaces = javaClass.getInterfaceNames();
                    for (int i = 0; i < interfaces.length; ++i) {
                        String interfaceName = interfaces[i];
                        if (!dependencies.containsKey(interfaceName)) {
                            nextAnalyze.put(interfaceName, interfaceName);
                        }
                    }

                    if (javaClass.isClass()) {
                        String superClass = javaClass.getSuperclassName();
                        if (!dependencies.containsKey(superClass)) {
                            nextAnalyze.put(superClass, superClass);
                        }
                    }
                } catch (IOException ioe) {
                    // ignore
                }
            }

            Hashtable<String, String> temp = toAnalyze;
            toAnalyze = nextAnalyze;
            nextAnalyze = temp;
        }

        files.removeAllElements();
        for (File f : containers.keySet()) {
            files.add(f);
        }

        classes.removeAllElements();
        for (String dependency : dependencies.keySet()) {
            classes.add(dependency);
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

