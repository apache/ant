/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ClasspathUtils;

/**
 * Base class for Definitions
 * handling uri and and class loading.
 * (This was part of Definer)
 *
 * @author Costin Manolache
 * @author Stefan Bodewig
 * @author Peter Reilly
 *
 * @since Ant 1.6
 */
public abstract class DefBase extends Task implements AntlibInterface {
    private String uri = "";
    private ClassLoader internalClassLoader;
    private ClassLoader createdLoader;
    private ClasspathUtils.Delegate cpDelegate;

    /**
     * The URI for this definition.
     * If the URI is "ant:core", the uri will be set to "". (This
     * is the default uri).
     * URIs that start with "ant:" and are not
     * "ant:core" are reserved and are not allowed in this context.
     * @param uri the namespace URI
     * @throws BuildException if a reserved URI is used
     */
    public void setURI(String uri) throws BuildException {
        if (uri.equals(ProjectHelper.ANT_CORE_URI)) {
            uri = "";
        }
        if (uri.startsWith("ant:") && !uri.startsWith("antlib:")) {
            throw new BuildException("Attempt to use a reserved URI " + uri);
        }
        this.uri = uri;
    }

    /**
     * @return the namespace uri for this definition
     */
    public String getUri() {
        return uri;
    }


    /**
     * Set the class loader, overrides the cpDelagate
     * classloader.
     *
     * @param classLoader a <code>ClassLoader</code> value
     */
    public void setAntlibClassLoader(ClassLoader classLoader) {
        this.internalClassLoader = classLoader;
    }

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
     * @return the class path path for this definition
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
        if (internalClassLoader != null) {
            return internalClassLoader;
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
