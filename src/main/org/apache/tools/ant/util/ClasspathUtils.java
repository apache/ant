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
package org.apache.tools.ant.util;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Offers some helper methods on the Path structure in ant.
 *
 * <p>Basic idea behind this utility class is to use it from inside the
 * different ant objects (and user defined objects) that need classLoading
 * for their operation.
 * Normally those would have a setClasspathRef() {for the @classpathref}
 * and/or a createClasspath() {for the nested &lt;classpath&gt;}
 * Typically one would have in your Ant Task or DataType</p>
 *
 * <pre><code>
 * ClasspathUtils.Delegate cpDelegate;
 *
 * public void init() {
 *     this.cpDelegate = ClasspathUtils.getDelegate(this);
 *     super.init();
 * }
 *
 * public void setClasspathRef(Reference r) {
 *     this.cpDelegate.setClasspathRef(r);
 * }
 *
 * public Path createClasspath() {
 *     return this.cpDelegate.createClasspath();
 * }
 *
 * public void setClassname(String fqcn) {
 *     this.cpDelegate.setClassname(fqcn);
 * }
 * </code></pre>
 *
 * <p>At execution time, when you actually need the classloading
 * you can just:</p>
 *
 * <pre><code>
 *     Object o = this.cpDelegate.newInstance();
 * </code></pre>
 *
 * @since Ant 1.6
 */
public class ClasspathUtils {
    private static final String LOADER_ID_PREFIX = "ant.loader.";
    /**
     * Name of the magic property that controls classloader reuse in Ant 1.4.
     */
    public static final String REUSE_LOADER_REF = "ant.reuse.loader";

    /**
     * Convenience overloaded version of {@link
     * #getClassLoaderForPath(Project, Reference, boolean)}.
     *
     * <p>Assumes the logical 'false' for the reverseLoader.</p>
     *
     * @param p
     * @param ref
     * @return
     */
    public static ClassLoader getClassLoaderForPath(
        Project p, Reference ref) {

        return getClassLoaderForPath(p, ref, false);
    }

    /**
     * Convenience overloaded version of {@link #getClassLoaderForPath(Project, Path,
     * String, boolean)}.
     *
     * <p>Delegates to the other one after extracting the referenced
     * Path from the Project This checks also that the passed
     * Reference is pointing to a Path all right.</p>
     * @param p current ant project
     * @param ref Reference to Path structure
     * @param reverseLoader if set to true this new loader will take
     * precedence over it's parent (which is contra the regular
     * classloader behaviour)
     * @return
     */
    public static ClassLoader getClassLoaderForPath(
        Project p, Reference ref, boolean reverseLoader) {

        String pathId = ref.getRefId();
        Object path = p.getReference(pathId);
        if (!(path instanceof Path)) {
            throw new BuildException(
                "The specified classpathref "
                    + pathId
                    + " does not reference a Path.");
        }
        String loaderId = LOADER_ID_PREFIX + pathId;
        return getClassLoaderForPath(p, (Path) path, loaderId, reverseLoader);
    }

    /**
     * Convenience overloaded version of {@link
     * #getClassLoaderForPath(Project, Path, String, boolean)}.
     *
     * <p>Assumes the logical 'false' for the reverseLoader.</p>
     *
     * @param path
     * @param loaderId
     * @return
     */
    public static ClassLoader getClassLoaderForPath(
        Project p, Path path, String loaderId) {

        return getClassLoaderForPath(p, path, loaderId, false);
    }

    /**
     * Convenience overloaded version of {@link
     * #getClassLoaderForPath(Project, Path, String, boolean, boolean)}.
     *
     * <p>Sets value for 'reuseLoader' to true if the magic property
     * has been set.</p>
     *
     * @param path
     * @param loaderId
     * @return
     */
    public static ClassLoader getClassLoaderForPath(
        Project p, Path path, String loaderId, boolean reverseLoader) {
        return getClassLoaderForPath(p, path, loaderId, reverseLoader,
                                     isMagicPropertySet(p));
    }

    /**
     * Gets a classloader that loads classes from the classpath
     * defined in the path argument.
     *
     * <p>Based on the setting of the magic property
     * 'ant.reuse.loader' this will try to reuse the perviously
     * created loader with that id, and of course store it there upon
     * creation.</p>
     * @param path Path object to be used as classpath for this classloader
     * @param loaderId identification for this Loader,
     * @param reverseLoader if set to true this new loader will take
     * precedence over it's parent (which is contra the regular
     * @param p Ant Project where the handled components are living in.
     * classloader behaviour)
     * @return ClassLoader that uses the Path as its classpath.
     */
    public static ClassLoader getClassLoaderForPath(
        Project p, Path path, String loaderId, boolean reverseLoader,
        boolean reuseLoader) {

        ClassLoader cl = null;

        // magic property
        if (loaderId != null && reuseLoader) {
            Object reusedLoader = p.getReference(loaderId);
            if (reusedLoader != null
                && !(reusedLoader instanceof ClassLoader)) {
                throw new BuildException("The specified loader id " + loaderId
                    + " does not reference a class loader");
            }

            cl = (ClassLoader) reusedLoader;
        }
        if (cl == null) {
            cl = getUniqueClassLoaderForPath(p, path, reverseLoader);
            if (loaderId != null && reuseLoader) {
                p.addReference(loaderId, cl);
            }
        }

        return cl;
    }

    /**
     * Gets a fresh, different, not used before classloader that uses the
     * passed path as it's classpath.
     *
     * <p>This method completely ignores the ant.reuse.loader magic
     * property and should be used with caution.</p>
     * @param path the classpath for this loader
     * @param reverseLoader
     * @return
     */
    public static ClassLoader getUniqueClassLoaderForPath(
        Project p,
        Path path,
        boolean reverseLoader) {

        AntClassLoader acl = p.createClassLoader(path != null
                                                 ? path : Path.systemClasspath);
        if (reverseLoader) {
            acl.setParentFirst(false);
            acl.addJavaLibraries();
        }

        return acl;
    }

    /**
     * Creates a fresh object instance of the specified classname.
     *
     * <p> This uses the userDefinedLoader to load the specified class,
     * and then makes an instance using the default no-argument constructor
     * </p>
     *
     * @param className the full qualified class name to load.
     * @param userDefinedLoader the classloader to use.
     * @return
     * @throws BuildException when loading or instantiation failed.
     */
    public static Object newInstance(
        String className,
        ClassLoader userDefinedLoader) {
        try {
            Class clazz = userDefinedLoader.loadClass(className);
            Object o = clazz.newInstance();
            return o;
        } catch (ClassNotFoundException e) {
            throw new BuildException(
                "Class "
                    + className
                    + " not found by the specific classLoader.",
                e);
        } catch (InstantiationException e) {
            throw new BuildException(
                "Could not instantiate "
                    + className
                    + ". Specified class should have a no "
                    + "argument constructor.",
                e);
        } catch (IllegalAccessException e) {
            throw new BuildException(
                "Could not instantiate "
                    + className
                    + ". Specified class should have a "
                    + "public constructor.",
                e);
        }
    }

    /**
     * Obtains a delegate that helps out with classic classpath configuration.
     *
     * @param component your projectComponent that needs the assistence
     * @return the helper, delegate.
     * @see ClasspathUtils.Delegate
     */
    public static Delegate getDelegate(ProjectComponent component) {
        return new Delegate(component);
    }

    /**
     * Checks for the magic property that enables class loader reuse
     * for <taskdef> and <typedef> in Ant 1.5 and earlier.
     */
    private static boolean isMagicPropertySet(Project p) {
        return p.getProperty(REUSE_LOADER_REF) != null;
    }

    /**
     * Delegate that helps out any specific ProjectComponent that needs
     * dynamic classloading.
     *
     * <p>Ant ProjectComponents that need a to be able to dynamically load
     * Classes and instantiate them often expose the following ant syntax
     * sugar: </p>
     *
     * <ul><li> nested &lt;classpath&gt; </li>
     * <li> attribute @classpathref </li>
     * <li> attribute @classname </li></ul>
     *
     * <p> This class functions as a delegate handling the configuration
     * issues for this recuring pattern.  Its usage pattern, as the name
     * suggests is delegation, not inheritance. </p>
     *
     * @since Ant 1.6
     */
    public static class Delegate {
        private final ProjectComponent component;
        private Path classpath;
        private String classpathId;
        private String className;
        private String loaderId;
        private boolean reverseLoader = false;

        /**
         * Constructs Delegate
         * @param component
         */
        Delegate(ProjectComponent component) {
            this.component = component;
        }

        /**
         * Delegate method handling the @classpath attribute
         *
         * <p>This attribute can set a path to add to the classpath</p>
         *
         * @param classpath
         */
        public void setClasspath(Path classpath) {
            if (this.classpath == null) {
                this.classpath = classpath;
            } else {
                this.classpath.append(classpath);
            }
        }

        /**
         * Delegate method handling the &lt;classpath&gt; tag.
         *
         * <p>This nested path-like structure can set a path to add to the
         * classpath</p>
         *
         * @return
         */
        public Path createClasspath() {
            if (this.classpath == null) {
                this.classpath = new Path(component.getProject());
            }
            return this.classpath.createPath();
        }

        /**
         * Delegate method handling the @classname attribute.
         *
         * <p>This attribute sets the full qualified class name of the class
         * to lad and instantiate</p>
         *
         * @param fcqn
         */
        public void setClassname(String fcqn) {
            this.className = fcqn;
        }

        /**
         * Delegate method handling the @classpathref attribute.
         *
         * <p>This attribute can add a referenced path-like structure to the
         * classpath</p>
         *
         * @param r
         */
        public void setClasspathref(Reference r) {
            this.classpathId = r.getRefId();
            createClasspath().setRefid(r);
        }

        /**
         * Delegate method handling the @reverseLoader attribute.
         *
         * <p>This attribute can set a boolean indicating that the used
         * classloader should NOT follow the classical parent-first scheme.
         * </p>
         *
         * <p>By default this is supposed to be false</p>
         *
         * <p>Caution: this behaviour is contradictory to the normal way
         * classloaders work.  Do not let your ProjectComponent use it if
         * you are not really sure</p>
         *
         * @param reverseLoader
         */
        public void setReverseLoader(boolean reverseLoader) {
            this.reverseLoader = reverseLoader;
        }

        /**
         * Sets the loaderRef
         * @param r
         */
        public void setLoaderRef(Reference r) {
            this.loaderId = r.getRefId();
        }


        /**
         * Finds or creates the classloader for this
         * @return
         */
        public ClassLoader getClassLoader() {
            ClassLoader cl;
            cl = ClasspathUtils.getClassLoaderForPath(
                    getContextProject(),
                    this.classpath,
                    getClassLoadId(),
                    this.reverseLoader,
                    loaderId != null || isMagicPropertySet(getContextProject()));
            return cl;
        }

        /**
         * The project of the ProjectComponent we are working for.
         */
        private Project getContextProject() {
            return this.component.getProject();
        }

        /**
         * Computes the loaderId based on the configuration of the component.
         */
        public String getClassLoadId() {
            if (this.loaderId == null && this.classpathId != null) {
                return ClasspathUtils.LOADER_ID_PREFIX + this.classpathId;
            } else {
                return this.loaderId;
            }
        }

        /**
         * Helper method obtaining a fresh instance of the class specified
         * in the @classname and using the specified classpath.
         *
         * @return the fresh instantiated object.
         */
        public Object newInstance() {
            ClassLoader cl = getClassLoader();
            return ClasspathUtils.newInstance(this.className, cl);
        }

        /**
         * The classpath.
         */
        public Path getClasspath() {
            return classpath;
        }

        public boolean isReverseLoader() {
            return reverseLoader;
        }

        //TODO no methods yet for getClassname
        //TODO no method for newInstance using a reverse-classloader
    }
}
