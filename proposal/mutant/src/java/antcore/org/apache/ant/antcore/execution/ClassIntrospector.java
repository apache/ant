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
package org.apache.ant.antcore.execution;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

/**
 * Introspects a class and builds a reflector for setting values on
 * instances of the class
 *
 * @author Conor MacNeill
 * @created 19 January 2002
 */
public class ClassIntrospector {
    /** The reflector that this introspector populates */
    private Reflector reflector;

    /**
     * A Map which maps the classnames to their depth in the class hiearchy,
     * with the current class being depth=0
     */
    private Map classDepth = new HashMap();

    /**
     * Determine the class hierarchy depths for the given class.
     *
     * @param bean the class for which the class depths will be determined.
     */
    private void getDepths(Class bean) {
        Class currentClass = bean;
        int index = 0;
        while (currentClass != null) {
            classDepth.put(currentClass, new Integer(index++));
            currentClass = currentClass.getSuperclass();
        }
    }
            
    
    /**
     * Create a introspector for the bean
     *
     * @param bean the class which is introspected
     * @param converters a collection of converters for converting values
     *      from strings
     */
    public ClassIntrospector(final Class bean, Map converters) {
        reflector = new Reflector();
        getDepths(bean);

        Method[] methods = bean.getMethods();
        for (int i = 0; i < methods.length; i++) {
            final Method m = methods[i];
            final String name = m.getName();
            Class returnType = m.getReturnType();
            Class[] args = m.getParameterTypes();

            if (name.equals("addText")
                 && returnType.equals(Void.TYPE)
                 && args.length == 1
                 && args[0].equals(String.class)) {
                reflector.setAddTextMethod(m);
            } else if (name.startsWith("set")
                 && name.length() > 3
                 && returnType.equals(Void.TYPE)
                 && args.length == 1
                 && !args[0].isArray()) {
                Integer depth = (Integer)classDepth.get(m.getDeclaringClass());
                reflector.addAttributeMethod(m, depth.intValue(), 
                    getPropertyName(name, "set"), converters);
            } else if (name.startsWith("addConfigured")
                 && name.length() > 13
                 && returnType.equals(Void.TYPE)
                 && args.length == 1
                 && !args[0].equals(String.class)
                 && !args[0].isArray()
                 && !args[0].isPrimitive()) {
                reflector.addElementMethod(m, 
                    getPropertyName(name, "addConfigured"));
             } else if (name.startsWith("add")
                 && name.length() > 3
                 && returnType.equals(Void.TYPE)
                 && args.length == 1
                 && !args[0].equals(String.class)
                 && !args[0].isArray()
                 && !args[0].isPrimitive()) {
                reflector.addElementMethod(m, getPropertyName(name, "add"));
           } else if (name.startsWith("create")
                 && name.length() > 6
                 && !returnType.isArray()
                 && !returnType.isPrimitive()
                 && args.length == 0) {
                reflector.addCreateMethod(m, getPropertyName(name, "create"));
            }
        }
    }

    /**
     * Gets the reflector associed with the class we are introspecting
     *
     * @return the reflector
     */
    public Reflector getReflector() {
        return reflector;
    }

    /**
     * extract the name of a property from a method name - subtracting a
     * given prefix.
     *
     * @param methodName the name of the method
     * @param prefix the prefix to be ignored
     * @return the property name
     */
    private String getPropertyName(String methodName, String prefix) {
        int start = prefix.length();
        return methodName.substring(start).toLowerCase();
    }
}

