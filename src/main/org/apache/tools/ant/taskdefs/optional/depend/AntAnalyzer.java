/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.depend;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.tools.ant.util.depend.AbstractAnalyzer;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

/**
 * An analyzer which uses the depend task's bytecode classes to analyze
 * dependencies
 *
 * @author Conor MacNeill
 */
public class AntAnalyzer extends AbstractAnalyzer {
    public AntAnalyzer() {
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
        int maxCount = isClosureRequired() ? MAX_LOOPS : 1;
        Hashtable analyzedDeps = null;
        while (toAnalyze.size() != 0 && count++ < maxCount) {
            analyzedDeps = new Hashtable();
            for (Enumeration e = toAnalyze.keys(); e.hasMoreElements();) {
                String classname = (String) e.nextElement();
                dependencies.put(classname, classname);
                try {
                    File container = getClassContainer(classname);
                    if (container == null) {
                        continue;
                    }
                    containers.put(container, container);

                    ZipFile zipFile = null;
                    InputStream inStream = null;
                    try {
                        if (container.getName().endsWith(".class")) {
                            inStream = new FileInputStream(container.getPath());
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
                        Vector dependencyList = classFile.getClassRefs();
                        Enumeration depEnum = dependencyList.elements();
                        while (depEnum.hasMoreElements()) {
                            String dependency = (String) depEnum.nextElement();
                            analyzedDeps.put(dependency, dependency);
                        }
                    } finally {
                        if (inStream != null) {
                            inStream.close();
                        }
                        if (zipFile != null) {
                            zipFile.close();
                        }
                    }
                } catch (IOException ioe) {
                    // ignore
                }
            }

            toAnalyze.clear();

            // now recover all the dependencies collected and add to the list.
            Enumeration depsEnum = analyzedDeps.elements();
            while (depsEnum.hasMoreElements()) {
                String className = (String) depsEnum.nextElement();
                if (!dependencies.containsKey(className)) {
                    toAnalyze.put(className, className);
                }
            }
        }

        // pick up the last round of dependencies that were determined
        Enumeration depsEnum = analyzedDeps.elements();
        while (depsEnum.hasMoreElements()) {
            String className = (String) depsEnum.nextElement();
            dependencies.put(className, className);
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

