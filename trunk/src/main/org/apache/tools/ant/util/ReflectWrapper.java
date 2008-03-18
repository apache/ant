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

import java.lang.reflect.Constructor;

/**
 * Utility class to handle reflection on java objects.
 * The class is a holder class for an object and
 * uses java reflection to call methods on the objects.
 * If things go wrong, BuildExceptions are thrown.
 */

public class ReflectWrapper {
    private Object obj;
    /**
     * Construct a wrapped object using the no arg constructor.
     * @param loader the classloader to use to construct the class.
     * @param name the classname of the object to construct.
     */
    public ReflectWrapper(ClassLoader loader, String name) {
        try {
            Class clazz;
            clazz = Class.forName(name, true, loader);
            Constructor constructor;
            constructor = clazz.getConstructor((Class[]) null);
            obj = constructor.newInstance((Object[]) null);
        } catch (Exception t) {
            ReflectUtil.throwBuildException(t);
        }
    }

    /**
     * Constructor using a passed in object.
     * @param obj the object to wrap.
     */
    public ReflectWrapper(Object obj) {
        this.obj = obj;
    }

    /**
     * @return the wrapped object.
     */
    public Object getObject() {
        return obj;
    }

    /**
     * Call a method on the object with no parameters.
     * @param methodName the name of the method to call
     * @return the object returned by the method
     */
    public Object invoke(String methodName) {
        return ReflectUtil.invoke(obj, methodName);
    }

    /**
     * Call a method on the object with one argument.
     * @param methodName the name of the method to call
     * @param argType    the type of argument.
     * @param arg        the value of the argument.
     * @return the object returned by the method
     */
    public Object invoke(
        String methodName, Class argType, Object arg) {
        return ReflectUtil.invoke(obj, methodName, argType, arg);
    }

    /**
     * Call a method on the object with one argument.
     * @param methodName the name of the method to call
     * @param argType1   the type of the first argument.
     * @param arg1       the value of the first argument.
     * @param argType2   the type of the second argument.
     * @param arg2       the value of the second argument.
     * @return the object returned by the method
     */
    public Object invoke(
        String methodName, Class argType1, Object arg1,
        Class argType2, Object arg2) {
        return ReflectUtil.invoke(
            obj, methodName, argType1, arg1, argType2, arg2);
    }
}
