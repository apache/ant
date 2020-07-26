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
package org.apache.tools.ant.util;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

import java.lang.reflect.InvocationTargetException;

// CheckStyle:HideUtilityClassConstructorCheck OFF - bc

/**
 * Offers some helper methods on the Path structure in ant.
 *
 * <p>The basic idea behind this utility class is to use it from inside the
 * different Ant objects (and user defined objects) that need classLoading
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

    /**
     * Name of the magic property that controls classloader reuse in Ant 1.4.
     */
    public static final String REUSE_LOADER_REF = MagicNames.REFID_CLASSPATH_REUSE_LOADER;

    /**
     * Convenience overloaded version of {@link
     * #getClassLoaderForPath(Project, Reference, boolean)}.
     *
     * <p>Assumes the logical 'false' for the reverseLoader.</p>
     *
     * @param p the project
     * @param ref the reference
     * @return The class loader
     */
    public static ClassLoader getClassLoaderForPath(Project p, Reference ref) {
        return getClassLoaderForPath(p, ref, false);
    }

    /**
     * Convenience overloaded version of {@link #getClassLoaderForPath(Project, Path,
     * String, boolean)}.
     *
     * <p>Delegates to the other one after extracting the referenced
     * Path from the Project. This checks also that the passed
     * Reference is pointing to a Path all right.</p>
     * @param p current Ant project
     * @param ref Reference to Path structure
     * @param reverseLoader if set to true this new loader will take
     * precedence over its parent (which is contra the regular
     * classloader behaviour)
     * @return The class loader
     */
    public static ClassLoader getClassLoaderForPath(
        Project p, Reference ref, boolean reverseLoader) {
        String pathId = ref.getRefId();
        Object path = p.getReference(pathId);
        if (!(path instanceof Path)) {
            throw new BuildException(
                "The specified classpathref %s does not reference a Path.",
                pathId);
        }
        String loaderId = MagicNames.REFID_CLASSPATH_LOADER_PREFIX + pathId;
        return getClassLoaderForPath(p, (Path) path, loaderId, reverseLoader);
    }

    /**
     * Convenience overloaded version of {@link
     * #getClassLoaderForPath(Project, Path, String, boolean)}.
     *
     * <p>Assumes the logical 'false' for the reverseLoader.</p>
     *
     * @param p current Ant project
     * @param path the path
     * @param loaderId the loader id string
     * @return The class loader
     */
    public static ClassLoader getClassLoaderForPath(Project p, Path path, String loaderId) {
        return getClassLoaderForPath(p, path, loaderId, false);
    }

    /**
     * Convenience overloaded version of {@link
     * #getClassLoaderForPath(Project, Path, String, boolean, boolean)}.
     *
     * <p>Sets value for 'reuseLoader' to true if the magic property
     * has been set.</p>
     *
     * @param p the project
     * @param path the path
     * @param loaderId the loader id string
     * @param reverseLoader if set to true this new loader will take
     * precedence over its parent (which is contra the regular
     * classloader behaviour)
     * @return The class loader
     */
    public static ClassLoader getClassLoaderForPath(
            Project p, Path path, String loaderId, boolean reverseLoader) {
        return getClassLoaderForPath(p, path, loaderId, reverseLoader, isMagicPropertySet(p));
    }

    /**
     * Gets a classloader that loads classes from the classpath
     * defined in the path argument.
     *
     * <p>Based on the setting of the magic property
     * 'ant.reuse.loader' this will try to reuse the previously
     * created loader with that id, and of course store it there upon
     * creation.</p>
     * @param p             Ant Project where the handled components are living in.
     * @param path          Path object to be used as classpath for this classloader
     * @param loaderId      identification for this Loader,
     * @param reverseLoader if set to true this new loader will take
     *                      precedence over its parent (which is contra the regular
     *                      classloader behaviour)
     * @param reuseLoader   if true reuse the loader if it is found
     * @return              ClassLoader that uses the Path as its classpath.
     */
    public static ClassLoader getClassLoaderForPath(
            Project p, Path path, String loaderId, boolean reverseLoader, boolean reuseLoader) {
        ClassLoader cl = null;

        // magic property
        if (loaderId != null && reuseLoader) {
            Object reusedLoader = p.getReference(loaderId);
            if (reusedLoader != null && !(reusedLoader instanceof ClassLoader)) {
                throw new BuildException(
                    "The specified loader id %s does not reference a class loader",
                    loaderId);
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
     * Gets a fresh, different, previously unused classloader that uses the
     * passed path as its classpath.
     *
     * <p>This method completely ignores the ant.reuse.loader magic
     * property and should be used with caution.</p>
     * @param p             Ant Project where the handled components are living in.
     * @param path          the classpath for this loader
     * @param reverseLoader if set to true this new loader will take
     *                      precedence over its parent (which is contra the regular
     *                      classloader behaviour)
     * @return The fresh, different, previously unused class loader.
     */
    public static ClassLoader getUniqueClassLoaderForPath(Project p, Path path,
            boolean reverseLoader) {
        AntClassLoader acl = p.createClassLoader(path);
        if (reverseLoader) {
            acl.setParentFirst(false);
            acl.addJavaLibraries();
        }
        return acl;
    }

    /**
     * Creates a fresh object instance of the specified classname.
     *
     * <p>This uses the userDefinedLoader to load the specified class,
     * and then makes an instance using the default no-argument constructor.</p>
     *
     * @param className the full qualified class name to load.
     * @param userDefinedLoader the classloader to use.
     * @return The fresh object instance
     * @throws BuildException when loading or instantiation failed.
     */
    public static Object newInstance(String className, ClassLoader userDefinedLoader) {
        return newInstance(className, userDefinedLoader, Object.class);
    }

    /**
     * Creates a fresh object instance of the specified classname.
     *
     * <p>This uses the userDefinedLoader to load the specified class,
     * and then makes an instance using the default no-argument constructor.</p>
     *
     * @param <T> desired type
     * @param className the full qualified class name to load.
     * @param userDefinedLoader the classloader to use.
     * @param expectedType the Class that the result should be assignment
     * compatible with. (No ClassCastException will be thrown in case
     * the result of this method is casted to the expectedType)
     * @return The fresh object instance
     * @throws BuildException when loading or instantiation failed.
     * @since Ant 1.7
     */
    public static <T> T newInstance(String className, ClassLoader userDefinedLoader,
            Class<T> expectedType) {
        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) Class.forName(className, true, userDefinedLoader);
            T o = clazz.getDeclaredConstructor().newInstance();
            if (!expectedType.isInstance(o)) {
                throw new BuildException(
                    "Class of unexpected Type: %s expected : %s", className,
                    expectedType);
            }
            return o;
        } catch (ClassNotFoundException e) {
            throw new BuildException("Class not found: " + className, e);
        } catch (InstantiationException e) {
            throw new BuildException("Could not instantiate " + className
                    + ". Specified class should have a no " + "argument constructor.", e);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new BuildException("Could not instantiate " + className
                    + ". Specified class should have a " + "public constructor.", e);
        } catch (LinkageError e) {
            throw new BuildException("Class " + className
                    + " could not be loaded because of an invalid dependency.", e);
        }
    }

    /**
     * Obtains a delegate that helps out with classic classpath configuration.
     *
     * @param component your projectComponent that needs the assistance
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

    private ClasspathUtils() {
    }

    /**
     * Delegate that helps out any specific ProjectComponent that needs
     * dynamic classloading.
     *
     * <p>Ant ProjectComponents that need a to be able to dynamically load
     * Classes and instantiate them often expose the following ant syntax
     * sugar: </p>
     *
     * <ul><li>nested &lt;classpath&gt;</li>
     * <li>attribute @classpathref</li>
     * <li>attribute @classname</li></ul>
     *
     * <p>This class functions as a delegate handling the configuration
     * issues for this recurring pattern.  Its usage pattern, as the name
     * suggests, is delegation rather than inheritance.</p>
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
         * Construct a Delegate
         * @param component the ProjectComponent this delegate is for.
         */
        Delegate(ProjectComponent component) {
            this.component = component;
        }

        /**
         * This method is a Delegate method handling the @classpath attribute.
         *
         * <p>This attribute can set a path to add to the classpath.</p>
         *
         * @param classpath the path to use for the classpath.
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
         * classpath.</p>
         *
         * @return the created path.
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
         * to load and instantiate.</p>
         *
         * @param fcqn the name of the class to load.
         */
        public void setClassname(String fcqn) {
            this.className = fcqn;
        }

        /**
         * Delegate method handling the @classpathref attribute.
         *
         * <p>This attribute can add a referenced path-like structure to the
         * classpath.</p>
         *
         * @param r the reference to the classpath.
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
         * <p>By default this is supposed to be false.</p>
         *
         * <p>Caution: this behaviour is contradictory to the normal way
         * classloaders work.  Do not let your ProjectComponent use it if
         * you are not really sure.</p>
         *
         * @param reverseLoader if true reverse the order of looking up a class.
         */
        public void setReverseLoader(boolean reverseLoader) {
            this.reverseLoader = reverseLoader;
        }

        /**
         * Sets the loaderRef.
         * @param r the reference to the loader.
         */
        public void setLoaderRef(Reference r) {
            this.loaderId = r.getRefId();
        }


        /**
         * Finds or creates the classloader for this object.
         * @return The class loader.
         */
        public ClassLoader getClassLoader() {
            return getClassLoaderForPath(getContextProject(), classpath, getClassLoadId(),
                    reverseLoader, loaderId != null || isMagicPropertySet(getContextProject()));
        }

        /**
         * The project of the ProjectComponent we are working for.
         */
        private Project getContextProject() {
            return component.getProject();
        }

        /**
         * Computes the loaderId based on the configuration of the component.
         * @return a loader identifier.
         */
        public String getClassLoadId() {
            if (loaderId == null && classpathId != null) {
                return MagicNames.REFID_CLASSPATH_LOADER_PREFIX + classpathId;
            } else {
                return loaderId;
            }
        }

        /**
         * Helper method obtaining a fresh instance of the class specified
         * in the @classname and using the specified classpath.
         *
         * @return the fresh instantiated object.
         */
        public Object newInstance() {
            return ClasspathUtils.newInstance(this.className, getClassLoader());
        }

        /**
         * The classpath.
         * @return the classpath.
         */
        public Path getClasspath() {
            return classpath;
        }

        /**
         * Get the reverseLoader setting.
         * @return true if looking up in reverse order.
         */
        public boolean isReverseLoader() {
            return reverseLoader;
        }

    }
}
