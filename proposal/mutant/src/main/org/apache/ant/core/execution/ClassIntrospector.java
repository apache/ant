/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

package org.apache.ant.core.execution;

import java.lang.reflect.*;
import java.io.File;
import java.util.*;

/**
 * Introspects a class and builds a set of objects to assist in intospecting the 
 * class.
 *
 * @author Stefan Bodewig <a href="mailto:stefan.bodewig@megabit.net">stefan.bodewig@megabit.net</a> 
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */
public class ClassIntrospector {
    /**
     * holds the types of the attributes that could be set.
     */
    private Hashtable attributeTypes;

    /**
     * holds the attribute setter methods.
     */
    private Hashtable attributeSetters;

    /**
     * Holds the types of nested elements that could be created.
     */
    private Hashtable nestedTypes;

    /**
     * Holds methods to create nested elements.
     */
    private Hashtable nestedCreators;

    /**
     * The method to add PCDATA stuff.
     */
    private Method addText = null;

    /**
     * The Class that's been introspected.
     */
    private Class bean;

    /**
     * returns the boolean equivalent of a string, which is considered true
     * if either "on", "true", or "yes" is found, ignoring case.
     */
    public static boolean toBoolean(String s) {
        return (s.equalsIgnoreCase("on") ||
                s.equalsIgnoreCase("true") ||
                s.equalsIgnoreCase("yes"));
    }

    public ClassIntrospector(final Class bean, Map converters) {
        attributeTypes = new Hashtable();
        attributeSetters = new Hashtable();
        nestedTypes = new Hashtable();
        nestedCreators = new Hashtable();
        this.bean = bean;

        Method[] methods = bean.getMethods();
        for (int i=0; i<methods.length; i++) {
            final Method m = methods[i];
            final String name = m.getName();
            Class returnType = m.getReturnType();
            Class[] args = m.getParameterTypes();

            if ("addText".equals(name)
                && java.lang.Void.TYPE.equals(returnType)
                && args.length == 1
                && java.lang.String.class.equals(args[0])) {

                addText = methods[i];

            } else if (name.startsWith("set")
                       && java.lang.Void.TYPE.equals(returnType)
                       && args.length == 1
                       && !args[0].isArray()) {

                String propName = getPropertyName(name, "set");
                AttributeSetter as = createAttributeSetter(m, args[0], converters);
                if (as != null) {
                    attributeTypes.put(propName, args[0]);
                    attributeSetters.put(propName, as);
                }

            } else if (name.startsWith("create")
                       && !returnType.isArray()
                       && !returnType.isPrimitive()
                       && args.length == 0) {

                String propName = getPropertyName(name, "create");
                nestedTypes.put(propName, returnType);
                nestedCreators.put(propName, new NestedCreator() {

                        public Object create(Object parent) 
                            throws InvocationTargetException, 
                            IllegalAccessException {

                            return m.invoke(parent, new Object[] {});
                        }

                    });
                
            } else if (name.startsWith("add")
                       && java.lang.Void.TYPE.equals(returnType)
                       && args.length == 1
                       && !java.lang.String.class.equals(args[0])
                       && !args[0].isArray()
                       && !args[0].isPrimitive()) {
                 
                try {
                    final Constructor c = 
                        args[0].getConstructor(new Class[] {});
                    String propName = getPropertyName(name, "add");
                    nestedTypes.put(propName, args[0]);
                    nestedCreators.put(propName, new NestedCreator() {

                            public Object create(Object parent) 
                                throws InvocationTargetException, IllegalAccessException, InstantiationException {
                                
                                Object o = c.newInstance(new Object[] {});
                                m.invoke(parent, new Object[] {o});
                                return o;
                            }

                        });
                } catch (NoSuchMethodException nse) {
                }
                    
            }
        }
    }
    
    /**
     * Sets the named attribute.
     */
    public void setAttribute(Object element, String attributeName, 
                             String value)
        throws ClassIntrospectionException, ConversionException {
        AttributeSetter as = (AttributeSetter) attributeSetters.get(attributeName);
        if (as == null) {
            String msg = "Class " + element.getClass().getName() +
                " doesn't support the \"" + attributeName + "\" attribute";
            throw new ClassIntrospectionException(msg);
        }
        try {
            as.set(element, value);
        } catch (IllegalAccessException ie) {
            // impossible as getMethods should only return public methods
            throw new ClassIntrospectionException(ie);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof ClassIntrospectionException) {
                throw (ClassIntrospectionException) t;
            }
            throw new ClassIntrospectionException(t);
        }
    }

    /**
     * Adds PCDATA areas.
     */
    public void addText(Object element, String text) 
        throws ClassIntrospectionException {
            
        if (addText == null) {
            String msg = "Class " + element.getClass().getName() +
                " doesn't support nested text elements";
            throw new ClassIntrospectionException(msg);
        }
        try {
            addText.invoke(element, new String[] {text});
        } catch (IllegalAccessException ie) {
            // impossible as getMethods should only return public methods
            throw new ClassIntrospectionException(ie);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof ClassIntrospectionException) {
                throw (ClassIntrospectionException) t;
            }
            throw new ClassIntrospectionException(t);
        }
    }

    public boolean supportsNestedElement(String elementName) {
        return nestedCreators.containsKey(elementName);
    }

    /**
     * Creates a named nested element.
     */
    public Object createElement(Object element, String elementName) 
         throws ClassIntrospectionException {
        NestedCreator nc = (NestedCreator) nestedCreators.get(elementName);
        if (nc == null) {
            String msg = "Class " + element.getClass().getName() +
                " doesn't support the nested \"" + elementName + "\" element";
            throw new ClassIntrospectionException(msg);
        }
        try {
            return nc.create(element);
        } catch (IllegalAccessException ie) {
            // impossible as getMethods should only return public methods
            throw new ClassIntrospectionException(ie);
        } catch (InstantiationException ine) {
            // impossible as getMethods should only return public methods
            throw new ClassIntrospectionException(ine);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof ClassIntrospectionException) {
                throw (ClassIntrospectionException) t;
            }
            throw new ClassIntrospectionException(t);
        }
    }

    /**
     * returns the type of a named nested element.
     */
    public Class getElementType(String elementName) 
        throws ClassIntrospectionException  {
        Class nt = (Class) nestedTypes.get(elementName);
        if (nt == null) {
            String msg = "Class " + bean.getName() +
                " doesn't support the nested \"" + elementName + "\" element";
            throw new ClassIntrospectionException(msg);
        }
        return nt;
    }

    /**
     * returns the type of a named attribute.
     */
    public Class getAttributeType(String attributeName) 
        throws ClassIntrospectionException {
        Class at = (Class) attributeTypes.get(attributeName);
        if (at == null) {
            String msg = "Class " + bean.getName() +
                " doesn't support the \"" + attributeName + "\" attribute";
            throw new ClassIntrospectionException(msg);
        }
        return at;
    }

    /**
     * Does the introspected class support PCDATA?
     */
    public boolean supportsCharacters() {
        return addText != null;
    }

    /**
     * Return all attribues supported by the introspected class.
     */
    public Enumeration getAttributes() {
        return attributeSetters.keys();
    }

    /**
     * Return all nested elements supported by the introspected class.
     */
    public Enumeration getNestedElements() {
        return nestedTypes.keys();
    }

    /**
     * Create a proper implementation of AttributeSetter for the given
     * attribute type.  
     */
    private AttributeSetter createAttributeSetter(final Method m,
                                                  final Class arg,
                                                  Map converters) {

        if (converters != null && converters.containsKey(arg)) {
            // we have a converter to use to convert the strign 
            // value of into something the set method expects.
            final Converter converter = (Converter)converters.get(arg);
            return new AttributeSetter() {
                    public void set(Object parent, String value) 
                        throws InvocationTargetException, IllegalAccessException, 
                               ClassIntrospectionException, ConversionException {
                        m.invoke(parent, new Object[] {converter.convert(value, arg)});
                    }
                };
        }
        // simplest case - setAttribute expects String
        else if (java.lang.String.class.equals(arg)) {
            return new AttributeSetter() {
                    public void set(Object parent, String value) 
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent, new String[] {value});
                    }
                };

        // now for the primitive types, use their wrappers
        } else if (java.lang.Character.class.equals(arg)
                   || java.lang.Character.TYPE.equals(arg)) {
            return new AttributeSetter() {
                    public void set(Object parent, String value) 
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent, new Character[] {new Character(value.charAt(0))});
                    }

                };
        } else if (java.lang.Byte.TYPE.equals(arg)) {
            return new AttributeSetter() {
                    public void set(Object parent, String value) 
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent, new Byte[] {new Byte(value)});
                    }

                };
        } else if (java.lang.Short.TYPE.equals(arg)) {
            return new AttributeSetter() {
                    public void set(Object parent, String value) 
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent, new Short[] {new Short(value)});
                    }

                };
        } else if (java.lang.Integer.TYPE.equals(arg)) {
            return new AttributeSetter() {
                    public void set(Object parent, String value) 
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent, new Integer[] {new Integer(value)});
                    }

                };
        } else if (java.lang.Long.TYPE.equals(arg)) {
            return new AttributeSetter() {
                    public void set(Object parent, String value) 
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent, new Long[] {new Long(value)});
                    }

                };
        } else if (java.lang.Float.TYPE.equals(arg)) {
            return new AttributeSetter() {
                    public void set(Object parent, String value) 
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent, new Float[] {new Float(value)});
                    }

                };
        } else if (java.lang.Double.TYPE.equals(arg)) {
            return new AttributeSetter() {
                    public void set(Object parent, String value) 
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent, new Double[] {new Double(value)});
                    }

                };

        } else if (java.lang.Boolean.class.equals(arg) 
                   || java.lang.Boolean.TYPE.equals(arg)) {
            return new AttributeSetter() {
                    public void set(Object parent, String value) 
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent, 
                                 new Boolean[] {new Boolean(toBoolean(value))});
                    }

                };

        // Class doesn't have a String constructor but a decent factory method
        } else if (java.lang.Class.class.equals(arg)) {
            return new AttributeSetter() {
                    public void set(Object parent, String value) 
                        throws InvocationTargetException, IllegalAccessException, ClassIntrospectionException {
                        try {
                            m.invoke(parent, new Class[] {Class.forName(value)});
                        } catch (ClassNotFoundException ce) {
                            throw new ClassIntrospectionException(ce);
                        }
                    }
                };
        // worst case. look for a public String constructor and use it
        } else {

            try {
                final Constructor c = 
                    arg.getConstructor(new Class[] {java.lang.String.class});

                return new AttributeSetter() {
                        public void set(Object parent, 
                                        String value) 
                            throws InvocationTargetException, IllegalAccessException, ClassIntrospectionException {
                            try {
                                m.invoke(parent, new Object[] {c.newInstance(new String[] {value})});
                            } catch (InstantiationException ie) {
                                throw new ClassIntrospectionException(ie);
                            }
                        }
                    };
                
            } catch (NoSuchMethodException nme) {
            }
        }
        
        return null;
    }

    /**
     * extract the name of a property from a method name - subtracting
     * a given prefix.  
     */
    private String getPropertyName(String methodName, String prefix) {
        int start = prefix.length();
        return methodName.substring(start).toLowerCase();
    }

    private interface NestedCreator {
        public Object create(Object parent) 
            throws InvocationTargetException, IllegalAccessException, InstantiationException;
    }
    private interface AttributeSetter {
        public void set(Object parent, String value)
            throws InvocationTargetException, IllegalAccessException, 
                   ClassIntrospectionException, ConversionException;
    }
}
