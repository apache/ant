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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.util.FileUtils;

/**
 * A Resource representation of anything that is accessed via a Java classloader.
 * The core methods to set/resolve the classpath are provided.
 *
 * @since Ant 1.8.0
 */

public abstract class AbstractClasspathResource extends Resource {
    private Path classpath;
    private Reference loader;
    private boolean parentFirst = true;

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
        setChecked(false);
    }

    /**
     * Add a classpath to use when looking up a resource.
     * @return The classpath to be configured
     */
    public Path createClasspath() {
        checkChildrenAllowed();
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        setChecked(false);
        return classpath.createPath();
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
        if (isReference()) {
            return getRef().getClasspath();
        }
        dieOnCircularReference();
        return classpath;
    }

    /**
     * Get the loader.
     * @return the loader.
     */
    public Reference getLoader() {
        if (isReference()) {
            return getRef().getLoader();
        }
        dieOnCircularReference();
        return loader;
    }

    /**
     * Use the reference to locate the loader. If the loader is not
     * found, taskdef will use the specified classpath and register it
     * with the specified name.
     * <p>
     * This allow multiple taskdef/typedef to use the same class loader,
     * so they can be used together. It eliminate the need to
     * put them in the CLASSPATH.
     * </p>
     *
     * @param r the reference to locate the loader.
     */
    public void setLoaderRef(Reference r) {
        checkAttributesAllowed();
        loader = r;
    }

    /**
     * Whether to consult the parent classloader first.
     *
     * <p>Only relevant if a classpath has been specified.</p>
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setParentFirst(boolean b) {
        parentFirst = b;
    }

    /**
     * Overrides the super version.
     *
     * @param r the Reference to set.
     */
    public void setRefid(Reference r) {
        if (loader != null || classpath != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Learn whether this resource exists. This implementation opens the input stream
     * as the test.
     *
     * @return true if this resource exists.
     */
    public boolean isExists() {
        if (isReference()) {
            return getRef().isExists();
        }
        dieOnCircularReference();
        try (InputStream is = getInputStream()) {
            return is != null;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Return an InputStream for reading the contents of this Resource.
     *
     * @return an InputStream object.
     * @throws IOException if an error occurs.
     */
    public InputStream getInputStream() throws IOException {
        if (isReference()) {
            return getRef().getInputStream();
        }
        dieOnCircularReference();

        final ClassLoaderWithFlag classLoader = getClassLoader();
        return !classLoader.needsCleanup()
            ? openInputStream(classLoader.getLoader())
            : new FilterInputStream(openInputStream(classLoader.getLoader())) {
                @Override
                public void close() throws IOException {
                    FileUtils.close(in);
                    classLoader.cleanup();
                }

                @Override
                protected void finalize() throws Throwable {
                    try {
                        close();
                    } finally {
                        super.finalize();
                    }
                }
            };
    }

    /**
     * combines the various ways that could specify a ClassLoader and
     * potentially creates one that needs to be cleaned up when it is
     * no longer needed so that classes can get garbage collected.
     *
     * @return ClassLoaderWithFlag
     */
    protected ClassLoaderWithFlag getClassLoader() {
        ClassLoader cl = null;
        if (loader != null) {
            cl = loader.getReferencedObject();
        }
        boolean clNeedsCleanup = false;
        if (cl == null) {
            if (getClasspath() != null) {
                Path p = getClasspath().concatSystemClasspath("ignore");
                if (parentFirst) {
                    cl = getProject().createClassLoader(p);
                } else {
                    cl = AntClassLoader.newAntClassLoader(getProject()
                                                          .getCoreLoader(),
                                                          getProject(),
                                                          p, false);
                }
                clNeedsCleanup = loader == null;
            } else {
                cl = JavaResource.class.getClassLoader();
            }
            if (loader != null && cl != null) {
                getProject().addReference(loader.getRefId(), cl);
            }
        }
        return new ClassLoaderWithFlag(cl, clNeedsCleanup);
    }

    /**
     * open the input stream from a specific classloader
     *
     * @param cl the classloader to use. Will be null if the system classloader is used
     * @return an open input stream for the resource
     * @throws IOException if an error occurs.
     */
    protected abstract InputStream openInputStream(ClassLoader cl) throws IOException;

    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p) {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            if (classpath != null) {
                pushAndInvokeCircularReferenceCheck(classpath, stk, p);
            }
            setChecked(true);
        }
    }

    @Override
    protected AbstractClasspathResource getRef() {
        return getCheckedRef(AbstractClasspathResource.class);
    }

    public static class ClassLoaderWithFlag {
        private final ClassLoader loader;
        private final boolean cleanup;

        ClassLoaderWithFlag(ClassLoader l, boolean needsCleanup) {
            loader = l;
            cleanup = needsCleanup && l instanceof AntClassLoader;
        }

        public ClassLoader getLoader() {
            return loader;
        }

        public boolean needsCleanup() {
            return cleanup;
        }

        public void cleanup() {
            if (cleanup) {
                ((AntClassLoader) loader).cleanup();
            }
        }
    }
}
