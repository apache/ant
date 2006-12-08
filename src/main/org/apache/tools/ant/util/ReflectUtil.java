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
package org.apache.tools.ant.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.tools.ant.BuildException;
import java.lang.reflect.Field;

/**
 * Utility class to handle reflection on java objects.
 * The class contains static methods to call reflection
 * methods, catch any exceptions, converting them
 * to BuildExceptions.
 */

public class ReflectUtil {

    /**  private constructor */
    private ReflectUtil() {
    }

    /**
     * Call a method on the object with no parameters.
     * @param obj  the object to invoke the method on.
     * @param methodName the name of the method to call
     * @return the object returned by the method
     */
    public static Object invoke(Object obj, String methodName) {
        try {
            Method method;
            method = obj.getClass().getMethod(
                methodName, (Class[]) null);
            return method.invoke(obj, (Object[]) null);
        } catch (Exception t) {
            throwBuildException(t);
            return null; // NotReached
        }
    }

    /**
     * Call a method on the object with one argument.
     * @param obj  the object to invoke the method on.
     * @param methodName the name of the method to call
     * @param argType    the type of argument.
     * @param arg        the value of the argument.
     * @return the object returned by the method
     */
    public static Object invoke(
        Object obj, String methodName, Class argType, Object arg) {
        try {
            Method method;
            method = obj.getClass().getMethod(
                methodName, new Class[] {argType});
            return method.invoke(obj, new Object[] {arg});
        } catch (Exception t) {
            throwBuildException(t);
            return null; // NotReached
        }
    }

    /**
     * Call a method on the object with two argument.
     * @param obj  the object to invoke the method on.
     * @param methodName the name of the method to call
     * @param argType1   the type of the first argument.
     * @param arg1       the value of the first argument.
     * @param argType2   the type of the second argument.
     * @param arg2       the value of the second argument.
     * @return the object returned by the method
     */
    public static Object invoke(
        Object obj, String methodName, Class argType1, Object arg1,
        Class argType2, Object arg2) {
        try {
            Method method;
            method = obj.getClass().getMethod(
                methodName, new Class[] {argType1, argType2});
            return method.invoke(obj, new Object[] {arg1, arg2});
        } catch (Exception t) {
            throwBuildException(t);
            return null; // NotReached
        }
    }

    /**
     * Get the value of a field in an object.
     * @param obj the object to look at.
     * @param fieldName the name of the field in the object.
     * @return the value of the field.
     * @throws BuildException if there is an error.
     */
    public static Object getField(Object obj, String fieldName)
        throws BuildException {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception t) {
            throwBuildException(t);
            return null; // NotReached
        }
    }

    /**
     * A method to convert an invocationTargetException to
     * a buildexception and throw it.
     * @param t the invocation target exception.
     * @throws BuildException the converted exception.
     */
    public static void throwBuildException(Exception t)
        throws BuildException {
        if (t instanceof InvocationTargetException) {
            Throwable t2 = ((InvocationTargetException) t)
                .getTargetException();
            if (t2 instanceof BuildException) {
                throw (BuildException) t2;
            }
            throw new BuildException(t2);
        } else {
            throw new BuildException(t);
        }
    }
}
