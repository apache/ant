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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.ant.common.antlib.Converter;
import org.apache.ant.common.util.AntException;

/**
 * AttributeSetters are created at introspection time for each
 * setter method a class provides and for which a conversion from a
 * String value is available.
 *
 * @author Conor MacNeill
 * @created 19 January 2002
 */
public class AttributeSetter {
    /** The method that will perform the setting */
    private Method method;

    /**
     * A converter to convert the string value to a value to be given to
     * the setter method
     */
    private Converter converter;

    /**
     * A constructor used to create the string value to an object to be used
     * by the setter
     */
    private Constructor valueConstructor;

    /** The depth of the setter in the class hierarchy */
    private int depth;

    /**
     * Create a setter which just uses string values
     *
     * @param method the method to be invoked.
     * @param depth the depth of this method declaraion in the class hierarchy.
     */
    public AttributeSetter(Method method, int depth) {
        this.method = method;
        this.depth = depth;
    }

    /**
     * Create a setter which just uses string values
     *
     * @param method the method to be invoked.
     * @param depth the depth of this method declaraion in the class hierarchy.
     * @param converter a converter to convert string values into instances of
     *        the type expected by the method.
     */
    public AttributeSetter(Method method, int depth, Converter converter) {
        this(method, depth);
        this.converter = converter;
    }

    /**
     * Create a setter which just uses string values
     *
     * @param method the method to be invoked.
     * @param depth the depth of this method declaraion in the class hierarchy.
     * @param valueConstructor an object constructor used to convert string
     *        values into instances of the type expected by the method.
     */
    public AttributeSetter(Method method, int depth,
                           Constructor valueConstructor) {
        this(method, depth);
        this.valueConstructor = valueConstructor;
    }

    /**
     * Set the attribute value on an object
     *
     * @param obj the object on which the set method is to be invoked
     * @param stringValue the string representation of the value
     * @exception InvocationTargetException if the method cannot be
     *      invoked
     * @exception IllegalAccessException if the method cannot be invoked
     * @exception AntException if the conversion of the value
     *      fails
     */
    void set(Object obj, String stringValue)
         throws InvocationTargetException, IllegalAccessException,
        AntException {

        Object value = null;
        if (converter != null) {
            Class type = getType();
            value = converter.convert(stringValue, type);
        } else if (valueConstructor != null) {
            try {
                value = valueConstructor.newInstance(new String[]{stringValue});
            } catch (InstantiationException e) {
                throw new ExecutionException(e);
            }
        } else {
            value = stringValue;
        }

        method.invoke(obj, new Object[]{value});
    }

    /**
     * Get the declaration depth of this setter.
     *
     * @return the attribute setter's declaration depth.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Get the type expected by this setter's method
     *
     * @return a Class instance being the type this setter's method accepts.
     */
    public Class getType() {
        return method.getParameterTypes()[0];
    }
}

