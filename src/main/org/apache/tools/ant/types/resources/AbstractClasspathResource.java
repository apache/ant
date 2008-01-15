/*
 * Copyright  2007 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileUtils;

import java.io.InputStream;
import java.io.IOException;

/**
 *
 * A Resource representation of anything that is accessed via a Java classloader.
 * The core methods to set/resolve the classpath are provided.
 * @since Ant 1.8
 *
 */

public abstract class AbstractClasspathResource extends Resource {
    protected Path classpath;
    protected Reference loader;

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
        if (classpath == null) {
            classpath = new Path(getProject());
        }
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
     * Learn whether this resource exists. This implementation opens the input stream
     * as the test.
     * @return true if this resource exists.
     */
    public boolean isExists() {
        if (isReference()) {
            return  ((Resource) getCheckedRef()).isExists();
        }
        InputStream is = null;
        try {
            is = getInputStream();
            return is != null;
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

        return openInputStream(cl);
    }

    /**
     * open the inpout stream from a specific classloader
     * @param cl the classloader to use. Will be null if the system classloader is used
     * @return an open input stream for the resource
     * @throws IOException if an error occurs.
     */
    protected abstract InputStream openInputStream(ClassLoader cl) throws IOException;
}
