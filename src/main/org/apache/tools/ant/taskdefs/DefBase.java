/*
 * Copyright  2003-2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ClasspathUtils;

/**
 * Base class for Definitions
 * handling uri and class loading.
 * (This was part of Definer)
 *
 * @author Costin Manolache
 * @author Stefan Bodewig
 * @author Peter Reilly
 *
 * @since Ant 1.6
 */
public abstract class DefBase extends AntlibDefinition {
    private ClassLoader createdLoader;
    private ClasspathUtils.Delegate cpDelegate;

    /**
     * @param reverseLoader if true a delegated loader will take precedence over
     *                      the parent
     * @deprecated stop using this attribute
     * @ant.attribute ignore="true"
     */
    public void setReverseLoader(boolean reverseLoader) {
        this.cpDelegate.setReverseLoader(reverseLoader);
        log("The reverseloader attribute is DEPRECATED. It will be removed",
            Project.MSG_WARN);
    }

    /**
     * @return the classpath for this definition
     */
    public Path getClasspath() {
        return cpDelegate.getClasspath();
    }

    /**
     * @return the reverse loader attribute of the classpath delegate.
     */
    public boolean isReverseLoader() {
        return cpDelegate.isReverseLoader();
    }

    /**
     * Returns the loader id of the class path Delegate.
     * @return the loader id
     */
    public String getLoaderId() {
        return cpDelegate.getClassLoadId();
    }

    /**
     * Returns the class path id of the class path delegate.
     * @return the class path id
     */
    public String getClasspathId() {
        return cpDelegate.getClassLoadId();
    }

    /**
     * Set the classpath to be used when searching for component being defined
     *
     * @param classpath an Ant Path object containing the classpath.
     */
    public void setClasspath(Path classpath) {
        this.cpDelegate.setClasspath(classpath);
    }

    /**
     * Create the classpath to be used when searching for component being
     * defined
     * @return the classpath of the this definition
     */
    public Path createClasspath() {
        return this.cpDelegate.createClasspath();
    }

    /**
     * reference to a classpath to use when loading the files.
     * To actually share the same loader, set loaderref as well
     * @param r the reference to the classpath
     */
    public void setClasspathRef(Reference r) {
        this.cpDelegate.setClasspathref(r);
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
     * @since Ant 1.5
     */
    public void setLoaderRef(Reference r) {
        this.cpDelegate.setLoaderRef(r);
    }

    /**
     * create a classloader for this definition
     * @return the classloader from the cpDelegate
     */
    protected ClassLoader createLoader() {
        if (getAntlibClassLoader() != null) {
            return getAntlibClassLoader();
        }
        if (createdLoader == null) {
            createdLoader = this.cpDelegate.getClassLoader();
            // need to load Task via system classloader or the new
            // task we want to define will never be a Task but always
            // be wrapped into a TaskAdapter.
            ((AntClassLoader) createdLoader)
                .addSystemPackageRoot("org.apache.tools.ant");
        }
        return createdLoader;
    }

    /**
     * @see org.apache.tools.ant.Task#init()
     * @since Ant 1.6
     */
    public void init() throws BuildException {
        this.cpDelegate = ClasspathUtils.getDelegate(this);
        super.init();
    }


}
