/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import java.util.*;
import java.util.zip.*;
import java.io.*;

/**
 * Used to load classes within ant with a different claspath from that used to start ant.
 * Note that it is possible to force a class into this loader even when that class is on the
 * system classpath by using the forceLoadClass method. Any subsequent classes loaded by that
 * class will then use this loader rather than the system class loader.
 *
 * @author Conor MacNeill
 */
public class AntClassLoader  extends ClassLoader {
    /**
     * The size of buffers to be used in this classloader.
     */
    static private final int BUFFER_SIZE = 1024;
    
    /**
     * The classpath that is to be used when loading classes using this class loader.
     */ 
    private Path classpath;
    
    /**
     * The project to which this class loader belongs.
     */
    private Project project;

    /**
     * The File components of the path. Typically these will be directories or jar files.
     */
    private Vector components = null;
    
    /**
     * Create a classloader for the given project using the classpath given.
     *
     * @param project the project to ehich this classloader is to belong.
     * @param classpath the classpath to use to load the classes.
     */
    public AntClassLoader(Project project, Path classpath) {
        this.project = project;
        this.classpath = classpath;
    }

    /**
     * Set up this classloader for the first time. 
     *
     * This method will set up the components field with the components from the
     * given classpath. 
     */
    private void setup() {
        // We iterate through the class path, resolving each element. 
        components = new Vector();
        
        String[] pathElements = classpath.list();
        for (int i = 0; i < pathElements.length; ++i) {
            File element = project.resolveFile(pathElements[i]);
            components.addElement(element);
        }
    }

    /**
     * Load a class through this class loader even if that class is available on the
     * system classpath.
     *
     * This ensures that any classes which are loaded by the returned class will use this
     * classloader.
     *
     * @param classname the classname to be loaded.
     * 
     * @return the required Class object
     *
     * @throws ClassNotFoundException if the requested class does not exist on
     * this loader's classpath.
     */
    public Class forceLoadClass(String classname) throws ClassNotFoundException {
        Class theClass = findLoadedClass(classname);

        if (theClass == null) {
            theClass = findClass(classname);
        }
        
        return theClass;
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
        if (components == null) {
            // we haven't set up the list of directories and jars yet.
            setup();
        }

        // we need to search the components of the path to see if we can find the 
        // class we want. 
        InputStream stream = null;

        for (Enumeration e = components.elements(); e.hasMoreElements() && stream == null; ) {
            File pathComponent = (File)e.nextElement();
            stream = getResourceStream(pathComponent, name);
        }

        return stream;
    }

    /**
     * Get an inputstream to a given resource in the given file which may
     * either be a directory or a jar type file.
     *
     * @param file the file (directory or jar) in which to search for the resource.
     * @param resourceName the name of the resource for which a stream is required.
     *
     * @return a stream to the required resource or null if the resource cannot be
     * found in the given file object
     */
    private InputStream getResourceStream(File file, String resourceName) {
        try {
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
     * Load a class with this class loader.
     *
     * This method will load a class. 
     *
     * This class attempts to load the class firstly using the parent class loader. For
     * JDK 1.1 compatability, this uses the findSystemClass method.
     *
     * @param classname the name of the class to be loaded.
     * @param resolve true if all classes upon which this class depends are to be loaded.
     * 
     * @return the required Class object
     *
     * @throws ClassNotFoundException if the requested class does not exist on
     * the system classpath or this loader's classpath.
     */
    protected Class loadClass(String classname, boolean resolve) throws ClassNotFoundException {
        
        Class theClass = findLoadedClass(classname);
        if (theClass == null) {
            try {
                theClass = findSystemClass(classname);
            }
            catch (ClassNotFoundException cnfe) {
                theClass = findClass(classname);
            }
        }
            
        if (resolve) {
            resolveClass(theClass);
        }
        
        return theClass;
    }

    /**
     * Convert the class dot notation to a file system equivalent for
     * searching purposes.
     *
     * @param classname the class name in dot format (ie java.lang.Integer)
     *
     * @return the classname in file system format (ie java/lang/Integer.class)
     */
    private String getClassFilename(String classname) {
        return classname.replace('.', '/') + ".class";
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

    /**
     * Search for and load a class on the classpath of this class loader.
     *
     * @param name the classname to be loaded.
     * 
     * @return the required Class object
     *
     * @throws ClassNotFoundException if the requested class does not exist on
     * this loader's classpath.
     */
    public Class findClass(String name) throws ClassNotFoundException {
        if (components == null) {
            // we haven't set up the list of directories and jars yet.
            setup();
        }

        // we need to search the components of the path to see if we can find the 
        // class we want. 
        InputStream stream = null;
        String classFilename = getClassFilename(name);
        try {
            for (Enumeration e = components.elements(); e.hasMoreElements() && stream == null; ) {
                File pathComponent = (File)e.nextElement();
                stream = getResourceStream(pathComponent, classFilename);
            }
        
            if (stream == null) {
                throw new ClassNotFoundException();
            }
                
            return getClassFromStream(stream, name);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            throw new ClassNotFoundException();
        }
        finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            }
            catch (IOException e) {}
        }
    }
}
