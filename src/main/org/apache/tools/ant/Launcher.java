/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

/**
 * This is the Ant command line front end to end. This front end
 * works out where ant is installed and loads the ant libraries before
 * starting Ant proper.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */ 
public class Launcher {
    static private File determineAntHome11() {
        String classpath = System.getProperty("java.class.path");
        StringTokenizer tokenizer = new StringTokenizer(classpath, System.getProperty("path.separator"));
        while (tokenizer.hasMoreTokens()) {
            String path = tokenizer.nextToken();
            if (path.endsWith("ant.jar")) {
                File antJarFile = new File(path);
                File libDirectory = new File(antJarFile.getParent());
                File antHome = new File(libDirectory.getParent());
                return antHome;
            }
        }
        return null;
    }

    static private File determineAntHome(ClassLoader systemClassLoader) {
        try {
            String className = Launcher.class.getName().replace('.', '/') + ".class";
            URL classResource = systemClassLoader.getResource(className);
            String fileComponent = classResource.getFile();
            if (classResource.getProtocol().equals("file")) {
                // Class comes from a directory of class files rather than
                // from a jar. 
                int classFileIndex = fileComponent.lastIndexOf(className);
                if (classFileIndex != -1) {
                    fileComponent = fileComponent.substring(0, classFileIndex);
                }
                File classFilesDir = new File(fileComponent);
                File buildDir = new File(classFilesDir.getParent());
                File devAntHome = new File(buildDir.getParent());
                return devAntHome;
            }
            else if (classResource.getProtocol().equals("jar")) {
                // Class is coming from a jar. The file component of the URL
                // is actually the URL of the jar file
                int classSeparatorIndex = fileComponent.lastIndexOf("!");
                if (classSeparatorIndex != -1) {
                    fileComponent = fileComponent.substring(0, classSeparatorIndex);
                }
                URL antJarURL = new URL(fileComponent);
                File antJarFile = new File(antJarURL.getFile());
                File libDirectory = new File(antJarFile.getParent());
                File antHome = new File(libDirectory.getParent());
                return antHome;
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static private void addDirJars(AntClassLoader classLoader, File jarDir) {
        String[] fileList = jarDir.list(new FilenameFilter() {
                                            public boolean accept(File dir, String name) {
                                                return name.endsWith(".jar");
                                            }
                                        });

        if (fileList != null) {                                                
            for (int i = 0; i < fileList.length; ++i) {
                File jarFile = new File(jarDir, fileList[i]);                                        
                classLoader.addPathElement(jarFile.getAbsolutePath());
            }
        }
    }
    
    static private void addToolsJar(AntClassLoader antLoader) {
        String javaHome = System.getProperty("java.home");
        if (javaHome.endsWith("jre")) {
            javaHome = javaHome.substring(0, javaHome.length() - 4);
        }
        System.out.println("Java home is " + javaHome);
        File toolsJar = new File(javaHome, "lib/tools.jar");
        if (!toolsJar.exists()) {
            System.out.println("Unable to find tools.jar at " + toolsJar.getPath());
        }
        else {
            antLoader.addPathElement(toolsJar.getAbsolutePath());
        }
    }
    

    static public void main(String[] args) {
        File antHome = null;
        ClassLoader systemClassLoader = Launcher.class.getClassLoader();
        if (systemClassLoader == null) {
            antHome = determineAntHome11();
        }
        else {
            antHome = determineAntHome(systemClassLoader);
        }
        if (antHome == null) {
            System.err.println("Unable to determine ANT_HOME");
            System.exit(1);
        }
    
        System.out.println("ANT_HOME is " + antHome);

        // We now create the class loader with which we are going to launch ant
        AntClassLoader antLoader = new AntClassLoader(systemClassLoader, false);

        // need to find tools.jar
        addToolsJar(antLoader);        
        
        // add everything in the lib directory to this classloader
        File libDir = new File(antHome, "lib");
        addDirJars(antLoader, libDir);
        
        File optionalDir = new File(antHome, "lib/optional");
        addDirJars(antLoader, optionalDir);

        Properties launchProperties = new Properties();
        launchProperties.put("ant.home", antHome.getAbsolutePath());        
        
        try {
            Class mainClass = antLoader.loadClass("org.apache.tools.ant.Main");
            antLoader.initializeClass(mainClass);
            
            final Class[] param = {Class.forName("[Ljava.lang.String;"),
                                   Properties.class, ClassLoader.class};
            final Method startMethod = mainClass.getMethod("start", param);
            final Object[] argument = {args, launchProperties, systemClassLoader};
            startMethod.invoke(null, argument);
        }
        catch (Exception e) {
            System.out.println("Exception running Ant: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}

