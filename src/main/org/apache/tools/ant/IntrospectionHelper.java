/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.taskdefs.PreSetDef;

/**
 * Helper class that collects the methods a task or nested element
 * holds to set attributes, create nested elements or hold PCDATA
 * elements.
 * The class is final as it has a private constructor.
 *
 * @author Stefan Bodewig
 * @author Peter Reilly
 */
public final class IntrospectionHelper implements BuildListener {

    /**
     * Map from attribute names to attribute types
     * (String to Class).
     */
    private Hashtable attributeTypes;

    /**
     * Map from attribute names to attribute setter methods
     * (String to AttributeSetter).
     */
    private Hashtable attributeSetters;

    /**
     * Map from attribute names to nested types
     * (String to Class).
     */
    private Hashtable nestedTypes;

    /**
     * Map from attribute names to methods to create nested types
     * (String to NestedCreator).
     */
    private Hashtable nestedCreators;

    /**
     * Vector of methods matching add[Configured](Class) pattern
     */
    private List   addTypeMethods;

    /**
     * The method to invoke to add PCDATA.
     */
    private Method addText = null;

    /**
     * The class introspected by this instance.
     */
    private Class bean;

    /**
     * Helper instances we've already created (Class to IntrospectionHelper).
     */
    private static Hashtable helpers = new Hashtable();

    /**
     * Map from primitive types to wrapper classes for use in
     * createAttributeSetter (Class to Class). Note that char
     * and boolean are in here even though they get special treatment
     * - this way we only need to test for the wrapper class.
     */
    private static final Hashtable PRIMITIVE_TYPE_MAP = new Hashtable(8);

    // Set up PRIMITIVE_TYPE_MAP
    static {
        Class[] primitives = {Boolean.TYPE, Byte.TYPE, Character.TYPE,
                              Short.TYPE, Integer.TYPE, Long.TYPE,
                              Float.TYPE, Double.TYPE};
        Class[] wrappers = {Boolean.class, Byte.class, Character.class,
                            Short.class, Integer.class, Long.class,
                            Float.class, Double.class};
        for (int i = 0; i < primitives.length; i++) {
            PRIMITIVE_TYPE_MAP.put (primitives[i], wrappers[i]);
        }
    }

    // XXX: (Jon Skeet) The documentation below doesn't draw a clear
    // distinction between addConfigured and add. It's obvious what the
    // code *here* does (addConfigured sets both a creator method which
    // calls a no-arg constructor and a storer method which calls the
    // method we're looking at, while add just sets a creator method
    // which calls the method we're looking at) but it's not at all
    // obvious what the difference in actual *effect* will be later
    // on. I can't see any mention of addConfiguredXXX in "Developing
    // with Ant" (at least in the version on the web site). Someone
    // who understands should update this documentation
    // (and preferably the manual too) at some stage.
    /**
     * Sole constructor, which is private to ensure that all
     * IntrospectionHelpers are created via {@link #getHelper(Class) getHelper}.
     * Introspects the given class for bean-like methods.
     * Each method is examined in turn, and the following rules are applied:
     * <p>
     * <ul>
     * <li>If the method is <code>Task.setLocation(Location)</code>,
     * <code>Task.setTaskType(String)</code>
     * or <code>TaskContainer.addTask(Task)</code>, it is ignored. These
     * methods are handled differently elsewhere.
     * <li><code>void addText(String)</code> is recognised as the method for
     * adding PCDATA to a bean.
     * <li><code>void setFoo(Bar)</code> is recognised as a method for
     * setting the value of attribute <code>foo</code>, so long as
     * <code>Bar</code> is non-void and is not an array type. Non-String
     * parameter types always overload String parameter types, but that is
     * the only guarantee made in terms of priority.
     * <li><code>Foo createBar()</code> is recognised as a method for
     * creating a nested element called <code>bar</code> of type
     * <code>Foo</code>, so long as <code>Foo</code> is not a primitive or
     * array type.
     * <li><code>void addConfiguredFoo(Bar)</code> is recognised as a
     * method for storing a pre-configured element called
     * <code>foo</code> and of type <code>Bar</code>, so long as
     * <code>Bar</code> is not an array, primitive or String type.
     * <code>Bar</code> must have an accessible constructor taking no
     * arguments.
     * <li><code>void addFoo(Bar)</code> is recognised as a
     * method for storing an element called <code>foobar</code>
     * and of type <code>Baz</code>, so long as
     * <code>Baz</code> is not an array, primitive or String type.
     * <code>Baz</code> must have an accessible constructor taking no
     * arguments.
     * </ul>
     * Note that only one method is retained to create/set/addConfigured/add
     * any element or attribute.
     *
     * @param bean The bean type to introspect.
     *             Must not be <code>null</code>.
     *
     * @see #getHelper(Class)
     */
    private IntrospectionHelper(final Class bean) {
        attributeTypes = new Hashtable();
        attributeSetters = new Hashtable();
        nestedTypes = new Hashtable();
        nestedCreators = new Hashtable();
        addTypeMethods = new ArrayList();

        this.bean = bean;

        Method[] methods = bean.getMethods();
        for (int i = 0; i < methods.length; i++) {
            final Method m = methods[i];
            final String name = m.getName();
            Class returnType = m.getReturnType();
            Class[] args = m.getParameterTypes();

            // check of add[Configured](Class) pattern
            if (args.length == 1
                && java.lang.Void.TYPE.equals(returnType)
                && (name.equals("add") || name.equals("addConfigured"))) {
                insertAddTypeMethod(m);
                continue;
            }

            // not really user settable properties on tasks
            if (org.apache.tools.ant.Task.class.isAssignableFrom(bean)
                 && args.length == 1 && isHiddenSetMethod(name, args[0])) {
                continue;
            }

            // hide addTask for TaskContainers
            if (org.apache.tools.ant.TaskContainer.class.isAssignableFrom(bean)
                && args.length == 1 && "addTask".equals(name)
                && org.apache.tools.ant.Task.class.equals(args[0])) {
                continue;
            }


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
                if (attributeSetters.get(propName) != null) {
                    if (java.lang.String.class.equals(args[0])) {
                        /*
                            Ignore method m, as there is an overloaded
                            form of this method that takes in a
                            non-string argument, which gains higher
                            priority.
                        */
                        continue;
                    }
                    /*
                        If the argument is not a String, and if there
                        is an overloaded form of this method already defined,
                        we just override that with the new one.
                        This mechanism does not guarantee any specific order
                        in which the methods will be selected: so any code
                        that depends on the order in which "set" methods have
                        been defined, is not guaranteed to be selected in any
                        particular order.
                    */
                }
                AttributeSetter as
                    = createAttributeSetter(m, args[0], propName);
                if (as != null) {
                    attributeTypes.put(propName, args[0]);
                    attributeSetters.put(propName, as);
                }

            } else if (name.startsWith("create")
                       && !returnType.isArray()
                       && !returnType.isPrimitive()
                       && args.length == 0) {

                String propName = getPropertyName(name, "create");
                // Check if a create of this property is already present
                // add takes preference over create for CB purposes
                if (nestedCreators.get(propName) == null) {
                    nestedTypes.put(propName, returnType);
                    nestedCreators.put(propName, new NestedCreator() {
                        public boolean isPolyMorphic() {
                            return false;
                        }

                        public Object getRealObject() {
                            return null;
                        }

                        public Class getElementClass() {
                            return null;
                        }

                        public Object create(
                            Project project, Object parent, Object ignore)
                            throws InvocationTargetException,
                            IllegalAccessException {
                            return m.invoke(parent, new Object[] {});
                        }

                        public void store(Object parent, Object child) {
                        }
                    });
                }
            } else if (name.startsWith("addConfigured")
                       && java.lang.Void.TYPE.equals(returnType)
                       && args.length == 1
                       && !java.lang.String.class.equals(args[0])
                       && !args[0].isArray()
                       && !args[0].isPrimitive()) {

                try {
                    Constructor constructor = null;
                    try {
                        constructor =
                            args[0].getConstructor(new Class[] {});
                    } catch (NoSuchMethodException ex) {
                        constructor =
                            args[0].getConstructor(new Class[] {
                                Project.class});
                    }
                    final Constructor c = constructor;
                    String propName = getPropertyName(name, "addConfigured");
                    nestedTypes.put(propName, args[0]);
                    nestedCreators.put(propName, new NestedCreator() {

                            public boolean isPolyMorphic() {
                                return true;
                            }

                            public Object getRealObject() {
                                return null;
                            }

                            public Class getElementClass() {
                                return c.getDeclaringClass();
                            }

                            public Object create(
                                Project project, Object parent, Object child)
                                throws InvocationTargetException,
                                IllegalAccessException, InstantiationException {
                                if (child != null) {
                                    return child;
                                } else if (c.getParameterTypes().length == 0) {
                                    return c.newInstance(new Object[] {});
                                } else {
                                    return c.newInstance(new Object[] {
                                        project});
                                }
                            }

                            public void store(Object parent, Object child)
                                throws InvocationTargetException,
                                IllegalAccessException, InstantiationException {

                                m.invoke(parent, new Object[] {child});
                            }

                        });
                } catch (NoSuchMethodException nse) {
                    // ignore
                }
            } else if (name.startsWith("add")
                       && java.lang.Void.TYPE.equals(returnType)
                       && args.length == 1
                       && !java.lang.String.class.equals(args[0])
                       && !args[0].isArray()
                       && !args[0].isPrimitive()) {

                try {
                    Constructor constructor = null;
                    try {
                        constructor =
                            args[0].getConstructor(new Class[] {});
                    } catch (NoSuchMethodException ex) {
                        constructor =
                            args[0].getConstructor(new Class[] {
                                Project.class});
                    }
                    final Constructor c = constructor;
                    String propName = getPropertyName(name, "add");
                    nestedTypes.put(propName, args[0]);
                    nestedCreators.put(propName, new NestedCreator() {

                            public boolean isPolyMorphic() {
                                return true;
                            }

                            public Object getRealObject() {
                                return null;
                            }

                            public Class getElementClass() {
                                return c.getDeclaringClass();
                            }

                            public Object create(
                                Project project, Object parent, Object child)
                                throws InvocationTargetException,
                                       IllegalAccessException, InstantiationException {
                                if (child != null) {
                                    // ignore
                                } else if (c.getParameterTypes().length == 0) {
                                    child = c.newInstance(new Object[] {});
                                } else {
                                    child = c.newInstance(new Object[] {
                                        project});
                                }
                                m.invoke(parent, new Object[] {child});
                                return child;
                            }
                            public void store(Object parent, Object child)
                                throws InvocationTargetException,
                                       IllegalAccessException, InstantiationException {

                            }

                        });
                } catch (NoSuchMethodException nse) {
                    // ignore
                }
            }
        }
    }

    /**
     * Certain set methods are part of the Ant core interface to tasks and
     * therefore not to be considered for introspection
     *
     * @param name the name of the set method
     * @param type the type of the set method's parameter
     * @return true if the given set method is to be hidden.
     */
    private boolean isHiddenSetMethod(String name, Class type) {
        if ("setLocation".equals(name)
             && org.apache.tools.ant.Location.class.equals(type)) {
            return true;
        }

        if  ("setTaskType".equals(name)
             && java.lang.String.class.equals(type)) {
            return true;
        }

        return false;
    }

    /**
     * Returns a helper for the given class, either from the cache
     * or by creating a new instance.
     *
     * @param c The class for which a helper is required.
     *          Must not be <code>null</code>.
     *
     * @return a helper for the specified class
     */
    public static synchronized IntrospectionHelper getHelper(Class c) {
        IntrospectionHelper ih = (IntrospectionHelper) helpers.get(c);
        if (ih == null) {
            ih = new IntrospectionHelper(c);
            helpers.put(c, ih);
        }
        return ih;
    }

    /**
     * Returns a helper for the given class, either from the cache
     * or by creating a new instance.
     *
     * The method will make sure the helper will be cleaned up at the end of
     * the project, and only one instance will be created for each class.
     *
     * @param p the project instance
     * @param c The class for which a helper is required.
     *          Must not be <code>null</code>.
     *
     * @return a helper for the specified class
     */
    public static synchronized IntrospectionHelper getHelper(Project p, Class c) {
        IntrospectionHelper ih = (IntrospectionHelper) helpers.get(c);
        if (ih == null) {
            ih = new IntrospectionHelper(c);
            helpers.put(c, ih);
            // Cleanup at end of project
            p.addBuildListener(ih);
        }
        return ih;
    }

    /**
     * Sets the named attribute in the given element, which is part of the
     * given project.
     *
     * @param p The project containing the element. This is used when files
     *          need to be resolved. Must not be <code>null</code>.
     * @param element The element to set the attribute in. Must not be
     *                <code>null</code>.
     * @param attributeName The name of the attribute to set. Must not be
     *                      <code>null</code>.
     * @param value The value to set the attribute to. This may be interpreted
     *              or converted to the necessary type if the setter method
     *              doesn't just take a string. Must not be <code>null</code>.
     *
     * @exception BuildException if the introspected class doesn't support
     *                           the given attribute, or if the setting
     *                           method fails.
     */
    public void setAttribute(Project p, Object element, String attributeName,
                             String value) throws BuildException {
        AttributeSetter as
            = (AttributeSetter) attributeSetters.get(attributeName);
        if (as == null) {
            if (element instanceof DynamicConfigurator) {
                DynamicConfigurator dc = (DynamicConfigurator) element;
                dc.setDynamicAttribute(attributeName, value);
                return;
            } else {
                String msg = getElementName(p, element)
                    + " doesn't support the \"" + attributeName
                    + "\" attribute.";
                throw new BuildException(msg);
            }
        }
        try {
            as.set(p, element, value);
        } catch (IllegalAccessException ie) {
            // impossible as getMethods should only return public methods
            throw new BuildException(ie);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof BuildException) {
                throw (BuildException) t;
            }
            throw new BuildException(t);
        }
    }

    /**
     * Adds PCDATA to an element, using the element's
     * <code>void addText(String)</code> method, if it has one. If no
     * such method is present, a BuildException is thrown if the
     * given text contains non-whitespace.
     *
     * @param project The project which the element is part of.
     *                Must not be <code>null</code>.
     * @param element The element to add the text to.
     *                Must not be <code>null</code>.
     * @param text    The text to add.
     *                Must not be <code>null</code>.
     *
     * @exception BuildException if non-whitespace text is provided and no
     *                           method is available to handle it, or if
     *                           the handling method fails.
     */
    public void addText(Project project, Object element, String text)
        throws BuildException {
        if (addText == null) {
            // Element doesn't handle text content
            if (text.trim().length() == 0) {
                // Only whitespace - ignore
                return;
            } else {
                // Not whitespace - fail
                String msg = project.getElementName(element)
                    + " doesn't support nested text data.";
                throw new BuildException(msg);
            }
        }
        try {
            addText.invoke(element, new String[] {text});
        } catch (IllegalAccessException ie) {
            // impossible as getMethods should only return public methods
            throw new BuildException(ie);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof BuildException) {
                throw (BuildException) t;
            }
            throw new BuildException(t);
        }
    }

    /**
     * Utility method to throw a NotSupported exception
     *
     * @param project the Project instance.
     * @param parent the object which doesn't support a requested element
     * @param elementName the name of the Element which is trying to be created.
     */
    public void throwNotSupported(Project project, Object parent,
        String elementName) {
        String msg = project.getElementName(parent)
            + " doesn't support the nested \"" + elementName + "\" element.";
        throw new BuildException(msg);
    }

    private NestedCreator getNestedCreator(Project project, Object parent,
        String elementName) throws BuildException {

        NestedCreator nc = (NestedCreator) nestedCreators.get(
            elementName.toLowerCase(Locale.US));
        if (nc == null) {
            nc = createAddTypeCreator(project, parent, elementName);
        }
        if (nc == null && parent instanceof DynamicConfigurator) {
            DynamicConfigurator dc = (DynamicConfigurator) parent;
            final Object nestedElement = dc.createDynamicElement(elementName);
            if (nestedElement != null) {
                nc = new NestedCreator() {
                    public boolean isPolyMorphic() {
                        return false;
                    }
                    public Class getElementClass() {
                        return null;
                    }

                    public Object getRealObject() {
                        return null;
                    }

                    public Object create(
                        Project project, Object parent, Object ignore) {
                        return nestedElement;
                    }
                    public void store(Object parent, Object child) {
                    }
                };
            }
        }
        if (nc == null) {
            throwNotSupported(project, parent, elementName);
        }
        return nc;
    }

    private NestedCreator getNestedCreator(
        Project project, String parentUri, Object parent,
        String elementName) throws BuildException {

        String uri = ProjectHelper.extractUriFromComponentName(elementName);
        String name = ProjectHelper.extractNameFromComponentName(elementName);

        NestedCreator nc = null;
        if (uri.equals(parentUri)) { //  || uri.equals("")) {
            nc = (NestedCreator) nestedCreators.get(
                name.toLowerCase(Locale.US));
        }
        if (nc == null) {
            nc = createAddTypeCreator(project, parent, elementName);
        }
        if (nc == null && parent instanceof DynamicConfigurator) {
            DynamicConfigurator dc = (DynamicConfigurator) parent;
            final Object nestedElement =
                dc.createDynamicElement(name.toLowerCase(Locale.US));
            if (nestedElement != null) {
                nc = new NestedCreator() {
                    public boolean isPolyMorphic() {
                        return false;
                    }
                    public Class getElementClass() {
                        return null;
                    }

                    public Object getRealObject() {
                        return null;
                    }

                    public Object create(
                        Project project, Object parent, Object ignore) {
                        return nestedElement;
                    }
                    public void store(Object parent, Object child) {
                    }
                };
            }
        }
        if (nc == null) {
            throwNotSupported(project, parent, elementName);
        }
        return nc;
    }

    /**
     * Creates a named nested element. Depending on the results of the
     * initial introspection, either a method in the given parent instance
     * or a simple no-arg constructor is used to create an instance of the
     * specified element type.
     *
     * @param project Project to which the parent object belongs.
     *                Must not be <code>null</code>. If the resulting
     *                object is an instance of ProjectComponent, its
     *                Project reference is set to this parameter value.
     * @param parent  Parent object used to create the instance.
     *                Must not be <code>null</code>.
     * @param elementName Name of the element to create an instance of.
     *                    Must not be <code>null</code>.
     *
     * @return an instance of the specified element type
     *
     * @exception BuildException if no method is available to create the
     *                           element instance, or if the creating method
     *                           fails.
     */
    public Object createElement(Project project, Object parent,
        String elementName) throws BuildException {
        NestedCreator nc = getNestedCreator(project, parent, elementName);
        try {
            Object nestedElement = nc.create(project, parent, null);
            if (project != null) {
                project.setProjectReference(nestedElement);
            }
            return nestedElement;
        } catch (IllegalAccessException ie) {
            // impossible as getMethods should only return public methods
            throw new BuildException(ie);
        } catch (InstantiationException ine) {
            // impossible as getMethods should only return public methods
            throw new BuildException(ine);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof BuildException) {
                throw (BuildException) t;
            }
            throw new BuildException(t);
        }
    }

    /**
     * returns an object that creates and stores an object
     * for an element of a parent.
     *
     * @param project      Project to which the parent object belongs.
     * @param parentUri    The namespace uri of the parent object.
     * @param parent       Parent object used to create the creator object to
     *                     create and store and instance of a subelement.
     * @param elementName  Name of the element to create an instance of.
     * @return a creator object to create and store the element instance.
     */

    public Creator getElementCreator(
        Project project, String parentUri, Object parent, String elementName) {
        NestedCreator nc = getNestedCreator(
            project, parentUri, parent, elementName);
        return new Creator(project, parent, nc);
    }

    /**
     * Indicate if this element supports a nested element of the
     * given name.
     *
     * @param elementName the name of the nested element being checked
     *
     * @return true if the given nested element is supported
     */
    public boolean supportsNestedElement(String elementName) {
        return nestedCreators.containsKey(elementName.toLowerCase(Locale.US))
            || DynamicConfigurator.class.isAssignableFrom(bean)
            || addTypeMethods.size() != 0;
    }

    /**
     * Indicate if this element supports a nested element of the
     * given name.
     *
     * @param parentUri   the uri of the parent
     * @param elementName the name of the nested element being checked
     *
     * @return true if the given nested element is supported
     */
    public boolean supportsNestedElement(String parentUri, String elementName) {
        String uri = ProjectHelper.extractUriFromComponentName(elementName);
        String name = ProjectHelper.extractNameFromComponentName(elementName);

        return (
            nestedCreators.containsKey(name.toLowerCase(Locale.US))
            && (uri.equals(parentUri))) // || uri.equals("")))
            || DynamicConfigurator.class.isAssignableFrom(bean)
            || addTypeMethods.size() != 0;
    }

    /**
     * Stores a named nested element using a storage method determined
     * by the initial introspection. If no appropriate storage method
     * is available, this method returns immediately.
     *
     * @param project Ignored in this implementation.
     *                May be <code>null</code>.
     *
     * @param parent  Parent instance to store the child in.
     *                Must not be <code>null</code>.
     *
     * @param child   Child instance to store in the parent.
     *                Should not be <code>null</code>.
     *
     * @param elementName  Name of the child element to store.
     *                     May be <code>null</code>, in which case
     *                     this method returns immediately.
     *
     * @exception BuildException if the storage method fails.
     */
    public void storeElement(Project project, Object parent, Object child,
        String elementName) throws BuildException {
        if (elementName == null) {
            return;
        }
        NestedCreator ns = (NestedCreator) nestedCreators.get(
            elementName.toLowerCase(Locale.US));
        if (ns == null) {
            return;
        }
        try {
            ns.store(parent, child);
        } catch (IllegalAccessException ie) {
            // impossible as getMethods should only return public methods
            throw new BuildException(ie);
        } catch (InstantiationException ine) {
            // impossible as getMethods should only return public methods
            throw new BuildException(ine);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof BuildException) {
                throw (BuildException) t;
            }
            throw new BuildException(t);
        }
    }

    /**
     * Returns the type of a named nested element.
     *
     * @param elementName The name of the element to find the type of.
     *                    Must not be <code>null</code>.
     *
     * @return the type of the nested element with the specified name.
     *         This will never be <code>null</code>.
     *
     * @exception BuildException if the introspected class does not
     *                           support the named nested element.
     */
    public Class getElementType(String elementName)
        throws BuildException {
        Class nt = (Class) nestedTypes.get(elementName);
        if (nt == null) {
            String msg = "Class " + bean.getName()
                + " doesn't support the nested \"" + elementName
                + "\" element.";
            throw new BuildException(msg);
        }
        return nt;
    }

    /**
     * Returns the type of a named attribute.
     *
     * @param attributeName The name of the attribute to find the type of.
     *                      Must not be <code>null</code>.
     *
     * @return the type of the attribute with the specified name.
     *         This will never be <code>null</code>.
     *
     * @exception BuildException if the introspected class does not
     *                           support the named attribute.
     */
    public Class getAttributeType(String attributeName)
        throws BuildException {
        Class at = (Class) attributeTypes.get(attributeName);
        if (at == null) {
            String msg = "Class " + bean.getName()
                + " doesn't support the \"" + attributeName + "\" attribute.";
            throw new BuildException(msg);
        }
        return at;
    }

    /**
     * Returns whether or not the introspected class supports PCDATA.
     *
     * @return whether or not the introspected class supports PCDATA.
     */
    public boolean supportsCharacters() {
        return addText != null;
    }

    /**
     * Returns an enumeration of the names of the attributes supported
     * by the introspected class.
     *
     * @return an enumeration of the names of the attributes supported
     *         by the introspected class.
     */
    public Enumeration getAttributes() {
        return attributeSetters.keys();
    }

    /**
     * Returns an enumeration of the names of the nested elements supported
     * by the introspected class.
     *
     * @return an enumeration of the names of the nested elements supported
     *         by the introspected class.
     */
    public Enumeration getNestedElements() {
        return nestedTypes.keys();
    }

    /**
     * Creates an implementation of AttributeSetter for the given
     * attribute type. Conversions (where necessary) are automatically
     * made for the following types:
     * <ul>
     * <li>String (left as it is)
     * <li>Character/char (first character is used)
     * <li>Boolean/boolean
     * ({@link Project#toBoolean(String) Project.toBoolean(String)} is used)
     * <li>Class (Class.forName is used)
     * <li>File (resolved relative to the appropriate project)
     * <li>Path (resolve relative to the appropriate project)
     * <li>EnumeratedAttribute (uses its own
     * {@link EnumeratedAttribute#setValue(String) setValue} method)
     * <li>Other primitive types (wrapper classes are used with constructors
     * taking String)
     * </ul>
     *
     * If none of the above covers the given parameters, a constructor for the
     * appropriate class taking a String parameter is used if it is available.
     *
     * @param m The method to invoke on the bean when the setter is invoked.
     *          Must not be <code>null</code>.
     * @param arg The type of the single argument of the bean's method.
     *            Must not be <code>null</code>.
     * @param attrName the name of the attribute for which the setter is being
     *                 created.
     *
     * @return an appropriate AttributeSetter instance, or <code>null</code>
     *         if no appropriate conversion is available.
     */
    private AttributeSetter createAttributeSetter(final Method m,
                                                  Class arg,
                                                  final String attrName) {
        // use wrappers for primitive classes, e.g. int and
        // Integer are treated identically
        final Class reflectedArg = PRIMITIVE_TYPE_MAP.containsKey (arg)
            ? (Class) PRIMITIVE_TYPE_MAP.get(arg) : arg;

        // simplest case - setAttribute expects String
        if (java.lang.String.class.equals(reflectedArg)) {
            return new AttributeSetter() {
                    public void set(Project p, Object parent, String value)
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent, new String[] {value});
                    }
                };

        // char and Character get special treatment - take the first character
        } else if (java.lang.Character.class.equals(reflectedArg)) {
            return new AttributeSetter() {
                    public void set(Project p, Object parent, String value)
                        throws InvocationTargetException, IllegalAccessException {
                        if (value.length() == 0) {
                            throw new BuildException("The value \"\" is not a "
                                + "legal value for attribute \""
                                + attrName + "\"");
                        }
                        m.invoke(parent, new Character[] {new Character(value.charAt(0))});
                    }

                };
        // boolean and Boolean get special treatment because we
        // have a nice method in Project
        } else if (java.lang.Boolean.class.equals(reflectedArg)) {
            return new AttributeSetter() {
                    public void set(Project p, Object parent, String value)
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent,
                                 new Boolean[] {
                                     new Boolean(Project.toBoolean(value))});
                    }

                };

        // Class doesn't have a String constructor but a decent factory method
        } else if (java.lang.Class.class.equals(reflectedArg)) {
            return new AttributeSetter() {
                    public void set(Project p, Object parent, String value)
                        throws InvocationTargetException, IllegalAccessException, BuildException {
                        try {
                            m.invoke(parent, new Class[] {Class.forName(value)});
                        } catch (ClassNotFoundException ce) {
                            throw new BuildException(ce);
                        }
                    }
                };

        // resolve relative paths through Project
        } else if (java.io.File.class.equals(reflectedArg)) {
            return new AttributeSetter() {
                    public void set(Project p, Object parent, String value)
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent, new File[] {p.resolveFile(value)});
                    }

                };

        // resolve relative paths through Project
        } else if (org.apache.tools.ant.types.Path.class.equals(reflectedArg)) {
            return new AttributeSetter() {
                    public void set(Project p, Object parent, String value)
                        throws InvocationTargetException, IllegalAccessException {
                        m.invoke(parent, new Path[] {new Path(p, value)});
                    }

                };

        // EnumeratedAttributes have their own helper class
        } else if (EnumeratedAttribute.class.isAssignableFrom(reflectedArg)) {
            return new AttributeSetter() {
                    public void set(Project p, Object parent, String value)
                        throws InvocationTargetException, IllegalAccessException, BuildException {
                        try {
                            EnumeratedAttribute ea =
                                (EnumeratedAttribute) reflectedArg.newInstance();
                            ea.setValue(value);
                            m.invoke(parent, new EnumeratedAttribute[] {ea});
                        } catch (InstantiationException ie) {
                            throw new BuildException(ie);
                        }
                    }
                };

        // worst case. look for a public String constructor and use it
        // This is used (deliberately) for all primitives/wrappers other than
        // char and boolean
        } else {

            try {
                final Constructor c =
                    reflectedArg.getConstructor(new Class[] {java.lang.String.class});

                return new AttributeSetter() {
                        public void set(Project p, Object parent,
                                        String value)
                            throws InvocationTargetException,
                                   IllegalAccessException, BuildException {
                            try {
                                Object attribute = c.newInstance(new String[] {value});
                                if (p != null) {
                                    p.setProjectReference(attribute);
                                }
                                m.invoke(parent, new Object[] {attribute});
                            } catch (InstantiationException ie) {
                                throw new BuildException(ie);
                            }
                        }
                    };

            } catch (NoSuchMethodException nme) {
                // ignore
            }
        }

        return null;
    }

    /**
     * Returns a description of the type of the given element in
     * relation to a given project. This is used for logging purposes
     * when the element is asked to cope with some data it has no
     * way of handling.
     *
     * @param project The project the element is defined in.
     *                Must not be <code>null</code>.
     *
     * @param element The element to describe.
     *                Must not be <code>null</code>.
     *
     * @return a description of the element type
     */
    protected String getElementName(Project project, Object element) {
        return project.getElementName(element);
    }

    /**
     * Extracts the name of a property from a method name by subtracting
     * a given prefix and converting into lower case. It is up to calling
     * code to make sure the method name does actually begin with the
     * specified prefix - no checking is done in this method.
     *
     * @param methodName The name of the method in question.
     *                   Must not be <code>null</code>.
     * @param prefix     The prefix to remove.
     *                   Must not be <code>null</code>.
     *
     * @return the lower-cased method name with the prefix removed.
     */
    private String getPropertyName(String methodName, String prefix) {
        int start = prefix.length();
        return methodName.substring(start).toLowerCase(Locale.US);
    }

    /**
     * creator - allows use of create/store external
     * to IntrospectionHelper.
     * The class is final as it has a private constructor.
     */
    public static final class Creator {
        private NestedCreator nestedCreator;
        private Object parent;
        private Project project;
        private Object nestedObject;
        private String polyType;

        /**
         * Creates a new Creator instance.
         * This object is given to the UnknownElement to create
         * objects for sub-elements. UnknownElement calls
         * create to create an object, the object then gets
         * configured and then UnknownElement calls store.
         * SetPolyType may be used to override the type used
         * to create the object with. SetPolyType gets called
         * before create.
         *
         * @param project the current project
         * @param parent  the parent object to create the object in
         * @param nestedCreator the nested creator object to use
         */
        private Creator(
            Project project, Object parent, NestedCreator nestedCreator) {
            this.project = project;
            this.parent = parent;
            this.nestedCreator = nestedCreator;
        }

        /**
         * Used to override the class used to create the object.
         *
         * @param polyType a ant component type name
         */
        public void setPolyType(String polyType) {
            this.polyType = polyType;
        }

        /**
         * Create an object using this creator, which is determined
         * by introspection.
         *
         * @return the created object
         */
        public Object create() {
            if (polyType != null) {
                if (!nestedCreator.isPolyMorphic()) {
                    throw new BuildException(
                        "Not allowed to use the polymorphic form"
                        + " for this element");
                }
                Class elementClass = nestedCreator.getElementClass();
                ComponentHelper helper =
                    ComponentHelper.getComponentHelper(project);
                nestedObject = ComponentHelper.getComponentHelper(project)
                    .createComponent(polyType);
                if (nestedObject == null) {
                    throw new BuildException(
                        "Unable to create object of type " + polyType);
                }
            }
            try {
                nestedObject = nestedCreator.create(
                    project, parent, nestedObject);
                if (project != null) {
                    project.setProjectReference(nestedObject);
                }
                return nestedObject;
            } catch (IllegalAccessException ex) {
                throw new BuildException(ex);
            } catch (InstantiationException ex) {
                throw new BuildException(ex);
            } catch (IllegalArgumentException ex) {
                if (polyType != null) {
                    throw new BuildException(
                        "Invalid type used " + polyType);
                }
                throw ex;
            } catch (InvocationTargetException ex) {
                Throwable t = ex.getTargetException();
                if (t instanceof BuildException) {
                    throw (BuildException) t;
                }
                throw new BuildException(t);
            }
        }

        /**
         * @return the real object (used currently only
         *         for preset def)
         */
        public Object getRealObject() {
            return nestedCreator.getRealObject();
        }

        /**
         * Stores the nested element object using a storage method
         * determined by introspection.
         *
         */
        public void store() {
            try {
                nestedCreator.store(parent, nestedObject);
            } catch (IllegalAccessException ex) {
                throw new BuildException(ex);
            } catch (InstantiationException ex) {
                throw new BuildException(ex);
            } catch (IllegalArgumentException ex) {
                if (polyType != null) {
                    throw new BuildException(
                        "Invalid type used " + polyType);
                }
                throw ex;
            } catch (InvocationTargetException ex) {
                Throwable t = ex.getTargetException();
                if (t instanceof BuildException) {
                    throw (BuildException) t;
                }
                throw new BuildException(t);
            }
        }
    }

    /**
     * Internal interface used to create nested elements. Not documented
     * in detail for reasons of source code readability.
     */
    private interface NestedCreator {
        boolean isPolyMorphic();
        Class getElementClass();
        Object getRealObject();
        Object create(Project project, Object parent, Object child)
            throws InvocationTargetException, IllegalAccessException, InstantiationException;
        void store(Object parent, Object child)
            throws InvocationTargetException, IllegalAccessException, InstantiationException;
    }


    /**
     * Internal interface used to setting element attributes. Not documented
     * in detail for reasons of source code readability.
     */
    private interface AttributeSetter {
        void set(Project p, Object parent, String value)
            throws InvocationTargetException, IllegalAccessException,
                   BuildException;
    }

    /**
     * Clears all storage used by this class, including the static cache of
     * helpers.
     *
     * @param event Ignored in this implementation.
     */
    public void buildFinished(BuildEvent event) {
        attributeTypes.clear();
        attributeSetters.clear();
        nestedTypes.clear();
        nestedCreators.clear();
        addText = null;
        helpers.clear();
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     * @param event Ignored in this implementation.
     */
    public void buildStarted(BuildEvent event) {
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     *
     * @param event Ignored in this implementation.
     */
    public void targetStarted(BuildEvent event) {
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     *
     * @param event Ignored in this implementation.
     */
    public void targetFinished(BuildEvent event) {
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     *
     * @param event Ignored in this implementation.
     */
    public void taskStarted(BuildEvent event) {
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     *
     * @param event Ignored in this implementation.
     */
    public void taskFinished(BuildEvent event) {
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     *
     * @param event Ignored in this implementation.
     */
    public void messageLogged(BuildEvent event) {
    }


    /**
     *
     */
    private NestedCreator createAddTypeCreator(
        Project project, Object parent, String elementName)
        throws BuildException {
        if (addTypeMethods.size() == 0) {
            return null;
        }
        ComponentHelper helper = ComponentHelper.getComponentHelper(project);

        Object addedObject = null;
        Method addMethod = null;
        Class clazz = helper.getComponentClass(elementName);
        if (clazz == null) {
            return null;
        }
        addMethod = findMatchingMethod(clazz, addTypeMethods);
        if (addMethod == null) {
            return null;
        }
        addedObject = helper.createComponent(elementName);
        if (addedObject == null) {
            return null;
        }
        Object rObject = addedObject;
        if (addedObject instanceof PreSetDef.PreSetDefinition) {
            rObject = ((PreSetDef.PreSetDefinition) addedObject).createObject(
                project);
        }
        final Method method = addMethod;
        final Object nestedObject = addedObject;
        final Object realObject = rObject;

        return new NestedCreator() {
            public boolean isPolyMorphic() {
                return false;
            }

            public Class getElementClass() {
                return null;
            }
            public Object create(Project project, Object parent, Object ignore)
                throws InvocationTargetException, IllegalAccessException {
                if (!method.getName().endsWith("Configured")) {
                    method.invoke(parent, new Object[]{realObject});
                }
                return nestedObject;
            }

            public Object getRealObject() {
                return realObject;
            }

            public void store(Object parent, Object child)
                throws InvocationTargetException, IllegalAccessException,
                InstantiationException {
                if (method.getName().endsWith("Configured")) {
                    method.invoke(parent, new Object[]{realObject});
                }
            }
        };
    }

    /**
     * Inserts an add or addConfigured method into
     * the addTypeMethods array. The array is
     * ordered so that the more derived classes
     * are first.
     */

    private void insertAddTypeMethod(Method method) {
        Class argClass = method.getParameterTypes()[0];
        for (int c = 0; c < addTypeMethods.size(); ++c) {
            Method current = (Method) addTypeMethods.get(c);
            if (current.getParameterTypes()[0].equals(argClass)) {
                return; // Already present
            }
            if (current.getParameterTypes()[0].isAssignableFrom(
                            argClass)) {
                addTypeMethods.add(c, method);
                return; // higher derived
            }
        }
        addTypeMethods.add(method);
    }


    /**
     * Search the list of methods to find the first method
     * that has a parameter that accepts the nested element object
     */
    private Method findMatchingMethod(Class paramClass, List methods) {
        Class matchedClass = null;
        Method matchedMethod = null;

        for (int i = 0; i < methods.size(); ++i) {
            Method method = (Method) methods.get(i);
            Class  methodClass = method.getParameterTypes()[0];
            if (methodClass.isAssignableFrom(paramClass)) {
                if (matchedClass == null) {
                    matchedClass = methodClass;
                    matchedMethod = method;
                } else {
                    if (!methodClass.isAssignableFrom(matchedClass)) {
                        throw new BuildException("ambiguous: types "
                            + matchedClass.getName() + " and "
                            + methodClass.getName() + " match "
                            + paramClass.getName());
                    }
                }
            }
        }
        return matchedMethod;
    }

}
