/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.ant.common.event.BuildEvent;
import org.apache.ant.init.InitUtils;
import org.apache.tools.ant.types.Path;

/**
 * AntClassLoader facade
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 2 February 2002
 */
public class AntClassLoader extends URLClassLoader
     implements BuildListener {
    /**
     * The context loader saved when setting the thread's current context
     * loader.
     */
    private ClassLoader savedContextLoader = null;

    /**
     * Flag which indicates if this loader is currently set as the thread's
     * context loader
     */
    private boolean isContextLoaderSaved = false;

    /**
     * indicates this loader should load classes by delegating to the parent
     * loader first
     */
    private boolean parentFirst = true;

    /** label used in debugging messages */
    private String debugLabel = null;

    /** flag to indicate of debugging is turned on */
    private boolean debug = false;

    /**
     * Create an AntClassLoader
     *
     * @param project Project instance this loader is associated with
     * @param classpath the classpath to use in searching for classes
     */
    public AntClassLoader(Project project, Path classpath) {
        super(new URL[0]);
        addPath(classpath);
    }

    /**
     * Constructor for the AntClassLoader object
     *
     * @param project Project instance this loader is associated with
     * @param classpath the classpath to use in searching for classes
     * @param parentFirst true if this loader should delagate to its parent
     *      loader
     */
    public AntClassLoader(Project project, Path classpath,
                          boolean parentFirst) {
        this(project, classpath);
        this.parentFirst = parentFirst;
    }

    /**
     * Constructor for the AntClassLoader object
     *
     * @param parent the parent classloader
     * @param project Project instance this loader is associated with
     * @param classpath the classpath to use in searching for classes
     * @param parentFirst true if this loader should delagate to its parent
     *      loader
     */
    public AntClassLoader(ClassLoader parent, Project project, Path classpath,
                          boolean parentFirst) {
        super(new URL[0], parent);
        addPath(classpath);
        this.parentFirst = parentFirst;
    }

    /**
     * Initialize the given class
     *
     * @param theClass the class to be initialised
     */
    public static void initializeClass(Class theClass) {
        // do nothing in Ant2
    }

    /**
     * Set this classloader to operate in isolated mode
     *
     * @param isolated true if this loader should isolate it from other
     *      classes in the VM
     */
    public void setIsolated(boolean isolated) {
    }

    /**
     * Set the current thread's context loader to this classloader, storing
     * the current loader value for later resetting
     */
    public void setThreadContextLoader() {
        if (isContextLoaderSaved) {
            throw new BuildException("Context loader has not been reset");
        }
        Thread currentThread = Thread.currentThread();
        savedContextLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(this);
        isContextLoaderSaved = true;
    }

    /**
     * sets this loader to debug mode
     *
     * @param debug true if loads should be debugged
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
        dumpURLs();
    }

    /**
     * Sets the debugLabel of the AntClassLoader
     *
     * @param debugLabel the label to use in debug statements
     */
    public void setDebugLabel(String debugLabel) {
        this.debugLabel = debugLabel;
    }

    /** Cleanup this loader */
    public void cleanup() {
    }

    /**
     * Force a class to be loaded by this loader
     *
     * @param classname the name of the class to be loaded
     * @return an instance of the requested class
     * @exception ClassNotFoundException if the class cannot be found
     */
    public Class forceLoadClass(String classname)
         throws ClassNotFoundException {
        return super.loadClass(classname);
    }

    /** Reset the thread's class loader to its original value */
    public void resetThreadContextLoader() {
        if (!isContextLoaderSaved) {
            throw new BuildException("Context loader is not currently set");
        }
        Thread currentThread = Thread.currentThread();
        currentThread.setContextClassLoader(savedContextLoader);
        isContextLoaderSaved = false;
    }

    /**
     * build started event
     *
     * @param event build started event
     */
    public void buildStarted(BuildEvent event) {
    }

    /**
     * build finished event
     *
     * @param event build finished event
     */
    public void buildFinished(BuildEvent event) {
        cleanup();
    }

    /**
     * target started event.
     *
     * @param event target started event.
     */
    public void targetStarted(BuildEvent event) {
    }

    /**
     * target finished event
     *
     * @param event  target finished event
     */
    public void targetFinished(BuildEvent event) {
    }

    /**
     * task started event
     *
     * @param event task started event
     */
    public void taskStarted(BuildEvent event) {
    }

    /**
     * task finished event
     *
     * @param event task finished event
     */
    public void taskFinished(BuildEvent event) {
    }

    /**
     * message logged event
     *
     * @param event message logged event
     */
    public void messageLogged(BuildEvent event) {
    }

    /**
     * Add a path to this loader
     *
     * @param path the path to be added to this loader
     */
    private void addPath(Path path) {
        try {
            String[] pathElements = path.list();
            for (int i = 0; i < pathElements.length; ++i) {
                File elementFile = new File(pathElements[i]);
                URL elementURL = InitUtils.getFileURL(elementFile);
                addURL(elementURL);
            }
        } catch (MalformedURLException e) {
            throw new BuildException(e);
        }
    }

    /** Dump the URLs being used for this loader */
    private void dumpURLs() {
        if (debug && debugLabel != null) {
            System.out.println(debugLabel + ": loader URLs");
            URL[] urls = getURLs();
            for (int i = 0; i < urls.length; ++i) {
                System.out.println(debugLabel + ": URL: " + urls[i]);
            }
        }
    }

    /*
    protected Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        if (debug && debugLabel != null) {
            System.out.println(debugLabel + ": Trying to load class " + name);
        }
        Class c = findLoadedClass(name);
        if (c == null) {
            try {
                c = findClass(name);
                if (debug && debugLabel != null) {
                    System.out.println(debugLabel + ": Found class "
                        + name + " in this loader");
                }
            } catch (ClassNotFoundException e) {
                c = super.loadClass(name, resolve);
                if (debug && debugLabel != null) {
                    System.out.println(debugLabel + ": Found class "
                        + name + " in parent loader");
                }
                return c;
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }
*/
}

