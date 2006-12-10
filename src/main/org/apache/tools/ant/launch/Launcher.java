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
package org.apache.tools.ant.launch;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.io.File;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;



/**
 * This is a launcher for Ant.
 *
 * @since Ant 1.6
 */
public class Launcher {

    /**
     * The Ant Home (installation) Directory property.
     * {@value}
     */
    public static final String ANTHOME_PROPERTY = "ant.home";

    /**
     * The Ant Library Directory property.
     * {@value}
     */
    public static final String ANTLIBDIR_PROPERTY = "ant.library.dir";

    /**
     * The directory name of the per-user ant directory.
     * {@value}
     */
    public static final String ANT_PRIVATEDIR = ".ant";

    /**
     * The name of a per-user library directory.
     * {@value}
     */
    public static final String ANT_PRIVATELIB = "lib";

    /**
     * The location of a per-user library directory.
     * <p>
     * It's value is the concatenation of {@link #ANT_PRIVATEDIR}
     * with {@link #ANT_PRIVATELIB}, with an appropriate file separator
     * in between. For example, on Unix, it's <code>.ant/lib</code>.
     */
    public static final String USER_LIBDIR =
        ANT_PRIVATEDIR + File.separatorChar + ANT_PRIVATELIB;

    /**
     * The startup class that is to be run.
     * {@value}
     */
    public static final String MAIN_CLASS = "org.apache.tools.ant.Main";

    /**
     * System property with user home directory.
     * {@value}
     */
    public static final String USER_HOMEDIR = "user.home";

    /**
     * System property with application classpath.
     * {@value}
     */
    private static final String JAVA_CLASS_PATH = "java.class.path";

    /**
     * Exit code on trouble
     */
    protected static final int EXIT_CODE_ERROR = 2;

    /**
     * Entry point for starting command line Ant.
     *
     * @param  args commandline arguments
     */
    public static void main(String[] args) {
        int exitCode;
        try {
            Launcher launcher = new Launcher();
            exitCode = launcher.run(args);
        } catch (LaunchException e) {
            exitCode = EXIT_CODE_ERROR;
            System.err.println(e.getMessage());
        } catch (Throwable t) {
            exitCode = EXIT_CODE_ERROR;
            t.printStackTrace(System.err);
        }
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }


    /**
     * Add a CLASSPATH or -lib to lib path urls.
     *
     * @param path        the classpath or lib path to add to the libPathULRLs
     * @param getJars     if true and a path is a directory, add the jars in
     *                    the directory to the path urls
     * @param libPathURLs the list of paths to add to
     */
    private void addPath(String path, boolean getJars, List libPathURLs)
            throws MalformedURLException {
        StringTokenizer tokenizer = new StringTokenizer(path, File.pathSeparator);
        while (tokenizer.hasMoreElements()) {
            String elementName = tokenizer.nextToken();
            File element = new File(elementName);
            if (elementName.indexOf("%") != -1 && !element.exists()) {
                continue;
            }
            if (getJars && element.isDirectory()) {
                // add any jars in the directory
                URL[] dirURLs = Locator.getLocationURLs(element);
                for (int j = 0; j < dirURLs.length; ++j) {
                    libPathURLs.add(dirURLs[j]);
                }
            }

            libPathURLs.add(Locator.fileToURL(element));
        }
    }

    /**
     * Run the launcher to launch Ant.
     *
     * @param args the command line arguments
     * @return an exit code. As the normal ant main calls exit when it ends,
     *         this is for handling failures at bind-time
     * @exception MalformedURLException if the URLs required for the classloader
     *            cannot be created.
     */
    private int run(String[] args)
            throws LaunchException, MalformedURLException {
        String antHomeProperty = System.getProperty(ANTHOME_PROPERTY);
        File antHome = null;

        File sourceJar = Locator.getClassSource(getClass());
        File jarDir = sourceJar.getParentFile();
        String mainClassname = MAIN_CLASS;

        if (antHomeProperty != null) {
            antHome = new File(antHomeProperty);
        }

        if (antHome == null || !antHome.exists()) {
            antHome = jarDir.getParentFile();
            System.setProperty(ANTHOME_PROPERTY, antHome.getAbsolutePath());
        }

        if (!antHome.exists()) {
            throw new LaunchException("Ant home is set incorrectly or "
                + "ant could not be located");
        }

        List libPaths = new ArrayList();
        String cpString = null;
        List argList = new ArrayList();
        String[] newArgs;
        boolean  noUserLib = false;
        boolean  noClassPath = false;

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-lib")) {
                if (i == args.length - 1) {
                    throw new LaunchException("The -lib argument must "
                        + "be followed by a library location");
                }
                libPaths.add(args[++i]);
            } else if (args[i].equals("-cp")) {
                if (i == args.length - 1) {
                    throw new LaunchException("The -cp argument must "
                        + "be followed by a classpath expression");
                }
                if (cpString != null) {
                    throw new LaunchException("The -cp argument must "
                        + "not be repeated");
                }
                cpString = args[++i];
            } else if (args[i].equals("--nouserlib") || args[i].equals("-nouserlib")) {
                noUserLib = true;
            } else if (args[i].equals("--noclasspath") || args[i].equals("-noclasspath")) {
                noClassPath = true;
            } else if (args[i].equals("-main")) {
                if (i == args.length - 1) {
                    throw new LaunchException("The -main argument must "
                            + "be followed by a library location");
                }
                mainClassname = args[++i];
            } else {
                argList.add(args[i]);
            }
        }

        //decide whether to copy the existing arg set, or
        //build a new one from the list of all args excluding the special
        //operations that only we handle
        if (argList.size() == args.length) {
            newArgs = args;
        } else {
            newArgs = (String[]) argList.toArray(new String[argList.size()]);
        }

        URL[] libURLs    = getLibPathURLs(
            noClassPath ? null : cpString, libPaths);
        URL[] systemURLs = getSystemURLs(jarDir);
        URL[] userURLs   = noUserLib ? new URL[0] : getUserURLs();

        URL[] jars = getJarArray(
            libURLs, userURLs, systemURLs, Locator.getToolsJar());

        // now update the class.path property
        StringBuffer baseClassPath
            = new StringBuffer(System.getProperty(JAVA_CLASS_PATH));
        if (baseClassPath.charAt(baseClassPath.length() - 1)
                == File.pathSeparatorChar) {
            baseClassPath.setLength(baseClassPath.length() - 1);
        }

        for (int i = 0; i < jars.length; ++i) {
            baseClassPath.append(File.pathSeparatorChar);
            baseClassPath.append(Locator.fromURI(jars[i].toString()));
        }

        System.setProperty(JAVA_CLASS_PATH, baseClassPath.toString());

        URLClassLoader loader = new URLClassLoader(jars);
        Thread.currentThread().setContextClassLoader(loader);
        Class mainClass = null;
        int exitCode = 0;
        try {
            mainClass = loader.loadClass(mainClassname);
            AntMain main = (AntMain) mainClass.newInstance();
            main.startAnt(newArgs, null, null);
        } catch (InstantiationException ex) {
            System.err.println(
                "Incompatible version of " + mainClassname + " detected");
            File mainJar = Locator.getClassSource(mainClass);
            System.err.println(
                "Location of this class " + mainJar);
            exitCode = EXIT_CODE_ERROR;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            exitCode = EXIT_CODE_ERROR;
        }
        return exitCode;
    }

    /**
     * Get the list of -lib enties and -cp entry into
     * a URL array.
     * @param cpString the classpath string
     * @param libPaths the list of -lib entries.
     * @return an array of URLs.
     */
    private URL[] getLibPathURLs(String cpString, List libPaths)
        throws MalformedURLException {
        List libPathURLs = new ArrayList();

        if (cpString != null) {
            addPath(cpString, false, libPathURLs);
        }

        for (Iterator i = libPaths.iterator(); i.hasNext();) {
            String libPath = (String) i.next();
            addPath(libPath, true, libPathURLs);
        }

        return  (URL[]) libPathURLs.toArray(new URL[libPathURLs.size()]);
    }

    /**
     * Get the jar files in ANT_HOME/lib.
     * determine ant library directory for system jars: use property
     * or default using location of ant-launcher.jar
     */
    private URL[] getSystemURLs(File antLauncherDir) throws MalformedURLException {
        File antLibDir = null;
        String antLibDirProperty = System.getProperty(ANTLIBDIR_PROPERTY);
        if (antLibDirProperty != null) {
            antLibDir = new File(antLibDirProperty);
        }
        if ((antLibDir == null) || !antLibDir.exists()) {
            antLibDir = antLauncherDir;
            System.setProperty(ANTLIBDIR_PROPERTY, antLibDir.getAbsolutePath());
        }
        return Locator.getLocationURLs(antLibDir);
    }

    /**
     * Get the jar files in user.home/.ant/lib
     */
    private URL[] getUserURLs() throws MalformedURLException {
        File userLibDir
            = new File(System.getProperty(USER_HOMEDIR), USER_LIBDIR);

        return Locator.getLocationURLs(userLibDir);
    }

    /**
     * Combine the various jar sources into a single array of jars.
     * @param libJars the jars specified in -lib command line options
     * @param userJars the jars in ~/.ant/lib
     * @param systemJars the jars in $ANT_HOME/lib
     * @param toolsJar   the tools.jar file
     * @return a combined array
     * @throws MalformedURLException if there is a problem.
     */
    private URL[] getJarArray (
        URL[] libJars, URL[] userJars, URL[] systemJars, File toolsJar)
        throws MalformedURLException {
        int numJars = libJars.length + userJars.length + systemJars.length;
        if (toolsJar != null) {
            numJars++;
        }
        URL[] jars = new URL[numJars];
        System.arraycopy(libJars, 0, jars, 0, libJars.length);
        System.arraycopy(userJars, 0, jars, libJars.length, userJars.length);
        System.arraycopy(systemJars, 0, jars, userJars.length + libJars.length,
            systemJars.length);

        if (toolsJar != null) {
            jars[jars.length - 1] = Locator.fileToURL(toolsJar);
        }
        return jars;
    }
}
