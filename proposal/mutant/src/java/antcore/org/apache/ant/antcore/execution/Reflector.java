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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.ant.common.antlib.Converter;
import org.apache.ant.common.util.AntException;

/**
 * A reflector is used to set attributes and add nested elements to an
 * instance of an object using reflection. It is the result of class
 * introspection.
 *
 * @author Conor MacNeill
 * @created 19 January 2002
 */
public class Reflector implements Setter {

    /**
     * An element adder is used to add an instance of an element to an of an
     * object. The object being added will have been fully configured by Ant
     * prior to calling this method.
     *
     * @author Conor MacNeill
     * @created 19 January 2002
     */
    private interface ElementAdder {
        /**
         * Add an object to the this container object
         *
         * @param container the object to which the element is the be added
         * @param obj an instance of the nested element
         * @exception InvocationTargetException if the method cannot be
         *      invoked
         * @exception IllegalAccessException if the method cannot be invoked
         */
        void add(Object container, Object obj)
             throws InvocationTargetException, IllegalAccessException;
    }

    /**
     * Element Creator's a factory method provided by an Ant Library
     * Component for creating its own nested element instances. These
     * methods are now deprecated. It is better to use the add style methods
     * and support polymorphic interfaces.
     *
     * @author Conor MacNeill
     * @created 31 January 2002
     */
    private interface ElementCreator {
        /**
         * Create a nested element object for the given container object
         *
         * @param container the object in which the nested element is to be
         *      created.
         * @return the nested element.
         * @exception InvocationTargetException if the create method fails
         * @exception IllegalAccessException if the create method cannot be
         *      accessed
         * @exception InstantiationException if the nested element instance
         *      cannot be created.
         */
        Object create(Object container)
             throws InvocationTargetException, IllegalAccessException,
            InstantiationException;
    }


    /** The method used to add content to the element */
    private Method addTextMethod;

    /** the list of attribute setters indexed by their property name */
    private Map attributeSetters = new HashMap();

    /**
     * A list of the Java class or interface accetpted by each element adder
     * indexed by the element name
     */
    private Map elementTypes = new HashMap();

    /** the collection of element adders indexed by their element names */
    private Map elementAdders = new HashMap();

    /** the collection of element creators indexed by their element names */
    private Map elementCreators = new HashMap();

    /**
     * Set an attribute value on an object
     *
     * @param obj the object on which the value is being set
     * @param attributeName the name of the attribute
     * @param value the string represenation of the attribute's value
     * @exception AntException if the object does not support the
     *      attribute or the object has a problem setting the value
     */
    public void setAttribute(Object obj, String attributeName,
                             String value)
         throws AntException {
        String name = attributeName.toLowerCase();
        AttributeSetter as
             = (AttributeSetter) attributeSetters.get(name);
        if (as == null) {
            throw new ExecutionException("Class " + obj.getClass().getName()
                 + " doesn't support the \"" + attributeName + "\" attribute");
        }
        try {
            as.set(obj, value);
        } catch (IllegalAccessException e) {
            // impossible as getMethods should only return public methods
            throw new ExecutionException(e);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof ExecutionException) {
                throw (ExecutionException) t;
            }
            throw new ExecutionException(t);
        }
    }

    /**
     * Set the method used to add content to the element
     *
     * @param addTextMethod the new addTextMethod value
     */
    public void setAddTextMethod(Method addTextMethod) {
        this.addTextMethod = addTextMethod;
    }

    /**
     * Get the type of the given nested element
     *
     * @param elementName the nested element whose type is desired
     * @return the class instance representing the type of the element adder
     */
    public Class getType(String elementName) {
        return (Class) elementTypes.get(elementName);
    }

    /**
     * Adds PCDATA to the element
     *
     * @param obj the instance whose content is being provided
     * @param text the required content
     * @exception ExecutionException if the object does not support
     *      contentor the object has a problem setting the content
     */
    public void addText(Object obj, String text)
         throws ExecutionException {

        if (addTextMethod == null) {
            throw new ExecutionException("Class " + obj.getClass().getName() +
                " doesn't support content");
        }
        try {
            addTextMethod.invoke(obj, new String[]{text});
        } catch (IllegalAccessException ie) {
            // impossible as getMethods should only return public methods
            throw new ExecutionException(ie);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof ExecutionException) {
                throw (ExecutionException) t;
            }
            throw new ExecutionException(t);
        }
    }

    /**
     * Add an element to the given object
     *
     * @param obj The object to which the element is being added
     * @param elementName the name of the element
     * @param value the object to be added - the nested element
     * @exception ExecutionException if the object does not support content
     *      or the object has a problem setting the content
     */
    public void addElement(Object obj, String elementName, Object value)
         throws ExecutionException {
        String name = elementName.toLowerCase();
        ElementAdder adder = (ElementAdder) elementAdders.get(name);
        if (adder == null) {
            throw new ExecutionException("Class " + obj.getClass().getName()
                 + " doesn't support the \"" + elementName
                 + "\" nested element");
        }
        try {
            adder.add(obj, value);
        } catch (IllegalAccessException ie) {
            // impossible as getMethods should only return public methods
            throw new ExecutionException(ie);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof ExecutionException) {
                throw (ExecutionException) t;
            }
            throw new ExecutionException(t);
        }

    }

    /**
     * Create a nested element using the object's element factory method.
     *
     * @param container the object in which the nested element is required.
     * @param elementName the name of the nested element
     * @return the new instance of the nested element
     * @exception ExecutionException if the nested element cannot be
     *      created.
     */
    public Object createElement(Object container, String elementName)
         throws ExecutionException {

        ElementCreator creator
             = (ElementCreator) elementCreators.get(elementName.toLowerCase());
        if (creator == null) {
            throw new ExecutionException("Class "
                 + container.getClass().getName()
                 + " doesn't support the \"" + elementName
                 + "\" nested element");
        }

        try {
            return creator.create(container);
        } catch (IllegalAccessException e) {
            // impossible as getMethods should only return public methods
            throw new ExecutionException(e);
        } catch (InstantiationException e) {
            // impossible as getMethods should only return public methods
            throw new ExecutionException(e);
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof ExecutionException) {
                throw (ExecutionException) t;
            }
            throw new ExecutionException(t);
        }
    }

    /**
     * Indicate if the class assocated with this reflector supports the
     * addition of text content.
     *
     * @return true if the class supports an addText method
     */
    public boolean supportsText() {
        return addTextMethod != null;
    }

    /**
     * Indicate if the class assocated with this reflector supports the
     * given attribute
     *
     * @param attributeName the name of the attribute
     * @return true if the given attribute is supported
     */
    public boolean supportsAttribute(String attributeName) {
        return attributeSetters.containsKey(attributeName.toLowerCase());
    }

    /**
     * Determine if the class associated with this reflector supports a
     * particular nested element via a create factory method
     *
     * @param elementName the name of the element
     * @return true if the class supports creation of that element
     */
    public boolean supportsNestedCreator(String elementName) {
        return elementCreators.containsKey(elementName.toLowerCase());
    }

    /**
     * Determine if the class associated with this reflector supports a
     * particular nested element via an add method
     *
     * @param elementName the name of the element
     * @return true if the class supports addition of that element
     */
    public boolean supportsNestedAdder(String elementName) {
        return elementAdders.containsKey(elementName.toLowerCase());
    }

    /**
     * Add an attribute setter for the given property. The setter will only
     * be added if it does not override a higher priorty setter
     *
     * @param attributeName the name of the attribute that the setter operates
     *        upon.
     * @param setter the AttribnuteSetter instance to use.
     */
    private void addAttributeSetter(String attributeName,
                                    AttributeSetter setter) {
        String name = attributeName.toLowerCase();
        AttributeSetter currentSetter
            = (AttributeSetter) attributeSetters.get(name);
        if (currentSetter != null) {
            // there is a setter, is it lower down in the class hierarchy
            int currentDepth = currentSetter.getDepth();
            if (currentDepth < setter.getDepth()) {
                return;
            } else if (currentDepth == setter.getDepth()) {
                // now check the types
                Class currentType = currentSetter.getType();
                if (currentType != String.class) {
                    return;
                }
            }
        }
        attributeSetters.put(name, setter);
    }


    /**
     * Determine if the class associated with this reflector supports a
     * particular nested element
     *
     * @param elementName the name of the element
     * @return true if the class supports the given type of nested element
     */
    public boolean supportsNestedElement(String elementName) {
        return supportsNestedAdder(elementName)
             || supportsNestedCreator(elementName);
    }

    /**
     * Add a method to the reflector for setting an attribute value
     *
     * @param m the method, obtained by introspection.
     * @param depth the depth of this method's declaration in the class
     *        hierarchy
     * @param propertyName the property name the method will set.
     * @param converters A map of converter classes used to convert strings
     *      to different types.
     */
    public void addAttributeMethod(Method m, int depth,
                                   String propertyName, Map converters) {
        Class type = m.getParameterTypes()[0];

        if (converters != null && converters.containsKey(type)) {
            // we have a converter to use to convert the String
            // value into something the set method expects.
            Converter converter = (Converter) converters.get(type);
            addConvertingSetter(m, depth, propertyName, converter);
            return;
        }

        if (type.equals(String.class)) {
            addAttributeSetter(propertyName, new AttributeSetter(m, depth));
            return;
        }

        try {
            final Constructor c =
                type.getConstructor(new Class[]{java.lang.String.class});
            addAttributeSetter(propertyName, new AttributeSetter(m, depth, c));
            return;
        } catch (NoSuchMethodException nme) {
            // ignore
        }

        if (converters != null) {
            // desparate by now - try top find a converter which handles a super
            // class of this type and which supports subclass instantiation
            for (Iterator i = converters.keySet().iterator(); i.hasNext();) {
                Class converterType = (Class) i.next();
                if (converterType.isAssignableFrom(type)) {
                    // could be a candidate
                    Converter converter
                         = (Converter) converters.get(converterType);
                    if (converter.canConvertSubType(type)) {
                        addConvertingSetter(m, depth, propertyName, converter);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Add an element adder method to the list of element adders in the
     * reflector
     *
     * @param m the adder method
     * @param elementName The name of the element for which this adder works
     */
    public void addElementMethod(final Method m, String elementName) {
        final Class type = m.getParameterTypes()[0];
        elementTypes.put(elementName, type);
        elementAdders.put(elementName.toLowerCase(),
            new ElementAdder() {
                public void add(Object container, Object obj)
                     throws InvocationTargetException, IllegalAccessException {
                    m.invoke(container, new Object[]{obj});
                }
            });
    }


    /**
     * Add a create factory method.
     *
     * @param m the create method
     * @param elementName the name of the nested element the create method
     *      supports.
     */
    public void addCreateMethod(final Method m, String elementName) {
        elementCreators.put(elementName.toLowerCase(),
            new ElementCreator() {
                public Object create(Object container)
                     throws InvocationTargetException, IllegalAccessException {
                    return m.invoke(container, new Object[]{});
                }
            });
    }

    /**
     * Add an attribute setter with an associated converter
     *
     * @param m the attribute setter method
     * @param depth the depth of this method's declaration in the class
     *        hierarchy
     * @param propertyName the name of the attribute this method supports
     * @param converter the converter to be used to construct the value
     *      expected by the method.
     */
    private void addConvertingSetter(Method m, int depth,
                                     String propertyName, Converter converter) {
        addAttributeSetter(propertyName,
            new AttributeSetter(m, depth, converter));
    }
}

