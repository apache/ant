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
package org.apache.tools.ant.launch;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.io.File;
import java.util.StringTokenizer;

/**
 *  This is a launcher for Ant.
 *
 * @author  Conor MacNeill
 * @since Ant 1.6
 */
public class Launcher {
    /** The Ant Home property */
    public static final String ANTHOME_PROPERTY = "ant.home";

    /** The location of a per-user library directory */
    public static final String USER_LIBDIR = ".ant/lib";

    /** The startup class that is to be run */
    public static final String MAIN_CLASS = "org.apache.tools.ant.Main";

    /**
     *  Entry point for starting command line Ant
     *
     * @param  args commandline arguments
     */
    public static void main(String[] args) {
        try {
            Launcher launcher = new Launcher();
            launcher.run(args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    /**
     * Run the launcher to launch Ant
     *
     * @param args the command line arguments
     *
     * @exception MalformedURLException if the URLs required for the classloader
     *            cannot be created.
     */
    private void run(String[] args) throws MalformedURLException {
        String antHomeProperty = System.getProperty(ANTHOME_PROPERTY);
        File antHome = null;

        File sourceJar = Locator.getClassSource(getClass());
        File jarDir = sourceJar.getParentFile();

        if (antHomeProperty != null) {
            antHome = new File(antHomeProperty);
        }

        if (antHome == null || !antHome.exists()) {
            antHome = jarDir.getParentFile();
            System.setProperty(ANTHOME_PROPERTY, antHome.getAbsolutePath());
        }

        if (!antHome.exists()) {
            throw new IllegalStateException("Ant home is set incorrectly or "
                + "ant could not be located");
        }
        String libPath = "";
        String[] newargs = null;
        int argcount = -1;
        for (argcount = 0; argcount < args.length -1; argcount++) {
            if (args[argcount].equals("-lib")) {
                libPath = args[argcount + 1];
                break;
            }
        }
        if (args.length > 0 && args[args.length -1].equals("-lib")) {
            // if the last argument is -lib
            // remove it from the arguments passed to Launcher
            //
            newargs = new String[args.length - 1];
            System.arraycopy(args, 0, newargs, 0, args.length -1);
        } else if (libPath.equals("")) {
           newargs = new String[args.length];
           System.arraycopy(args, 0, newargs, 0, args.length);
        }  else {
            newargs = new String[args.length - 2];
            // copy the beginning of the args array
            if (argcount > 0 ) {
                System.arraycopy(args, 0, newargs, 0 ,argcount);
            }
            // copy the end of the args array
            if ((argcount + 2 < args.length) && argcount > 0) {
                System.arraycopy(args, argcount + 2, newargs, argcount, args.length - (argcount + 2));
            }
        }
        StringTokenizer myTokenizer = new StringTokenizer(libPath, System.getProperty("path.separator"));
        URL[] classPathJars = new URL[myTokenizer.countTokens()];
        int classPathJarCount = 0;
        while (myTokenizer.hasMoreElements()) {
            String token = myTokenizer.nextToken();
            classPathJars[classPathJarCount++] = new File(token).toURL();
        }


        // Now try and find JAVA_HOME
        File toolsJar = Locator.getToolsJar();

        URL[] systemJars = Locator.getLocationURLs(jarDir);

        File userLibDir
            = new File(System.getProperty("user.home"), USER_LIBDIR);
        URL[] userJars = Locator.getLocationURLs(userLibDir);


        int numJars = classPathJars.length + userJars.length + systemJars.length;
        if (toolsJar != null) {
            numJars++;
        }
        URL[] jars = new URL[numJars];
        System.arraycopy(classPathJars, 0, jars, 0, classPathJars.length);
        System.arraycopy(userJars, 0, jars, classPathJars.length, userJars.length);
        System.arraycopy(systemJars, 0, jars, userJars.length + classPathJars.length,
            systemJars.length);

        if (toolsJar != null) {
            jars[jars.length - 1] = toolsJar.toURL();
        }


        // now update the class.path property
        StringBuffer baseClassPath
            = new StringBuffer(System.getProperty("java.class.path"));

        for (int i = 0; i < jars.length; ++i) {
            baseClassPath.append(File.pathSeparatorChar);
            baseClassPath.append(Locator.fromURI(jars[i].toString()));
        }

        System.setProperty("java.class.path", baseClassPath.toString());

        URLClassLoader loader = new URLClassLoader(jars);
        Thread.currentThread().setContextClassLoader(loader);
        try {
            Class mainClass = loader.loadClass(MAIN_CLASS);
            AntMain main = (AntMain) mainClass.newInstance();
            main.startAnt(newargs, null, null);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

