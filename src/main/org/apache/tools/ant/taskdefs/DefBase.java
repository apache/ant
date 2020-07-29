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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ClasspathUtils;

/**
 * Base class for Definitions handling uri and class loading.
 * (This was part of Definer)
 *
 * @since Ant 1.6
 */
public abstract class DefBase extends AntlibDefinition {
    private ClassLoader createdLoader;
    private ClasspathUtils.Delegate cpDelegate;

    /**
     * Check if classpath attributes have been set.
     * (to be called before getCpDelegate() is used.
     * @return true if cpDelegate has been created.
     */
    protected boolean hasCpDelegate() {
        return cpDelegate != null;
    }

    /**
     * @param reverseLoader if true a delegated loader will take precedence over
     *                      the parent
     * @deprecated since 1.6.x.
     *             stop using this attribute
     * @ant.attribute ignore="true"
     */
    @Deprecated
    public void setReverseLoader(boolean reverseLoader) {
        getDelegate().setReverseLoader(reverseLoader);
        log("The reverseloader attribute is DEPRECATED. It will be removed",
            Project.MSG_WARN);
    }

    /**
     * @return the classpath for this definition
     */
    public Path getClasspath() {
        return getDelegate().getClasspath();
    }

    /**
     * @return the reverse loader attribute of the classpath delegate.
     */
    public boolean isReverseLoader() {
        return getDelegate().isReverseLoader();
    }

    /**
     * Returns the loader id of the class path Delegate.
     * @return the loader id
     */
    public String getLoaderId() {
        return getDelegate().getClassLoadId();
    }

    /**
     * Returns the class path id of the class path delegate.
     * @return the class path id
     */
    public String getClasspathId() {
        return getDelegate().getClassLoadId();
    }

    /**
     * Set the classpath to be used when searching for component being defined.
     *
     * @param classpath an Ant Path object containing the classpath.
     */
    public void setClasspath(Path classpath) {
        getDelegate().setClasspath(classpath);
    }

    /**
     * Create the classpath to be used when searching for component being
     * defined.
     * @return the classpath of the this definition
     */
    public Path createClasspath() {
        return getDelegate().createClasspath();
    }

    /**
     * Set a reference to a classpath to use when loading the files.
     * To actually share the same loader, set loaderref as well
     * @param r the reference to the classpath
     */
    public void setClasspathRef(Reference r) {
        getDelegate().setClasspathref(r);
    }

    /**
     * Use the reference to locate the loader. If the loader is not
     * found, the specified classpath will be used and registered
     * with the specified name.
     *
     * This allows multiple taskdef/typedef to use the same class loader,
     * so they can be used together, eliminating the need to
     * put them in the CLASSPATH.
     *
     * @param r the reference to locate the loader.
     * @since Ant 1.5
     */
    public void setLoaderRef(Reference r) {
        getDelegate().setLoaderRef(r);
    }

    /**
     * create a classloader for this definition
     * @return the classloader from the cpDelegate
     */
    protected ClassLoader createLoader() {
        if (getAntlibClassLoader() != null && cpDelegate == null) {
            return getAntlibClassLoader();
        }
        if (createdLoader == null) {
            createdLoader = getDelegate().getClassLoader();
            // need to load Task via system classloader or the new
            // task we want to define will never be a Task but always
            // be wrapped into a TaskAdapter.
            ((AntClassLoader) createdLoader)
                .addSystemPackageRoot(MagicNames.ANT_CORE_PACKAGE);
        }
        return createdLoader;
    }

    /**
     * @see org.apache.tools.ant.Task#init()
     * @throws BuildException on error.
     * @since Ant 1.6
     */
    public void init() throws BuildException {
        super.init();
    }

    private ClasspathUtils.Delegate getDelegate() {
        if (cpDelegate == null) {
            cpDelegate = ClasspathUtils.getDelegate(this);
        }
        return cpDelegate;
    }
}
