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
package org.apache.tools.ant;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.helper.ProjectHelper2;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.util.LoaderUtils;

/**
 * Repository of {@link ProjectHelper} found in the classpath or via
 * some System properties.
 * 
 * <p>See the ProjectHelper documentation in the manual.</p>
 * 
 * @since Ant 1.8.0
 */
public class ProjectHelperRepository {

    private static final String DEBUG_PROJECT_HELPER_REPOSITORY =
        "ant.project-helper-repo.debug";

    // The message log level is not accessible here because everything
    // is instanciated statically
    private static final boolean DEBUG =
        "true".equals(System.getProperty(DEBUG_PROJECT_HELPER_REPOSITORY));

    private static ProjectHelperRepository instance =
        new ProjectHelperRepository();

    private List/* <Constructor> */ helpers = new ArrayList();

    private static final Class[] NO_CLASS = new Class[0];
    private static final Object[] NO_OBJECT = new Object[0];

    private static Constructor PROJECTHELPER2_CONSTRUCTOR;

    static {
        try {
            PROJECTHELPER2_CONSTRUCTOR = ProjectHelper2.class
                    .getConstructor(NO_CLASS);
        } catch (Exception e) {
            // ProjectHelper2 must be available
            throw new RuntimeException(e);
        }
    }

    public static ProjectHelperRepository getInstance() {
        return instance;
    }

    private ProjectHelperRepository() {
        collectProjectHelpers();
    }

    private void collectProjectHelpers() {
        // First, try the system property
        Constructor projectHelper = getProjectHelperBySystemProperty();
        registerProjectHelper(projectHelper);

        // A JDK1.3 'service' ( like in JAXP ). That will plug a helper
        // automatically if in CLASSPATH, with the right META-INF/services.
        try {
            ClassLoader classLoader = LoaderUtils.getContextClassLoader();
            if (classLoader != null) {
                Enumeration resources =
                    classLoader.getResources(ProjectHelper.SERVICE_ID);
                while (resources.hasMoreElements()) {
                    URL resource = (URL) resources.nextElement();
                    projectHelper =
                        getProjectHelperByService(resource.openStream());
                    registerProjectHelper(projectHelper);
                }
            }

            InputStream systemResource =
                ClassLoader.getSystemResourceAsStream(ProjectHelper.SERVICE_ID);
            if (systemResource != null) {
                projectHelper = getProjectHelperByService(systemResource);
                registerProjectHelper(projectHelper);
            }
        } catch (Exception e) {
            System.err.println("Unable to load ProjectHelper from service "
                               + ProjectHelper.SERVICE_ID + " ("
                               + e.getClass().getName()
                               + ": " + e.getMessage() + ")");
            if (DEBUG) {
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * Register the specified project helper into the repository.
     * <p>
     * The helper will be added after all the already registered helpers, but
     * before the default one (ProjectHelper2)
     * 
     * @param helperClassName
     *            the fully qualified name of the helper
     * @throws BuildException
     *             if the class cannot be loaded or if there is no constructor
     *             with no argument
     * @since Ant 1.8.2
     */
    public void registerProjectHelper(String helperClassName)
            throws BuildException {
        registerProjectHelper(getHelperConstructor(helperClassName));
    }

    /**
     * Register the specified project helper into the repository.
     * <p>
     * The helper will be added after all the already registered helpers, but
     * before the default one (ProjectHelper2)
     * 
     * @param helperClass
     *            the class of the helper
     * @throws BuildException
     *             if there is no constructor with no argument
     * @since Ant 1.8.2
     */
    public void registerProjectHelper(Class helperClass) throws BuildException {
        try {
            registerProjectHelper(helperClass.getConstructor(NO_CLASS));
        } catch (NoSuchMethodException e) {
            throw new BuildException("Couldn't find no-arg constructor in "
                    + helperClass.getName());
        }
    }

    private void registerProjectHelper(Constructor helperConstructor) {
        if (helperConstructor == null) {
            return;
        }
        if (DEBUG) {
            System.out.println("ProjectHelper "
                    + helperConstructor.getClass().getName() + " registered.");
        }
        helpers.add(helperConstructor);
    }

    private Constructor getProjectHelperBySystemProperty() {
        String helperClass = System.getProperty(ProjectHelper.HELPER_PROPERTY);
        try {
            if (helperClass != null) {
                return getHelperConstructor(helperClass);
            }
        } catch (SecurityException e) {
            System.err.println("Unable to load ProjectHelper class \""
                               + helperClass + " specified in system property "
                               + ProjectHelper.HELPER_PROPERTY + " ("
                               + e.getMessage() + ")");
            if (DEBUG) {
                e.printStackTrace(System.err);
            }
        }
        return null;
    }

    private Constructor getProjectHelperByService(InputStream is) {
        try {
            // This code is needed by EBCDIC and other strange systems.
            // It's a fix for bugs reported in xerces
            InputStreamReader isr;
            try {
                isr = new InputStreamReader(is, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                isr = new InputStreamReader(is);
            }
            BufferedReader rd = new BufferedReader(isr);

            String helperClassName = rd.readLine();
            rd.close();

            if (helperClassName != null && !"".equals(helperClassName)) {
                return getHelperConstructor(helperClassName);
            }
        } catch (Exception e) {
            System.out.println("Unable to load ProjectHelper from service "
                    + ProjectHelper.SERVICE_ID + " (" + e.getMessage() + ")");
            if (DEBUG) {
                e.printStackTrace(System.err);
            }
        }
        return null;
    }

    /**
     * Get the constructor with not argument of an helper from its class name.
     * It'll first try the thread class loader, then Class.forName() will load
     * from the same loader that loaded this class.
     * 
     * @param helperClass
     *            The name of the class to create an instance of. Must not be
     *            <code>null</code>.
     * 
     * @return the constructor of the specified class.
     * 
     * @exception BuildException
     *                if the class cannot be found or if a constructor with no
     *                argument cannot be found.
     */
    private Constructor getHelperConstructor(String helperClass) throws BuildException {
        ClassLoader classLoader = LoaderUtils.getContextClassLoader();
        try {
            Class clazz = null;
            if (classLoader != null) {
                try {
                    clazz = classLoader.loadClass(helperClass);
                } catch (ClassNotFoundException ex) {
                    // try next method
                }
            }
            if (clazz == null) {
                clazz = Class.forName(helperClass);
            }
            return clazz.getConstructor(NO_CLASS);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Get the helper that will be able to parse the specified build file. The helper
     * will be chosen among the ones found in the classpath
     * 
     * @return the first ProjectHelper that fit the requirement (never <code>null</code>).
     */
    public ProjectHelper getProjectHelperForBuildFile(Resource buildFile) throws BuildException {
        Iterator it = getHelpers();
        while (it.hasNext()) {
            ProjectHelper helper = (ProjectHelper) it.next();
            if (helper.canParseBuildFile(buildFile)) {
                if (DEBUG) {
                    System.out.println("ProjectHelper "
                                       + helper.getClass().getName()
                                       + " selected for the build file "
                                       + buildFile);
                }
                return helper;
            }
        }
        throw new RuntimeException("BUG: at least the ProjectHelper2 should "
                                   + "have supported the file " + buildFile);
    }

    /**
     * Get the helper that will be able to parse the specified antlib. The helper
     * will be chosen among the ones found in the classpath
     * 
     * @return the first ProjectHelper that fit the requirement (never <code>null</code>).
     */
    public ProjectHelper getProjectHelperForAntlib(Resource antlib) throws BuildException {
        Iterator it = getHelpers();
        while (it.hasNext()) {
            ProjectHelper helper = (ProjectHelper) it.next();
            if (helper.canParseAntlibDescriptor(antlib)) {
                if (DEBUG) {
                    System.out.println("ProjectHelper "
                                       + helper.getClass().getName()
                                       + " selected for the antlib "
                                       + antlib);
                }
                return helper;
            }
        }
        throw new RuntimeException("BUG: at least the ProjectHelper2 should "
                                   + "have supported the file " + antlib);
    }

    /**
     * Get an iterator on the list of project helpers configured. The iterator
     * will always return at least one element as there will always be the
     * default project helper configured.
     * 
     * @return an iterator of {@link ProjectHelper}
     */
    public Iterator getHelpers() {
        return new ConstructingIterator(helpers.iterator());
    }

    private static class ConstructingIterator implements Iterator {
        private final Iterator nested;
        private boolean empty = false;

        ConstructingIterator(Iterator nested) {
            this.nested = nested;
        }

        public boolean hasNext() {
            return nested.hasNext() || !empty;
        }

        public Object next() {
            Constructor c;
            if (nested.hasNext()) {
                c = (Constructor) nested.next();
            } else {
                // last but not least, ant default project helper
                empty = true;
                c = PROJECTHELPER2_CONSTRUCTOR;
            }
            try {
                return c.newInstance(NO_OBJECT);
            } catch (Exception e) {
                throw new BuildException("Failed to invoke no-arg constructor"
                                         + " on " + c.getName());
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("remove is not supported");
        }
    }
}
