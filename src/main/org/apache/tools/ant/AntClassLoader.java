/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.MalformedURLException;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.LoaderUtils;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Used to load classes within ant with a different claspath from
 * that used to start ant. Note that it is possible to force a class
 * into this loader even when that class is on the system classpath by
 * using the forceLoadClass method. Any subsequent classes loaded by that
 * class will then use this loader rather than the system class loader.
 *
 * @author Conor MacNeill
 * @author <a href="mailto:Jesse.Glick@netbeans.com">Jesse Glick</a>
 * @author Magesh Umasankar
 */
public class AntClassLoader extends ClassLoader implements BuildListener {

    /**
     * An enumeration of all resources of a given name found within the
     * classpath of this class loader. This enumeration is used by the
     * ClassLoader.findResources method, which is in
     * turn used by the ClassLoader.getResources method.
     *
     * @see AntClassLoader#findResources(String)
     * @see java.lang.ClassLoader#getResources(String)
     * @author <a href="mailto:hermand@alumni.grinnell.edu">David A. Herman</a>
     */
    private class ResourceEnumeration implements Enumeration {

        /**
         * The name of the resource being searched for.
         */
        private String resourceName;

        /**
         * The index of the next classpath element to search.
         */
        private int pathElementsIndex;

        /**
         * The URL of the next resource to return in the enumeration. If this
         * field is <code>null</code> then the enumeration has been completed,
         * i.e., there are no more elements to return.
         */
        private URL nextResource;

        /**
         * Constructs a new enumeration of resources of the given name found
         * within this class loader's classpath.
         *
         * @param name the name of the resource to search for.
         */
        ResourceEnumeration(String name) {
            this.resourceName = name;
            this.pathElementsIndex = 0;
            findNextResource();
        }

        /**
         * Indicates whether there are more elements in the enumeration to
         * return.
         *
         * @return <code>true</code> if there are more elements in the
         *         enumeration; <code>false</code> otherwise.
         */
        public boolean hasMoreElements() {
            return (this.nextResource != null);
        }

        /**
         * Returns the next resource in the enumeration.
         *
         * @return the next resource in the enumeration
         */
        public Object nextElement() {
            URL ret = this.nextResource;
            findNextResource();
            return ret;
        }

        /**
         * Locates the next resource of the correct name in the classpath and
         * sets <code>nextResource</code> to the URL of that resource. If no
         * more resources can be found, <code>nextResource</code> is set to
         * <code>null</code>.
         */
        private void findNextResource() {
            URL url = null;
            while ((pathElementsIndex < pathComponents.size()) &&
                   (url == null)) {
                try {
                    File pathComponent
                        = (File) pathComponents.elementAt(pathElementsIndex);
                    url = getResourceURL(pathComponent, this.resourceName);
                    pathElementsIndex++;
                } catch (BuildException e) {
                    // ignore path elements which are not valid relative to the
                    // project
                }
            }
            this.nextResource = url;
        }
    }

    /**
     * The size of buffers to be used in this classloader.
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * The components of the classpath that the classloader searches
     * for classes.
     */
    private Vector pathComponents  = new Vector();

    /**
     * The project to which this class loader belongs.
     */
    private Project project;

    /**
     * Indicates whether the parent class loader should be
     * consulted before trying to load with this class loader.
     */
    private boolean parentFirst = true;

    /**
     * These are the package roots that are to be loaded by the parent class
     * loader regardless of whether the parent class loader is being searched
     * first or not.
     */
    private Vector systemPackages = new Vector();

    /**
     * These are the package roots that are to be loaded by this class loader
     * regardless of whether the parent class loader is being searched first
     * or not.
     */
    private Vector loaderPackages = new Vector();

    /**
     * Whether or not this classloader will ignore the base
     * classloader if it can't find a class.
     *
     * @see #setIsolated(boolean)
     */
    private boolean ignoreBase = false;

    /**
     * The parent class loader, if one is given or can be determined.
     */
    private ClassLoader parent = null;

    /**
     * A hashtable of zip files opened by the classloader (File to ZipFile).
     */
    private Hashtable zipFiles = new Hashtable();

    /**
     * The context loader saved when setting the thread's current
     * context loader.
     */
    private ClassLoader savedContextLoader = null;
    /**
     * Whether or not the context loader is currently saved.
     */
    private boolean isContextLoaderSaved = false;

    /**
     * Reflection method reference for getProtectionDomain;
     * used to avoid 1.1-compatibility problems.
     */
    private static Method getProtectionDomain = null;

    /**
     * Reflection method reference for defineClassProtectionDomain;
     * used to avoid 1.1-compatibility problems.
     */
    private static Method defineClassProtectionDomain = null;


    // Set up the reflection-based Java2 methods if possible
    static {
        try {
            getProtectionDomain
                = Class.class.getMethod("getProtectionDomain", new Class[0]);
            Class protectionDomain
                = Class.forName("java.security.ProtectionDomain");
            Class[] args = new Class[] {String.class, byte[].class,
                Integer.TYPE, Integer.TYPE, protectionDomain};
            defineClassProtectionDomain
                = ClassLoader.class.getDeclaredMethod("defineClass", args);
        } catch (Exception e) {
            // ignore failure to get access to 1.2+ methods
        }
    }


    /**
     * Creates a classloader for the given project using the classpath given.
     *
     * @param project The project to which this classloader is to belong.
     *                Must not be <code>null</code>.
     * @param classpath The classpath to use to load the classes.  This
     *                is combined with the system classpath in a manner
     *                determined by the value of ${build.sysclasspath}.
     *                May be <code>null</code>, in which case no path
     *                elements are set up to start with.
     */
    public AntClassLoader(Project project, Path classpath) {
        parent = AntClassLoader.class.getClassLoader();
        this.project = project;
        project.addBuildListener(this);
        if (classpath != null) {
            Path actualClasspath = classpath.concatSystemClasspath("ignore");
            String[] pathElements = actualClasspath.list();
            for (int i = 0; i < pathElements.length; ++i) {
                try {
                    addPathElement(pathElements[i]);
                } catch (BuildException e) {
                    // ignore path elements which are invalid
                    // relative to the project
                }
            }
        }
    }

    /**
     * Creates a classloader for the given project using the classpath given.
     *
     * @param parent The parent classloader to which unsatisfied loading
     *               attempts are delegated. May be <code>null</code>,
     *               in which case the classloader which loaded this
     *               class is used as the parent.
     * @param project The project to which this classloader is to belong.
     *                Must not be <code>null</code>.
     * @param classpath the classpath to use to load the classes.
     *                  May be <code>null</code>, in which case no path
     *                  elements are set up to start with.
     * @param parentFirst If <code>true</code>, indicates that the parent
     *                    classloader should be consulted  before trying to
     *                    load the a class through this loader.
     */
    public AntClassLoader(ClassLoader parent, Project project, Path classpath,
                          boolean parentFirst) {
        this(project, classpath);
        if (parent != null) {
            this.parent = parent;
        }
        this.parentFirst = parentFirst;
        addJavaLibraries();
    }


    /**
     * Creates a classloader for the given project using the classpath given.
     *
     * @param project The project to which this classloader is to belong.
     *                Must not be <code>null</code>.
     * @param classpath The classpath to use to load the classes. May be
     *                  <code>null</code>, in which case no path
     *                  elements are set up to start with.
     * @param parentFirst If <code>true</code>, indicates that the parent
     *                    classloader should be consulted before trying to
     *                    load the a class through this loader.
     */
    public AntClassLoader(Project project, Path classpath,
                          boolean parentFirst) {
        this(null, project, classpath, parentFirst);
    }

    /**
     * Creates an empty class loader. The classloader should be configured
     * with path elements to specify where the loader is to look for
     * classes.
     *
     * @param parent The parent classloader to which unsatisfied loading
     *               attempts are delegated. May be <code>null</code>,
     *               in which case the classloader which loaded this
     *               class is used as the parent.
     * @param parentFirst If <code>true</code>, indicates that the parent
     *                    classloader should be consulted before trying to
     *                    load the a class through this loader.
     */
    public AntClassLoader(ClassLoader parent, boolean parentFirst) {
        if (parent != null) {
            this.parent = parent;
        } else {
            parent = AntClassLoader.class.getClassLoader();
        }
        project = null;
        this.parentFirst = parentFirst;
    }

    /**
     * Logs a message through the project object if one has been provided.
     *
     * @param message The message to log.
     *                Should not be <code>null</code>.
     *
     * @param priority The logging priority of the message.
     */
    protected void log(String message, int priority) {
        if (project != null) {
            project.log(message, priority);
        }
//         else {
//             System.out.println(message);
//         }
    }

    /**
     * Sets the current thread's context loader to this classloader, storing
     * the current loader value for later resetting.
     */
    public void setThreadContextLoader() {
        if (isContextLoaderSaved) {
            throw new BuildException("Context loader has not been reset");
        }
        if (LoaderUtils.isContextLoaderAvailable()) {
            savedContextLoader = LoaderUtils.getContextClassLoader();
            ClassLoader loader = this;
            if (project != null 
                && "only".equals(project.getProperty("build.sysclasspath"))) {
                loader = this.getClass().getClassLoader();
            }
            LoaderUtils.setContextClassLoader(loader);
            isContextLoaderSaved = true;
        }
    }

    /**
     * Resets the current thread's context loader to its original value.
     */
    public void resetThreadContextLoader() {
        if (LoaderUtils.isContextLoaderAvailable()
            && isContextLoaderSaved) {
            LoaderUtils.setContextClassLoader(savedContextLoader);
            savedContextLoader = null;
            isContextLoaderSaved = false;
        }
    }


    /**
     * Adds an element to the classpath to be searched.
     *
     * @param pathElement The path element to add. Must not be
     *                    <code>null</code>.
     *
     * @exception BuildException if the given path element cannot be resolved
     *                           against the project.
     */
    public void addPathElement(String pathElement) throws BuildException {
        File pathComponent
            = project != null ? project.resolveFile(pathElement)
                              : new File(pathElement);
        pathComponents.addElement(pathComponent);
    }

    /**
     * Returns the classpath this classloader will consult.
     *
     * @return the classpath used for this classloader, with elements
     *         separated by the path separator for the system.
     */
    public String getClasspath(){
        StringBuffer sb = new StringBuffer();
        boolean firstPass = true;
        Enumeration enum = pathComponents.elements();
        while (enum.hasMoreElements()) {
            if (!firstPass) {
                sb.append(System.getProperty("path.separator"));
            } else {
                firstPass = false;
            }
            sb.append(((File) enum.nextElement()).getAbsolutePath());
        }
        return sb.toString();
    }

    /**
     * Sets whether this classloader should run in isolated mode. In
     * isolated mode, classes not found on the given classpath will
     * not be referred to the parent class loader but will cause a
     * ClassNotFoundException.
     *
     * @param isolated Whether or not this classloader should run in
     *                 isolated mode.
     */
    public void setIsolated(boolean isolated) {
        ignoreBase = isolated;
    }

    /**
     * Forces initialization of a class in a JDK 1.1 compatible, albeit hacky
     * way.
     *
     * @param theClass The class to initialize.
     *                 Must not be <code>null</code>.
     */
    public static void initializeClass(Class theClass) {
        // ***HACK*** We ask the VM to create an instance
        // by voluntarily providing illegal arguments to force
        // the VM to run the class' static initializer, while
        // at the same time not running a valid constructor.

        final Constructor[] cons = theClass.getDeclaredConstructors();
        //At least one constructor is guaranteed to be there, but check anyway.
        if (cons != null) {
            if (cons.length > 0 && cons[0] != null) {
                final String[] strs = new String[256];
                try {
                    cons[0].newInstance(strs);
                    // Expecting an exception to be thrown by this call:
                    // IllegalArgumentException: wrong number of Arguments
                } catch (Throwable t) {
                    // Ignore - we are interested only in the side
                    // effect - that of getting the static initializers
                    // invoked.  As we do not want to call a valid
                    // constructor to get this side effect, an
                    // attempt is made to call a hopefully
                    // invalid constructor - come on, nobody
                    // would have a constructor that takes in
                    // 256 String arguments ;-)
                    // (In fact, they can't - according to JVM spec
                    // section 4.10, the number of method parameters is limited
                    // to 255 by the definition of a method descriptor.
                    // Constructors count as methods here.)
                }
            }
        }
    }

    /**
     * Adds a package root to the list of packages which must be loaded on the
     * parent loader.
     *
     * All subpackages are also included.
     *
     * @param packageRoot The root of all packages to be included.
     *                    Should not be <code>null</code>.
     */
    public void addSystemPackageRoot(String packageRoot) {
        systemPackages.addElement(packageRoot 
                                  + (packageRoot.endsWith(".") ? "" : "."));
    }

    /**
     * Adds a package root to the list of packages which must be loaded using
     * this loader.
     *
     * All subpackages are also included.
     *
     * @param packageRoot The root of all packages to be included.
     *                    Should not be <code>null</code>.
     */
    public void addLoaderPackageRoot(String packageRoot) {
        loaderPackages.addElement(packageRoot
                                  + (packageRoot.endsWith(".") ? "" : "."));
    }

    /**
     * Loads a class through this class loader even if that class is available
     * on the parent classpath.
     *
     * This ensures that any classes which are loaded by the returned class
     * will use this classloader.
     *
     * @param classname The name of the class to be loaded.
     *                  Must not be <code>null</code>.
     *
     * @return the required Class object
     *
     * @exception ClassNotFoundException if the requested class does not exist
     *                                   on this loader's classpath.
     */
    public Class forceLoadClass(String classname)
         throws ClassNotFoundException {
        log("force loading " + classname, Project.MSG_DEBUG);

        Class theClass = findLoadedClass(classname);

        if (theClass == null) {
            theClass = findClass(classname);
        }

        return theClass;
    }

    /**
     * Loads a class through this class loader but defer to the parent class
     * loader.
     *
     * This ensures that instances of the returned class will be compatible
     * with instances which which have already been loaded on the parent
     * loader.
     *
     * @param classname The name of the class to be loaded.
     *                  Must not be <code>null</code>.
     *
     * @return the required Class object
     *
     * @exception ClassNotFoundException if the requested class does not exist
     * on this loader's classpath.
     */
    public Class forceLoadSystemClass(String classname)
         throws ClassNotFoundException {
        log("force system loading " + classname, Project.MSG_DEBUG);

        Class theClass = findLoadedClass(classname);

        if (theClass == null) {
            theClass = findBaseClass(classname);
        }

        return theClass;
    }

    /**
     * Returns a stream to read the requested resource name.
     *
     * @param name The name of the resource for which a stream is required.
     *             Must not be <code>null</code>.
     *
     * @return a stream to the required resource or <code>null</code> if the
     *         resource cannot be found on the loader's classpath.
     */
    public InputStream getResourceAsStream(String name) {

        InputStream resourceStream = null;
        if (isParentFirst(name)) {
            resourceStream = loadBaseResource(name);
            if (resourceStream != null) {
                log("ResourceStream for " + name
                    + " loaded from parent loader", Project.MSG_DEBUG);

            } else {
                resourceStream = loadResource(name);
                if (resourceStream != null) {
                    log("ResourceStream for " + name
                        + " loaded from ant loader", Project.MSG_DEBUG);
                }
            }
        } else {
            resourceStream = loadResource(name);
            if (resourceStream != null) {
                log("ResourceStream for " + name
                    + " loaded from ant loader", Project.MSG_DEBUG);

            } else {
                resourceStream = loadBaseResource(name);
                if (resourceStream != null) {
                    log("ResourceStream for " + name
                        + " loaded from parent loader", Project.MSG_DEBUG);
                }
            }
        }

        if (resourceStream == null) {
            log("Couldn't load ResourceStream for " + name,
                Project.MSG_DEBUG);
        }

        return resourceStream;
    }

    /**
     * Returns a stream to read the requested resource name from this loader.
     *
     * @param name The name of the resource for which a stream is required.
     *             Must not be <code>null</code>.
     *
     * @return a stream to the required resource or <code>null</code> if
     *         the resource cannot be found on the loader's classpath.
     */
    private InputStream loadResource(String name) {
        // we need to search the components of the path to see if we can
        // find the class we want.
        InputStream stream = null;

        Enumeration e = pathComponents.elements();
        while (e.hasMoreElements() && stream == null) {
            File pathComponent = (File) e.nextElement();
            stream = getResourceStream(pathComponent, name);
        }
        return stream;
    }

    /**
     * Finds a system resource (which should be loaded from the parent
     * classloader).
     *
     * @param name The name of the system resource to load.
     *             Must not be <code>null</code>.
     *
     * @return a stream to the named resource, or <code>null</code> if
     *         the resource cannot be found.
     */
    private InputStream loadBaseResource(String name) {
        if (parent == null) {
            return getSystemResourceAsStream(name);
        } else {
            return parent.getResourceAsStream(name);
        }
    }

    /**
     * Returns an inputstream to a given resource in the given file which may
     * either be a directory or a zip file.
     *
     * @param file the file (directory or jar) in which to search for the
     *             resource. Must not be <code>null</code>.
     * @param resourceName The name of the resource for which a stream is
     *                     required. Must not be <code>null</code>.
     *
     * @return a stream to the required resource or <code>null</code> if
     *         the resource cannot be found in the given file.
     */
    private InputStream getResourceStream(File file, String resourceName) {
        try {
            if (!file.exists()) {
                return null;
            }

            if (file.isDirectory()) {
                File resource = new File(file, resourceName);

                if (resource.exists()) {
                    return new FileInputStream(resource);
                }
            } else {
                // is the zip file in the cache
                ZipFile zipFile = (ZipFile) zipFiles.get(file);
                if (zipFile == null) {
                    zipFile = new ZipFile(file);
                    zipFiles.put(file, zipFile);
                }
                ZipEntry entry = zipFile.getEntry(resourceName);
                if (entry != null) {
                    return zipFile.getInputStream(entry);
                }
            }
        } catch (Exception e) {
            log("Ignoring Exception " + e.getClass().getName()
                + ": " + e.getMessage() + " reading resource " + resourceName
                + " from " + file, Project.MSG_VERBOSE);
        }

        return null;
    }

    /**
     * Tests whether or not the parent classloader should be checked for
     * a resource before this one. If the resource matches both the
     * "use parent classloader first" and the "use this classloader first"
     * lists, the latter takes priority.
     *
     * @param resourceName The name of the resource to check.
     *                     Must not be <code>null</code>.
     *
     * @return whether or not the parent classloader should be checked for a
     *         resource before this one is.
     */
    private boolean isParentFirst(String resourceName) {
        // default to the global setting and then see
        // if this class belongs to a package which has been
        // designated to use a specific loader first
        // (this one or the parent one)

        // XXX - shouldn't this always return false in isolated mode?

        boolean useParentFirst = parentFirst;

        for (Enumeration e = systemPackages.elements(); e.hasMoreElements();) {
            String packageName = (String) e.nextElement();
            if (resourceName.startsWith(packageName)) {
                useParentFirst = true;
                break;
            }
        }

        for (Enumeration e = loaderPackages.elements(); e.hasMoreElements();) {
            String packageName = (String) e.nextElement();
            if (resourceName.startsWith(packageName)) {
                useParentFirst = false;
                break;
            }
        }

        return useParentFirst;
    }

    /**
     * Finds the resource with the given name. A resource is
     * some data (images, audio, text, etc) that can be accessed by class
     * code in a way that is independent of the location of the code.
     *
     * @param name The name of the resource for which a stream is required.
     *             Must not be <code>null</code>.
     *
     * @return a URL for reading the resource, or <code>null</code> if the
     *         resource could not be found or the caller doesn't have
     *         adequate privileges to get the resource.
     */
    public URL getResource(String name) {
        // we need to search the components of the path to see if
        // we can find the class we want.
        URL url = null;
        if (isParentFirst(name)) {
            url = (parent == null) ? super.getResource(name)
                                   : parent.getResource(name);
        }

        if (url != null) {
            log("Resource " + name + " loaded from parent loader",
                Project.MSG_DEBUG);

        } else {
            // try and load from this loader if the parent either didn't find
            // it or wasn't consulted.
            Enumeration e = pathComponents.elements();
            while (e.hasMoreElements() && url == null) {
                File pathComponent = (File) e.nextElement();
                url = getResourceURL(pathComponent, name);
                if (url != null) {
                    log("Resource " + name
                        + " loaded from ant loader",
                        Project.MSG_DEBUG);
                }
            }
        }

        if (url == null && !isParentFirst(name)) {
            // this loader was first but it didn't find it - try the parent

            url = (parent == null) ? super.getResource(name)
                : parent.getResource(name);
            if (url != null) {
                log("Resource " + name + " loaded from parent loader",
                    Project.MSG_DEBUG);
            }
        }

        if (url == null) {
            log("Couldn't load Resource " + name, Project.MSG_DEBUG);
        }

        return url;
    }

    /**
     * Returns an enumeration of URLs representing all the resources with the
     * given name by searching the class loader's classpath.
     *
     * @param name The resource name to search for.
     *             Must not be <code>null</code>.
     * @return an enumeration of URLs for the resources
     * @exception IOException if I/O errors occurs (can't happen)
     */
    protected Enumeration findResources(String name) throws IOException {
        return new ResourceEnumeration(name);
    }

    /**
     * Returns an inputstream to a given resource in the given file which may
     * either be a directory or a zip file.
     *
     * @param file The file (directory or jar) in which to search for
     *             the resource. Must not be <code>null</code>.
     * @param resourceName The name of the resource for which a stream
     *                     is required. Must not be <code>null</code>.
     *
     * @return a stream to the required resource or <code>null</code> if the
     *         resource cannot be found in the given file object.
     */
    private URL getResourceURL(File file, String resourceName) {
        try {
            if (!file.exists()) {
                return null;
            }

            if (file.isDirectory()) {
                File resource = new File(file, resourceName);

                if (resource.exists()) {
                    try {
                        return new URL("file:" + resource.toString());
                    } catch (MalformedURLException ex) {
                        return null;
                    }
                }
            } else {
                ZipFile zipFile = (ZipFile) zipFiles.get(file);
                if (zipFile == null) {
                    zipFile = new ZipFile(file);
                    zipFiles.put(file, zipFile);
                }

                ZipEntry entry = zipFile.getEntry(resourceName);
                if (entry != null) {
                    try {
                        return new URL("jar:file:" + file.toString()
                            + "!/" + entry);
                    } catch (MalformedURLException ex) {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Loads a class with this class loader.
     *
     * This class attempts to load the class in an order determined by whether
     * or not the class matches the system/loader package lists, with the
     * loader package list taking priority. If the classloader is in isolated
     * mode, failure to load the class in this loader will result in a
     * ClassNotFoundException.
     *
     * @param classname The name of the class to be loaded.
     *                  Must not be <code>null</code>.
     * @param resolve <code>true</code> if all classes upon which this class
     *                depends are to be loaded.
     *
     * @return the required Class object
     *
     * @exception ClassNotFoundException if the requested class does not exist
     * on the system classpath (when not in isolated mode) or this loader's
     * classpath.
     */
    protected synchronized Class loadClass(String classname, boolean resolve)
         throws ClassNotFoundException {
        // 'sync' is needed - otherwise 2 threads can load the same class
        // twice, resulting in LinkageError: duplicated class definition.
        // findLoadedClass avoids that, but without sync it won't work.

        Class theClass = findLoadedClass(classname);
        if (theClass != null) {
            return theClass;
        }

        if (isParentFirst(classname)) {
            try {
                theClass = findBaseClass(classname);
                log("Class " + classname + " loaded from parent loader",
                    Project.MSG_DEBUG);
            } catch (ClassNotFoundException cnfe) {
                theClass = findClass(classname);
                log("Class " + classname + " loaded from ant loader",
                    Project.MSG_DEBUG);
            }
        } else {
            try {
                theClass = findClass(classname);
                log("Class " + classname + " loaded from ant loader",
                    Project.MSG_DEBUG);
            } catch (ClassNotFoundException cnfe) {
                if (ignoreBase) {
                    throw cnfe;
                }
                theClass = findBaseClass(classname);
                log("Class " + classname + " loaded from parent loader",
                    Project.MSG_DEBUG);
            }
        }

        if (resolve) {
            resolveClass(theClass);
        }

        return theClass;
    }

    /**
     * Converts the class dot notation to a filesystem equivalent for
     * searching purposes.
     *
     * @param classname The class name in dot format (eg java.lang.Integer).
     *                  Must not be <code>null</code>.
     *
     * @return the classname in filesystem format (eg java/lang/Integer.class)
     */
    private String getClassFilename(String classname) {
        return classname.replace('.', '/') + ".class";
    }

    /**
     * Reads a class definition from a stream.
     *
     * @param stream The stream from which the class is to be read.
     *               Must not be <code>null</code>.
     * @param classname The name of the class in the stream.
     *                  Must not be <code>null</code>.
     *
     * @return the Class object read from the stream.
     *
     * @exception IOException if there is a problem reading the class from the
     * stream.
     * @exception SecurityException if there is a security problem while
     * reading the class from the stream.
     */
    private Class getClassFromStream(InputStream stream, String classname)
                throws IOException, SecurityException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bytesRead = -1;
        byte[] buffer = new byte[BUFFER_SIZE];

        while ((bytesRead = stream.read(buffer, 0, BUFFER_SIZE)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }

        byte[] classData = baos.toByteArray();

        // Simply put:
        // defineClass(classname, classData, 0, classData.length,
        //             Project.class.getProtectionDomain());
        // Made more elaborate to be 1.1-safe.
        if (defineClassProtectionDomain != null) {
            try {
                Object domain
                    = getProtectionDomain.invoke(Project.class, new Object[0]);
                Object[] args
                    = new Object[] {classname, classData, new Integer(0),
                                    new Integer(classData.length), domain};
                return (Class) defineClassProtectionDomain.invoke(this, args);
            } catch (InvocationTargetException ite) {
                Throwable t = ite.getTargetException();
                if (t instanceof ClassFormatError) {
                    throw (ClassFormatError) t;
                } else if (t instanceof NoClassDefFoundError) {
                    throw (NoClassDefFoundError) t;
                } else if (t instanceof SecurityException) {
                    throw (SecurityException) t;
                } else {
                    throw new IOException(t.toString());
                }
            } catch (Exception e) {
                throw new IOException(e.toString());
            }
        } else {
            return defineClass(classname, classData, 0, classData.length);
        }
    }

    /**
     * Searches for and load a class on the classpath of this class loader.
     *
     * @param name The name of the class to be loaded. Must not be
     *             <code>null</code>.
     *
     * @return the required Class object
     *
     * @exception ClassNotFoundException if the requested class does not exist
     *                                   on this loader's classpath.
     */
    public Class findClass(String name) throws ClassNotFoundException {
        log("Finding class " + name, Project.MSG_DEBUG);

        return findClassInComponents(name);
    }


    /**
     * Finds a class on the given classpath.
     *
     * @param name The name of the class to be loaded. Must not be
     *             <code>null</code>.
     *
     * @return the required Class object
     *
     * @exception ClassNotFoundException if the requested class does not exist
     * on this loader's classpath.
     */
    private Class findClassInComponents(String name)
         throws ClassNotFoundException {
        // we need to search the components of the path to see if
        // we can find the class we want.
        InputStream stream = null;
        String classFilename = getClassFilename(name);
        try {
            Enumeration e = pathComponents.elements();
            while (e.hasMoreElements()) {
                File pathComponent = (File) e.nextElement();
                try {
                    stream = getResourceStream(pathComponent, classFilename);
                    if (stream != null) {
                        return getClassFromStream(stream, name);
                    }
                } catch (SecurityException se) {
                    throw se;
                } catch (IOException ioe) {
                    // ioe.printStackTrace();
                    log("Exception reading component " + pathComponent ,
                        Project.MSG_VERBOSE);
                }
            }

            throw new ClassNotFoundException(name);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {}
        }
    }

    /**
     * Finds a system class (which should be loaded from the same classloader
     * as the Ant core).
     *
     * For JDK 1.1 compatability, this uses the findSystemClass method if
     * no parent classloader has been specified.
     *
     * @param name The name of the class to be loaded.
     *             Must not be <code>null</code>.
     *
     * @return the required Class object
     *
     * @exception ClassNotFoundException if the requested class does not exist
     * on this loader's classpath.
     */
    private Class findBaseClass(String name) throws ClassNotFoundException {
        if (parent == null) {
            return findSystemClass(name);
        } else {
            return parent.loadClass(name);
        }
    }

    /**
     * Cleans up any resources held by this classloader. Any open archive
     * files are closed.
     */
    public synchronized void cleanup() {
        for (Enumeration e = zipFiles.elements(); e.hasMoreElements();) {
            ZipFile zipFile = (ZipFile)e.nextElement();
            try {
                zipFile.close();
            } catch (IOException ioe) {
                // ignore
            }
        }
        zipFiles = new Hashtable();
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     *
     * @param event the buildStarted event
     */
    public void buildStarted(BuildEvent event) {
    }

    /**
     * Cleans up any resources held by this classloader at the end
     * of a build.
     *
     * @param event the buildFinished event
     */
    public void buildFinished(BuildEvent event) {
        project.removeBuildListener(this);
        project = null;
        cleanup();
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     *
     * @param event the targetStarted event
     */
    public void targetStarted(BuildEvent event) {
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     *
     * @param event the targetFinished event
     */
    public void targetFinished(BuildEvent event) {
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     *
     * @param event the taskStarted event
     */
    public void taskStarted(BuildEvent event) {
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     *
     * @param event the taskFinished event
     */
    public void taskFinished(BuildEvent event) {
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     *
     * @param event the messageLogged event
     */
    public void messageLogged(BuildEvent event) {
    }
    
    /**
     * add any libraries that come with different java versions
     * here
     */
    private void addJavaLibraries() {
        Vector packages=JavaEnvUtils.getJrePackages();
        Enumeration e=packages.elements();
        while(e.hasMoreElements()) {
            String packageName=(String)e.nextElement();
            addSystemPackageRoot(packageName);
        }
    }
    
}
