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
package org.apache.tools.ant.util;

import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import org.apache.tools.ant.BuildException;

/**
 * ClassLoader utility methods
 *
 * @author Conor MacNeill
 */
public class LoaderUtils {
    /** The getContextClassLoader method */
    private static Method getContextClassLoader;
    /** The setContextClassLoader method */
    private static Method setContextClassLoader;

    // Set up the reflection-based Java2 methods if possible
    static {
        try {
            getContextClassLoader
                 = Thread.class.getMethod("getContextClassLoader",
                new Class[0]);
            Class[] setContextArgs = new Class[]{ClassLoader.class};
            setContextClassLoader
                 = Thread.class.getMethod("setContextClassLoader",
                setContextArgs);
        } catch (Exception e) {
            // ignore any problems accessing the methods - probably JDK 1.1
        }
    }

    /**
     * JDK1.1 compatible access to get the context class loader. Has no
     * effect on JDK 1.1
     *
     * @param loader the ClassLoader to be used as the context class loader
     *      on the current thread.
     */
    public static void setContextClassLoader(ClassLoader loader) {
        if (setContextClassLoader == null) {
            return;
        }

        try {
            Thread currentThread = Thread.currentThread();
            setContextClassLoader.invoke(currentThread,
                new Object[]{loader});
        } catch (IllegalAccessException e) {
            throw new BuildException
                ("Unexpected IllegalAccessException", e);
        } catch (InvocationTargetException e) {
            throw new BuildException
                ("Unexpected InvocationTargetException", e);
        }

    }


    /**
     * JDK1.1 compatible access to set the context class loader.
     *
     * @return the ClassLoader instance being used as the context
     *      classloader on the current thread. Returns null on JDK 1.1
     */
    public static ClassLoader getContextClassLoader() {
        if (getContextClassLoader == null) {
            return null;
        }

        try {
            Thread currentThread = Thread.currentThread();
            return (ClassLoader) getContextClassLoader.invoke(currentThread,
                new Object[0]);
        } catch (IllegalAccessException e) {
            throw new BuildException
                ("Unexpected IllegalAccessException", e);
        } catch (InvocationTargetException e) {
            throw new BuildException
                ("Unexpected InvocationTargetException", e);
        }
    }

    /**
     * Indicates if the context class loader methods are available
     *
     * @return true if the get and set methods dealing with the context
     *      classloader are available.
     */
    public static boolean isContextLoaderAvailable() {
        return getContextClassLoader != null &&
            setContextClassLoader != null;
    }
}

