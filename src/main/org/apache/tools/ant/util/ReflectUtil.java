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
package org.apache.tools.ant.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;

/**
 * Utility class to handle reflection on java objects.
 * The class contains static methods to call reflection
 * methods, catch any exceptions, converting them
 * to BuildExceptions.
 */
// CheckStyle:FinalClassCheck OFF - backward compatible
public class ReflectUtil {

    /**  private constructor */
    private ReflectUtil() {
    }

    /**
     * Create an instance of a class using the constructor matching
     * the given arguments.
     * @param <T> desired type
     * @param ofClass Class&lt;T&gt;
     * @param argTypes Class&lt;?&gt;[]
     * @param args Object[]
     * @return class instance
     * @since Ant 1.8.0
     */
    public static <T> T newInstance(Class<T> ofClass,
                                     Class<?>[] argTypes,
                                     Object[] args) {
        try {
            Constructor<T> con = ofClass.getConstructor(argTypes);
            return con.newInstance(args);
        } catch (Exception t) {
            throwBuildException(t);
            return null; // NotReached
        }
    }

    /**
     * Call a method on the object with no parameters.
     * @param <T> desired type
     * @param obj  the object to invoke the method on.
     * @param methodName the name of the method to call
     * @return the object returned by the method
     */
    @SuppressWarnings("unchecked")
    public static <T> T invoke(Object obj, String methodName) {
        try {
            Method method = obj.getClass().getMethod(methodName);
            return (T) method.invoke(obj);
        } catch (Exception t) {
            throwBuildException(t);
            return null; // NotReached
        }
    }

    /**
     * Call a method on the object with no parameters.
     * Note: Unlike the invoke method above, this
     * calls class or static methods, not instance methods.
     * @param <T> desired type
     * @param obj  the object to invoke the method on.
     * @param methodName the name of the method to call
     * @return the object returned by the method
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeStatic(Object obj, String methodName) {
        try {
            Method method = ((Class<?>) obj).getMethod(methodName);
            return (T) method.invoke(obj);
        } catch (Exception t) {
            throwBuildException(t);
            return null; // NotReached
        }
    }

    /**
     * Call a method on the object with one argument.
     * @param <T> desired type
     * @param obj  the object to invoke the method on.
     * @param methodName the name of the method to call
     * @param argType    the type of argument.
     * @param arg        the value of the argument.
     * @return the object returned by the method
     */
    @SuppressWarnings("unchecked")
    public static <T> T invoke(
        Object obj, String methodName, Class<?> argType, Object arg) {
        try {
            Method method = obj.getClass().getMethod(methodName, argType);
            return (T) method.invoke(obj, arg);
        } catch (Exception t) {
            throwBuildException(t);
            return null; // NotReached
        }
    }

    /**
     * Call a method on the object with two argument.
     * @param <T> desired type
     * @param obj  the object to invoke the method on.
     * @param methodName the name of the method to call
     * @param argType1   the type of the first argument.
     * @param arg1       the value of the first argument.
     * @param argType2   the type of the second argument.
     * @param arg2       the value of the second argument.
     * @return the object returned by the method
     */
    @SuppressWarnings("unchecked")
    public static <T> T invoke(
        Object obj, String methodName, Class<?> argType1, Object arg1,
        Class<?> argType2, Object arg2) {
        try {
            Method method =
                obj.getClass().getMethod(methodName, argType1, argType2);
            return (T) method.invoke(obj, arg1, arg2);
        } catch (Exception t) {
            throwBuildException(t);
            return null; // NotReached
        }
    }

    /**
     * Get the value of a field in an object.
     * @param <T> desired type
     * @param obj the object to look at.
     * @param fieldName the name of the field in the object.
     * @return the value of the field.
     * @throws BuildException if there is an error.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getField(Object obj, String fieldName)
        throws BuildException {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (Exception t) {
            throwBuildException(t);
            return null; // NotReached
        }
    }

    /**
     * A method to convert an invocationTargetException to
     * a BuildException and throw it.
     * @param t the invocation target exception.
     * @throws BuildException the converted exception.
     */
    public static void throwBuildException(Exception t)
        throws BuildException {
        throw toBuildException(t);
    }

    /**
     * A method to convert an invocationTargetException to
     * a BuildException.
     * @param t the invocation target exception.
     * @return the converted exception.
     * @since ant 1.7.1
     */
    public static BuildException toBuildException(Exception t) {
        if (t instanceof InvocationTargetException) {
            Throwable t2 = ((InvocationTargetException) t)
                .getTargetException();
            if (t2 instanceof BuildException) {
                return (BuildException) t2;
            }
            return new BuildException(t2);
        }
        return new BuildException(t);
    }

    /**
     * A method to test if an object responds to a given
     * message (method call)
     * @param o the object
     * @param methodName the method to check for
     * @return true if the object has the method.
     * @throws BuildException if there is a problem.
     */
    public static boolean respondsTo(Object o, String methodName)
        throws BuildException {
        try {
            return Stream.of(o.getClass().getMethods()).map(Method::getName)
                .anyMatch(Predicate.isEqual(methodName));
        } catch (Exception t) {
            throw toBuildException(t);
        }
    }
}
