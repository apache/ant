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
package org.apache.tools.ant.types.resources;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.Reference;

/**
 * A Resource representation of something loadable via a Java classloader.
 * @since Ant 1.7
 */
public class JavaResource extends Resource {

    private Path classpath;
    private Reference loader;

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
        classpath = path;
    }

    /**
     * Set the classpath to use when looking up a resource.
     * @param classpath to add to any existing classpath
     */
    public void setClasspath(Path classpath) {
        checkAttributesAllowed();
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }

    /**
     * Add a classpath to use when looking up a resource.
     * @return The classpath to be configured
     */
    public Path createClasspath() {
        checkChildrenAllowed();
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        return this.classpath.createPath();
    }

    /**
     * Set the classpath to use when looking up a resource,
     * given as reference to a &lt;path&gt; defined elsewhere
     * @param r The reference value
     */
    public void setClasspathRef(Reference r) {
        checkAttributesAllowed();
        createClasspath().setRefid(r);
    }

    /**
     * get the classpath used by this <code>LoadProperties</code>.
     * @return The classpath
     */
    public Path getClasspath() {
        return isReference()
            ? ((JavaResource) getCheckedRef()).getClasspath() : classpath;
    }

    /**
     * Use the reference to locate the loader. If the loader is not
     * found, taskdef will use the specified classpath and register it
     * with the specified name.
     *
     * This allow multiple taskdef/typedef to use the same class loader,
     * so they can be used together. It eliminate the need to
     * put them in the CLASSPATH.
     *
     * @param r the reference to locate the loader.
     */
    public void setLoaderRef(Reference r) {
        checkAttributesAllowed();
        loader = r;
    }

    /**
     * Overrides the super version.
     * @param r the Reference to set.
     */
    public void setRefid(Reference r) {
        if (loader != null || classpath != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Learn whether this file exists.
     * @return true if this resource exists.
     */
    public boolean isExists() {
        InputStream is = null;
        try {
            return isReference() ? ((Resource) getCheckedRef()).isExists()
                : (is = getInputStream()) != null;
        } catch (IOException ex) {
            return false;
        } finally {
            FileUtils.close(is);
        }
    }

    /**
     * Return an InputStream for reading the contents of this Resource.
     * @return an InputStream object.
     * @throws IOException if an error occurs.
     */
    public InputStream getInputStream() throws IOException {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getInputStream();
        }
        ClassLoader cl = null;
        if (loader != null) {
            cl = (ClassLoader) loader.getReferencedObject();
        }
        if (cl == null) {
            if (getClasspath() != null) {
                cl = getProject().createClassLoader(classpath);
            } else {
                cl = JavaResource.class.getClassLoader();
            }
            if (loader != null && cl != null) {
                getProject().addReference(loader.getRefId(), cl);
            }
        }

        return cl == null ? ClassLoader.getSystemResourceAsStream(getName())
            : cl.getResourceAsStream(getName());
    }

    /**
     * Compare this JavaResource to another Resource.
     * @param another the other Resource against which to compare.
     * @return a negative integer, zero, or a positive integer as this
     * JavaResource is less than, equal to, or greater than the
     * specified Resource.
     */
    public int compareTo(Object another) {
        if (isReference()) {
            return ((Comparable) getCheckedRef()).compareTo(another);
        }
        if (another.getClass().equals(getClass())) {
            JavaResource otherjr = (JavaResource) another;
            if (!getName().equals(otherjr.getName())) {
                return getName().compareTo(otherjr.getName());
            }
            if (loader != otherjr.loader) {
                if (loader == null) {
                    return -1;
                }
                if (otherjr.loader == null) {
                    return 1;
                }
                return loader.getRefId().compareTo(otherjr.loader.getRefId());
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

}
