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
package org.apache.tools.ant.taskdefs.optional.rjunit;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import org.apache.tools.ant.types.Path;

/**
 * A set of helpers functions to deal with JUnit.
 *
 */
public final class JUnitHelper {

    private final static String SUITE_METHODNAME = "suite";

    /**
     * This method parse the output of the method <tt>toString()</tt>
     * from the <tt>TestCase</tt> class. The format returned is:
     * <tt>name(classname)</tt>
     * @return the string corresponding to name.
     */
    public static String getTestName(String text){
        int p1 = text.indexOf('(');
        return text.substring(0, p1);
    }

    /**
     * This method parse the output of the method <tt>toString()</tt>
     * from the <tt>TestCase</tt> class. The format returned is:
     * <tt>name(classname)</tt>
     * @return the string corresponding to classname.
     */
    public static String getSuiteName(String text){
        int p1 = text.indexOf('(');
        int p2 = text.indexOf(')', p1);
        return text.substring(p1 + 1, p2);
    }

    /**
     * Returns the Test corresponding to to the given class name
     * @param loader classloader to use when loading the class or
     * <tt>null</tt> for system classloader.
     * @param classname the classname of the test we want to extract.
     * @throws Exception a generic exception
     */
    public static Test getTest(ClassLoader loader, String classname) throws Exception {
        Class clazz = null;
        if (loader == null) {
            clazz = Class.forName(classname);
        } else {
            loader.loadClass(classname);
        }
        return getTest(clazz);
    }


    /**
     * Extract a test from a given class
     * @param clazz the class to extract a test from.
     */
    public static Test getTest(Class clazz) {
        try {
            Object obj = clazz.newInstance();
            if (obj instanceof TestSuite) {
                return (TestSuite) obj;
            }
        } catch (Exception e) {
        }
        try {
            // check if there is a suite method
            Method suiteMethod = clazz.getMethod(SUITE_METHODNAME, new Class[0]);
            return (Test) suiteMethod.invoke(null, new Class[0]);
        } catch (Exception e) {
        }

        // check if it is really a valid testcase
        int modifiers = clazz.getModifiers();
        if ( !Modifier.isPublic(modifiers) ||
                Modifier.isAbstract(modifiers) ||
                !TestCase.class.isAssignableFrom(clazz)) {
            return null;
        }
        // try to extract a test suite automatically
        // this will generate warnings if the class is no suitable Test
        try {
            return new TestSuite(clazz);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Search for the given resource and return the directory or archive
     * that contains it.
     *
     * <p>Doesn't work for archives in JDK 1.1 as the URL returned by
     * getResource doesn't contain the name of the archive.</p>
     *
     * @param resource the resource to look for in the JVM classpath.
     * @return the file or directory containing the resource or
     * <tt>null</tt> if it does not know how to handle it.
     */
    public static File getResourceEntry(String resource) {
        URL url = JUnitHelper.class.getResource(resource);
        if (url == null) {
            // can't find the resource...
            return null;
        }
        String u = url.toString();
        if (u.startsWith("jar:file:")) {
            int pling = u.indexOf("!");
            String jarName = u.substring(9, pling);
            return new File((new File(jarName)).getAbsolutePath());
        } else if (u.startsWith("file:")) {
            int tail = u.indexOf(resource);
            String dirName = u.substring(5, tail);
            return new File((new File(dirName)).getAbsolutePath());
        }
        // don't know how to handle it...
        return null;
    }

    /**
     * Add the entry corresponding to a specific resource to the
     * specified path instance. The entry can either be a directory
     * or an archive.
     * @param path the path to add the resource entry to.
     * @param resource the resource to look for.
     * @see #getResourceEntry(String)
     */
    public static void addClasspathEntry(Path path, String resource) {
        File f = getResourceEntry(resource);
        if (f != null) {
            path.createPathElement().setLocation(f);
        }
    }

}
