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
package org.apache.tools.ant.launch;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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
     * <p>It's value is the concatenation of {@link #ANT_PRIVATEDIR}
     * with {@link #ANT_PRIVATELIB}, with an appropriate file separator
     * in between. For example, on Unix, it's <code>.ant/lib</code>.</p>
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
    public static void main(final String[] args) {
        int exitCode;
        boolean launchDiag = false;
        try {
            final Launcher launcher = new Launcher();
            exitCode = launcher.run(args);
            launchDiag = launcher.launchDiag;
        } catch (final LaunchException e) {
            exitCode = EXIT_CODE_ERROR;
            System.err.println(e.getMessage());
        } catch (final Throwable t) {
            exitCode = EXIT_CODE_ERROR;
            t.printStackTrace(System.err); //NOSONAR
        }
        if (exitCode != 0) {
            if (launchDiag) {
                System.out.println("Exit code: " + exitCode);
            }
            System.exit(exitCode);
        }
    }

    /**
     * launch diagnostics flag; for debugging trouble at launch time.
     */
    public boolean launchDiag = false;

    private Launcher() {
    }

    /**
     * Add a CLASSPATH or -lib to lib path urls.
     * Only filesystem resources are supported.
     *
     * @param path        the classpath or lib path to add to the libPathULRLs
     * @param getJars     if true and a path is a directory, add the jars in
     *                    the directory to the path urls
     * @param libPathURLs the list of paths to add to
     * @throws MalformedURLException if we can't create a URL
     */
    private void addPath(final String path, final boolean getJars, final List<URL> libPathURLs)
            throws MalformedURLException {
        final StringTokenizer tokenizer = new StringTokenizer(path, File.pathSeparator);
        while (tokenizer.hasMoreElements()) {
            final String elementName = tokenizer.nextToken();
            final File element = new File(elementName);
            if (elementName.contains("%") && !element.exists()) {
                continue;
            }
            if (getJars && element.isDirectory()) {
                // add any jars in the directory
                for (URL dirURL : Locator.getLocationURLs(element)) {
                    if (launchDiag) {
                        System.out.println("adding library JAR: " + dirURL);
                    }
                    libPathURLs.add(dirURL);
                }
            }

            final URL url = new URL(element.toURI().toASCIIString());
            if (launchDiag) {
                System.out.println("adding library URL: " + url);
            }
            libPathURLs.add(url);
        }
    }

    /**
     * Run the launcher to launch Ant.
     *
     * @param args the command line arguments
     * @return an exit code. As the normal ant main calls exit when it ends,
     *         this is for handling failures at bind-time
     * @throws MalformedURLException if the URLs required for the classloader
     *            cannot be created.
     * @throws LaunchException for launching problems
     */
    private int run(final String[] args)
            throws LaunchException, MalformedURLException {
        final String antHomeProperty = System.getProperty(ANTHOME_PROPERTY);
        File antHome = null;

        final File sourceJar = Locator.getClassSource(getClass());
        final File jarDir = sourceJar.getParentFile();
        String mainClassname = MAIN_CLASS;

        if (antHomeProperty != null) {
            antHome = new File(antHomeProperty);
        }

        if (antHome == null || !antHome.exists()) {
            antHome = jarDir.getParentFile();
            setProperty(ANTHOME_PROPERTY, antHome.getAbsolutePath());
        }

        if (!antHome.exists()) {
            throw new LaunchException(
                "Ant home is set incorrectly or ant could not be located (estimated value="
                    + antHome.getAbsolutePath() + ")");
        }

        final List<String> libPaths = new ArrayList<>();
        String cpString = null;
        final List<String> argList = new ArrayList<>();
        String[] newArgs;
        boolean  noUserLib = false;
        boolean  noClassPath = false;

        for (int i = 0; i < args.length; ++i) {
            if ("-lib".equals(args[i])) {
                if (i == args.length - 1) {
                    throw new LaunchException(
                        "The -lib argument must be followed by a library location");
                }
                libPaths.add(args[++i]);
            } else if ("-cp".equals(args[i])) {
                if (i == args.length - 1) {
                    throw new LaunchException(
                        "The -cp argument must be followed by a classpath expression");
                }
                if (cpString != null) {
                    throw new LaunchException(
                        "The -cp argument must not be repeated");
                }
                cpString = args[++i];
            } else if ("--nouserlib".equals(args[i]) || "-nouserlib".equals(args[i])) {
                noUserLib = true;
            } else if ("--launchdiag".equals(args[i])) {
                launchDiag = true;
            } else if ("--noclasspath".equals(args[i]) || "-noclasspath".equals(args[i])) {
                noClassPath = true;
            } else if ("-main".equals(args[i])) {
                if (i == args.length - 1) {
                    throw new LaunchException(
                        "The -main argument must be followed by a library location");
                }
                mainClassname = args[++i];
            } else {
                argList.add(args[i]);
            }
        }

        logPath("Launcher JAR", sourceJar);
        logPath("Launcher JAR directory", sourceJar.getParentFile());
        logPath("java.home", new File(System.getProperty("java.home")));

        //decide whether to copy the existing arg set, or
        //build a new one from the list of all args excluding the special
        //operations that only we handle
        if (argList.size() == args.length) {
            newArgs = args;
        } else {
            newArgs = argList.toArray(new String[0]);
        }

        final URL[] libURLs    = getLibPathURLs(
            noClassPath ? null : cpString, libPaths);
        final URL[] systemURLs = getSystemURLs(jarDir);
        final URL[] userURLs   = noUserLib ? new URL[0] : getUserURLs();

        final File toolsJAR = Locator.getToolsJar();
        logPath("tools.jar", toolsJAR);
        final URL[] jars = getJarArray(
            libURLs, userURLs, systemURLs, toolsJAR);

        // now update the class.path property
        final StringBuilder baseClassPath
            = new StringBuilder(System.getProperty(JAVA_CLASS_PATH));
        if (baseClassPath.charAt(baseClassPath.length() - 1)
                == File.pathSeparatorChar) {
            baseClassPath.setLength(baseClassPath.length() - 1);
        }

        for (URL jar : jars) {
            baseClassPath.append(File.pathSeparatorChar);
            baseClassPath.append(Locator.fromURI(jar.toString()));
        }

        setProperty(JAVA_CLASS_PATH, baseClassPath.toString());

        final URLClassLoader loader = new URLClassLoader(jars, Launcher.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
        Class<? extends AntMain> mainClass = null;
        int exitCode = 0;
        Throwable thrown = null;
        try {
            mainClass = loader.loadClass(mainClassname).asSubclass(AntMain.class);
            final AntMain main = mainClass.getDeclaredConstructor().newInstance();
            main.startAnt(newArgs, null, null);
        } catch (final InstantiationException ex) {
            System.err.println(
                "Incompatible version of " + mainClassname + " detected");
            final File mainJar = Locator.getClassSource(mainClass);
            System.err.println(
                "Location of this class " + mainJar);
            thrown = ex;
        } catch (final ClassNotFoundException cnfe) {
            System.err.println(
                    "Failed to locate" + mainClassname);
            thrown = cnfe;
        } catch (final Throwable t) {
            t.printStackTrace(System.err); //NOSONAR
            thrown = t;
        }
        if (thrown != null) {
            System.err.println(ANTHOME_PROPERTY + ": " + antHome.getAbsolutePath());
            System.err.println("Classpath: " + baseClassPath.toString());
            System.err.println("Launcher JAR: " + sourceJar.getAbsolutePath());
            System.err.println("Launcher Directory: " + jarDir.getAbsolutePath());
            exitCode = EXIT_CODE_ERROR;
        }
        return exitCode;
    }

    /**
     * Get the list of -lib entries and -cp entry into
     * a URL array.
     * @param cpString the classpath string
     * @param libPaths the list of -lib entries.
     * @return an array of URLs.
     * @throws MalformedURLException if the URLs  cannot be created.
     */
    private URL[] getLibPathURLs(final String cpString, final List<String> libPaths)
        throws MalformedURLException {
        final List<URL> libPathURLs = new ArrayList<>();

        if (cpString != null) {
            addPath(cpString, false, libPathURLs);
        }

        for (final String libPath : libPaths) {
            addPath(libPath, true, libPathURLs);
        }

        return libPathURLs.toArray(new URL[0]);
    }

    /**
     * Get the jar files in ANT_HOME/lib.
     * determine ant library directory for system jars: use property
     * or default using location of ant-launcher.jar
     * @param antLauncherDir the dir that ant-launcher ran from
     * @return the URLs
     * @throws MalformedURLException if the URLs cannot be created.
     */
    private URL[] getSystemURLs(final File antLauncherDir) throws MalformedURLException {
        File antLibDir = null;
        final String antLibDirProperty = System.getProperty(ANTLIBDIR_PROPERTY);
        if (antLibDirProperty != null) {
            antLibDir = new File(antLibDirProperty);
        }
        if (antLibDir == null || !antLibDir.exists()) {
            antLibDir = antLauncherDir;
            setProperty(ANTLIBDIR_PROPERTY, antLibDir.getAbsolutePath());
        }
        return Locator.getLocationURLs(antLibDir);
    }

    /**
     * Get the jar files in user.home/.ant/lib
     * @return the URLS from the user's lib dir
     * @throws MalformedURLException if the URLs cannot be created.
     */
    private URL[] getUserURLs() throws MalformedURLException {
        final File userLibDir
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
    private URL[] getJarArray(final URL[] libJars, final URL[] userJars,
        final URL[] systemJars, final File toolsJar)
        throws MalformedURLException {
        int numJars = libJars.length + userJars.length + systemJars.length;
        if (toolsJar != null) {
            numJars++;
        }
        final URL[] jars = new URL[numJars];
        System.arraycopy(libJars, 0, jars, 0, libJars.length);
        System.arraycopy(userJars, 0, jars, libJars.length, userJars.length);
        System.arraycopy(systemJars, 0, jars, userJars.length + libJars.length,
            systemJars.length);

        if (toolsJar != null) {
            jars[jars.length - 1] = new URL(toolsJar.toURI().toASCIIString());
        }
        return jars;
    }

    /**
     * set a system property, optionally log what is going on
     * @param name property name
     * @param value value
     */
    private void setProperty(final String name, final String value) {
        if (launchDiag) {
            System.out.println("Setting \"" + name + "\" to \"" + value + "\"");
        }
        System.setProperty(name, value);
    }

    private void logPath(final String name, final File path) {
        if (launchDiag) {
            System.out.println(name + "= \"" + path + "\"");
        }
    }
}
