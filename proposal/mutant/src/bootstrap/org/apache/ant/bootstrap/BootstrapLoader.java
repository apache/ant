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

package org.apache.ant.bootstrap;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.*;

/**
 * Bootstrap class to build the rest of ant with a minimum of user intervention
 *
 * The bootstrap class is able to act as a class loader to load new classes/jars
 * into the VM in which it is running.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */
public class BootstrapLoader extends ClassLoader {
    static public final String RECURSION_GUARD = "ant.bootstrap.recursionGuard";
    static private final int BUFFER_SIZE = 1024;
    
    private String[] classpathElements;
    
    public BootstrapLoader(String classpath) {
        StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);
        classpathElements = new String[tokenizer.countTokens()];
        
        for (int i = 0; tokenizer.hasMoreTokens(); ++i) {
            classpathElements[i] = tokenizer.nextToken();
        }
    }
    
    protected Class findClass(String name)
                   throws ClassNotFoundException {
        String resourceName = name.replace('.', '/') + ".class";
        InputStream classStream = getResourceStream(resourceName);
        
        if (classStream == null) {
            throw new ClassNotFoundException();
        }
        
        try {            
            return getClassFromStream(classStream, name);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            throw new ClassNotFoundException();
        }
    }

    /**
     * Get a stream to read the requested resource name.
     *
     * @param name the name of the resource for which a stream is required.
     *
     * @return a stream to the required resource or null if the resource cannot be
     * found on the loader's classpath.
     */
    private InputStream getResourceStream(String name) {
        // we need to search the components of the path to see if we can find the 
        // class we want. 
        InputStream stream = null;
 
        for (int i = 0; i < classpathElements.length && stream == null; ++i) {
            File pathComponent = new File(classpathElements[i]);
            stream = getResourceStream(pathComponent, name);
        }

        return stream;
    }
    

                   
    /**
     * Get a stream to read the requested resource name.
     *
     * @param name the name of the resource for which a stream is required.
     *
     * @return a stream to the required resource or null if the resource cannot be
     * found on the loader's classpath.
     */
    public InputStream getResourceAsStream(String name) {
        return getResourceStream(name);
    }
    
    protected Class loadClass(String name,
                              boolean resolve)
                   throws ClassNotFoundException {
        Class requestedClass = findLoadedClass(name);
        try {
            if (requestedClass == null) {
                requestedClass = findClass(name);
                if (resolve) {
                    resolveClass(requestedClass);
                }
            }
            return requestedClass;
        }
        catch (ClassNotFoundException cnfe) {
            return super.loadClass(name, resolve);
        }
    }                    

    /**
     * Get an inputstream to a given resource in the given file which may
     * either be a directory or a zip file.
     *
     * @param file the file (directory or jar) in which to search for the resource.
     * @param resourceName the name of the resource for which a stream is required.
     *
     * @return a stream to the required resource or null if the resource cannot be
     * found in the given file object
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
            }
            else {
                ZipFile zipFile = null;
                try {
                    zipFile = new ZipFile(file);
        
                    ZipEntry entry = zipFile.getEntry(resourceName);
                    if (entry != null) {
                        // we need to read the entry out of the zip file into
                        // a baos and then 
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytesRead;
                        InputStream stream = zipFile.getInputStream(entry);
                        while ((bytesRead = stream.read(buffer, 0, BUFFER_SIZE)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                        return new ByteArrayInputStream(baos.toByteArray());   
                    }
                }
                finally {
                    if (zipFile != null) {
                        zipFile.close();
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;   
    }

    /**
     * Read a class definition from a stream.
     *
     * @param stream the stream from which the class is to be read.
     * @param classname the class name of the class in the stream.
     *
     * @return the Class object read from the stream.
     *
     * @throws IOException if there is a problem reading the class from the
     * stream.
     */
    private Class getClassFromStream(InputStream stream, String classname) 
                throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bytesRead = -1;
        byte[] buffer = new byte[1024];
        
        while ((bytesRead = stream.read(buffer, 0, 1024)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        
        byte[] classData = baos.toByteArray();

        return defineClass(classname, classData, 0, classData.length); 
    }


    static private void buildAnt() {
        System.out.println("Bootstrapping Ant ...");
        
    }

    static private void runWithToolsJar(String[] args) {
        try {
            
            String javaHome = System.getProperty("java.home");
            if (javaHome.endsWith("jre")) {
                javaHome = javaHome.substring(0, javaHome.length() - 4);
            }
            File toolsjar = new File(javaHome + "/lib/tools.jar");
            if (!toolsjar.exists()) {
                System.out.println("Unable to locate tools.jar. expected it to be in " +
                                           toolsjar.getPath());
                return;                                           
            }                                            
            String newclasspath = toolsjar.getPath() + File.pathSeparator + 
                                  System.getProperty("java.class.path");
            
            System.out.println("New Classpath is " + newclasspath);
            
            BootstrapLoader loader = new BootstrapLoader(newclasspath);
            
            Class newBootClass = loader.loadClass("org.apache.ant.bootstrap.BootstrapLoader",
                                                  true);
            final Class[] param = { Class.forName("[Ljava.lang.String;") };
            final Method main = newBootClass.getMethod("main", param);
            final Object[] argument = { args };
            main.invoke(null, argument);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to run boot with tools.jar");
        }
    }
    
    static public void main(String[] args) {
        // check whether the tools.jar is already in the classpath.
        try {
            Class compilerClass = Class.forName("sun.tools.javac.Main");
            System.out.println("Compiler is available");
        } catch (ClassNotFoundException cnfe) {
            if (System.getProperty(RECURSION_GUARD) != null) {
                cnfe.printStackTrace();
                System.out.println("Unable to load compiler");
                return;
            }
            System.setProperty(RECURSION_GUARD, "yes");
            System.out.println("Compiler is not on classpath - locating ...");
            runWithToolsJar(args);
            return;                                                           
        }
        
        buildAnt();
    }
}


