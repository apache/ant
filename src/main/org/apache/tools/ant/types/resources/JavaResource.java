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
package org.apache.tools.ant.types.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;

/**
 * A Resource representation of something loadable via a Java classloader.
 * @since Ant 1.7
 */
public class JavaResource extends AbstractClasspathResource
    implements URLProvider {

    /**
     * Default constructor.
     */
    public JavaResource() {
    }

    /**
     * Construct a new JavaResource using the specified name and
     * classpath.
     *
     * @param name   the resource name.
     * @param path   the classpath.
     */
    public JavaResource(String name, Path path) {
        setName(name);
        setClasspath(path);
    }

    /**
     * open the input stream from a specific classloader
     * @param cl the classloader to use. Will be null if the system
     * classloader is used
     * @return an open input stream for the resource
     * @throws IOException if an error occurs.
     */
    protected InputStream openInputStream(ClassLoader cl) throws IOException {
        InputStream inputStream;
        if (cl == null) {
            inputStream = ClassLoader.getSystemResourceAsStream(getName());
            if (inputStream == null) {
                throw new FileNotFoundException("No resource " + getName()
                        + " on Ant's classpath");
            }
        } else {
            inputStream = cl.getResourceAsStream(getName());
            if (inputStream == null) {
                throw new FileNotFoundException("No resource " + getName()
                        + " on the classpath " + cl);
            }
        }
        return inputStream;
    }

    /**
     * Get the URL represented by this Resource.
     * @since Ant 1.8.0
     */
    @Override
    public URL getURL() {
        if (isReference()) {
            return getRef().getURL();
        }
        AbstractClasspathResource.ClassLoaderWithFlag classLoader =
            getClassLoader();
        if (classLoader.getLoader() == null) {
            return ClassLoader.getSystemResource(getName());
        }
        try {
            return classLoader.getLoader().getResource(getName());
        } finally {
            classLoader.cleanup();
        }
    }

    /**
     * Compare this JavaResource to another Resource.
     * @param another the other Resource against which to compare.
     * @return a negative integer, zero, or a positive integer as this
     * JavaResource is less than, equal to, or greater than the
     * specified Resource.
     */
    @Override
    public int compareTo(Resource another) {
        if (isReference()) {
            return getRef().compareTo(another);
        }
        if (another.getClass().equals(getClass())) {
            JavaResource otherjr = (JavaResource) another;
            if (!getName().equals(otherjr.getName())) {
                return getName().compareTo(otherjr.getName());
            }
            if (getLoader() != otherjr.getLoader()) {
                if (getLoader() == null) {
                    return -1;
                }
                if (otherjr.getLoader() == null) {
                    return 1;
                }
                return getLoader().getRefId()
                    .compareTo(otherjr.getLoader().getRefId());
            }
            Path p = getClasspath();
            Path op = otherjr.getClasspath();
            if (p != op) {
                if (p == null) {
                    return -1;
                }
                if (op == null) {
                    return 1;
                }
                return p.toString().compareTo(op.toString());
            }
            return 0;
        }
        return super.compareTo(another);
    }

    @Override
    protected JavaResource getRef() {
        return getCheckedRef(JavaResource.class);
    }
}
