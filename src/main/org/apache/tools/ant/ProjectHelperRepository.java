package org.apache.tools.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.helper.ProjectHelper2;
import org.apache.tools.ant.util.LoaderUtils;

/**
 * Repository of {@link ProjectHelper} found in the classpath or via
 * some System properties.

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

    public static ProjectHelperRepository getInstance() {
        return instance;
    }

    private ProjectHelperRepository() {
        collectProjectHelpers();
    }

    private void collectProjectHelpers() {
        // First, try the system property
        ProjectHelper projectHelper = getProjectHelperBySystemProperty();
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

        // last but not least, ant default project helper
        projectHelper = new ProjectHelper2();
        registerProjectHelper(projectHelper);
    }

    private void registerProjectHelper(ProjectHelper projectHelper) {
        if (projectHelper == null) {
            return;
        }
        if (DEBUG) {
            System.out.println("ProjectHelper " +
                               projectHelper.getClass().getName()
                               + " registered.");
        }
        try {
            helpers.add(projectHelper.getClass().getConstructor(NO_CLASS));
        } catch (NoSuchMethodException nse) {
            // impossible to get here
            throw new BuildException("Couldn't find no-arg constructor in "
                                     + projectHelper.getClass().getName());
        }
    }

    private ProjectHelper getProjectHelperBySystemProperty() {
        String helperClass = System.getProperty(ProjectHelper.HELPER_PROPERTY);
        try {
            if (helperClass != null) {
                return newHelper(helperClass);
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

    private ProjectHelper getProjectHelperByService(InputStream is) {
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
                return newHelper(helperClassName);
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
     * Creates a new helper instance from the name of the class. It'll
     * first try the thread class loader, then Class.forName() will
     * load from the same loader that loaded this class.
     * 
     * @param helperClass
     *            The name of the class to create an instance of. Must not be
     *            <code>null</code>.
     * 
     * @return a new instance of the specified class.
     * 
     * @exception BuildException
     *                if the class cannot be found or cannot be appropriate
     *                instantiated.
     */
    private ProjectHelper newHelper(String helperClass) throws BuildException {
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
            return ((ProjectHelper) clazz.newInstance());
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Get the helper that will be able to parse the specified file. The helper
     * will be chosen among the ones found in the classpath
     * 
     * @return the first ProjectHelper that fit the requirement (never <code>null</code>).
     */
    public ProjectHelper getProjectHelper(File buildFile) throws BuildException {
        Iterator it = getHelpers();
        while (it.hasNext()) {
            ProjectHelper helper = (ProjectHelper) it.next();
            if (helper.supportsBuildFile(buildFile)) {
                if (DEBUG) {
                    System.out.println("ProjectHelper "
                                       + helper.getClass().getName()
                                       + " selected for the file "
                                       + buildFile);
                }
                return helper;
            }
        }
        throw new RuntimeException("BUG: at least the ProjectHelper2 should "
                                   + "have supported the file " + buildFile);
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

        ConstructingIterator(Iterator nested) {
            this.nested = nested;
        }

        public boolean hasNext() {
            return nested.hasNext();
        }

        public Object next() {
            Constructor c = (Constructor) nested.next();
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
