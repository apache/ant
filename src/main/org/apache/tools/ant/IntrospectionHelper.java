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
package org.apache.tools.ant;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.tools.ant.taskdefs.PreSetDef;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.StringUtils;

/**
 * Helper class that collects the methods a task or nested element
 * holds to set attributes, create nested elements or hold PCDATA
 * elements.
 *
 * It contains hashtables containing classes that use introspection
 * to handle all the invocation of the project-component specific methods.
 *
 * This class is somewhat complex, as it implements the O/X mapping between
 * Ant XML and Java class instances. This is not the best place for someone new
 * to Ant to start contributing to the codebase, as a change here can break the
 * entire system in interesting ways. Always run a full test of Ant before checking
 * in/submitting changes to this file.
 *
 * The class is final and has a private constructor.
 * To get an instance for a specific (class,project) combination,
 * use {@link #getHelper(Project,Class)}.
 * This may return an existing version, or a new one
 * ...do not make any assumptions about its uniqueness, or its validity after the Project
 * instance has finished its build.
 *
 */
public final class IntrospectionHelper {

    /**
     * Helper instances we've already created (Class.getName() to IntrospectionHelper).
     */
    private static final Map<String, IntrospectionHelper> HELPERS = new Hashtable<>();

    /**
     * Map from primitive types to wrapper classes for use in
     * createAttributeSetter (Class to Class). Note that char
     * and boolean are in here even though they get special treatment
     * - this way we only need to test for the wrapper class.
     */
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPE_MAP = new HashMap<>(8);

    // Set up PRIMITIVE_TYPE_MAP
    static {
        final Class<?>[] primitives = {Boolean.TYPE, Byte.TYPE, Character.TYPE, Short.TYPE,
                              Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE};
        final Class<?>[] wrappers = {Boolean.class, Byte.class, Character.class, Short.class,
                            Integer.class, Long.class, Float.class, Double.class};
        for (int i = 0; i < primitives.length; i++) {
            PRIMITIVE_TYPE_MAP.put(primitives[i], wrappers[i]);
        }
    }

    private static final int MAX_REPORT_NESTED_TEXT = 20;
    private static final String ELLIPSIS = "...";

    /**
     * Map from attribute names to attribute types
     * (String to Class).
     */
    private final Map<String, Class<?>> attributeTypes = new Hashtable<>();

    /**
     * Map from attribute names to attribute setter methods
     * (String to AttributeSetter).
     */
    private final Map<String, AttributeSetter> attributeSetters = new Hashtable<>();

    /**
     * Map from attribute names to nested types
     * (String to Class).
     */
    private final Map<String, Class<?>> nestedTypes = new Hashtable<>();

    /**
     * Map from attribute names to methods to create nested types
     * (String to NestedCreator).
     */
    private final Map<String, NestedCreator> nestedCreators = new Hashtable<>();

    /**
     * Vector of methods matching add[Configured](Class) pattern.
     */
    private final List<Method> addTypeMethods = new ArrayList<>();

    /**
     * The method to invoke to add PCDATA.
     */
    private final Method addText;

    /**
     * The class introspected by this instance.
     */
    private final Class<?> bean;

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
     * <code>Bar</code> is non-void and is not an array type.
     * As of Ant 1.8, a Resource or FileProvider parameter overrides a java.io.File parameter;
     * in practice the only effect of this is to allow objects rendered from
     * the 1.8 PropertyHelper implementation to be used as Resource parameters,
     * since Resources set from Strings are resolved as project-relative files
     * to preserve backward compatibility.  Beyond this, non-String
     * parameter types always overload String parameter types; these are
     * the only guarantees made in terms of priority.
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
     * <li><code>void addFoo(Bar)</code> is recognised as a method for storing
     * an element called <code>foo</code> and of type <code>Bar</code>, so
     * long as <code>Bar</code> is not an array, primitive or String type.
     * <code>Bar</code> must have an accessible constructor taking no
     * arguments. This is distinct from the 'addConfigured' idiom in that
     * the nested element is added to the parent immediately after it is
     * constructed; in practice this means that <code>addFoo(Bar)</code> should
     * do little or nothing with its argument besides storing it for later use.
     * </ul>
     * Note that only one method is retained to create/set/addConfigured/add
     * any element or attribute.
     *
     * @param bean The bean type to introspect.
     *             Must not be <code>null</code>.
     *
     * @see #getHelper(Class)
     */
    private IntrospectionHelper(final Class<?> bean) {
        this.bean = bean;
        Method addTextMethod = null;
        for (final Method m : bean.getMethods()) {
            final String name = m.getName();
            final Class<?> returnType = m.getReturnType();
            final Class<?>[] args = m.getParameterTypes();

            // check of add[Configured](Class) pattern
            if (args.length == 1 && Void.TYPE.equals(returnType)
                    && ("add".equals(name) || "addConfigured".equals(name))) {
                insertAddTypeMethod(m);
                continue;
            }
            // not really user settable properties on tasks/project components
            if (ProjectComponent.class.isAssignableFrom(bean)
                    && args.length == 1 && isHiddenSetMethod(name, args[0])) {
                continue;
            }
            // hide addTask for TaskContainers
            if (isContainer() && args.length == 1 && "addTask".equals(name)
                    && Task.class.equals(args[0])) {
                continue;
            }
            if ("addText".equals(name) && Void.TYPE.equals(returnType)
                    && args.length == 1 && String.class.equals(args[0])) {
                addTextMethod = m;
            } else if (name.startsWith("set") && Void.TYPE.equals(returnType)
                    && args.length == 1 && !args[0].isArray()) {
                final String propName = getPropertyName(name, "set");
                AttributeSetter as = attributeSetters.get(propName);
                if (as != null) {
                    if (String.class.equals(args[0])) {
                        /*
                            Ignore method m, as there is an overloaded
                            form of this method that takes in a
                            non-string argument, which gains higher
                            priority.
                        */
                        continue;
                    }
                    if (File.class.equals(args[0])) {
                        // Ant Resources/FileProviders override java.io.File
                        if (Resource.class.equals(as.type) || FileProvider.class.equals(as.type)) {
                            continue;
                        }
                    }
                    /*
                        In cases other than those just explicitly covered,
                        we just override that with the new one.
                        This mechanism does not guarantee any specific order
                        in which the methods will be selected: so any code
                        that depends on the order in which "set" methods have
                        been defined, is not guaranteed to be selected in any
                        particular order.
                    */
                }
                as = createAttributeSetter(m, args[0], propName);
                if (as != null) {
                    attributeTypes.put(propName, args[0]);
                    attributeSetters.put(propName, as);
                }
            } else if (name.startsWith("create") && !returnType.isArray()
                    && !returnType.isPrimitive() && args.length == 0) {

                final String propName = getPropertyName(name, "create");
                // Check if a create of this property is already present
                // add takes preference over create for CB purposes
                if (nestedCreators.get(propName) == null) {
                    nestedTypes.put(propName, returnType);
                    nestedCreators.put(propName, new CreateNestedCreator(m));
                }
            } else if (name.startsWith("addConfigured")
                    && Void.TYPE.equals(returnType) && args.length == 1
                    && !String.class.equals(args[0])
                    && !args[0].isArray() && !args[0].isPrimitive()) {
                try {
                    Constructor<?> constructor = null;
                    try {
                        constructor = args[0].getConstructor();
                    } catch (final NoSuchMethodException ex) {
                        constructor = args[0].getConstructor(Project.class);
                    }
                    final String propName = getPropertyName(name, "addConfigured");
                    nestedTypes.put(propName, args[0]);
                    nestedCreators.put(propName, new AddNestedCreator(m,
                        constructor, AddNestedCreator.ADD_CONFIGURED));
                } catch (final NoSuchMethodException nse) {
                    // ignore
                }
            } else if (name.startsWith("add")
                    && Void.TYPE.equals(returnType) && args.length == 1
                    && !String.class.equals(args[0])
                    && !args[0].isArray() && !args[0].isPrimitive()) {
                try {
                    Constructor<?> constructor = null;
                    try {
                        constructor = args[0].getConstructor();
                    } catch (final NoSuchMethodException ex) {
                        constructor = args[0].getConstructor(Project.class);
                    }
                    final String propName = getPropertyName(name, "add");
                    if (nestedTypes.get(propName) != null) {
                        /*
                         *  Ignore this method as there is an addConfigured
                         *  form of this method that has a higher
                         *  priority
                         */
                        continue;
                    }
                    nestedTypes.put(propName, args[0]);
                    nestedCreators.put(propName, new AddNestedCreator(m,
                            constructor, AddNestedCreator.ADD));
                } catch (final NoSuchMethodException nse) {
                    // ignore
                }
            }
        }
        addText = addTextMethod;
    }

    /**
     * Certain set methods are part of the Ant core interface to tasks and
     * therefore not to be considered for introspection
     *
     * @param name the name of the set method
     * @param type the type of the set method's parameter
     * @return true if the given set method is to be hidden.
     */
    private boolean isHiddenSetMethod(final String name, final Class<?> type) {
        return "setLocation".equals(name) && Location.class.equals(type)
                || "setTaskType".equals(name) && String.class.equals(type);
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
    public static IntrospectionHelper getHelper(final Class<?> c) {
        return getHelper(null, c);
    }

    /**
     * Returns a helper for the given class, either from the cache
     * or by creating a new instance.
     *
     * The method will make sure the helper will be cleaned up at the end of
     * the project, and only one instance will be created for each class.
     *
     * @param p the project instance. Can be null, in which case the helper is not cached.
     * @param c The class for which a helper is required.
     *          Must not be <code>null</code>.
     *
     * @return a helper for the specified class
     */
    public static IntrospectionHelper getHelper(final Project p, final Class<?> c) {
        if (p == null) {
            // #30162: do *not* use cache if there is no project, as we
            // cannot guarantee that the cache will be cleared.
            return new IntrospectionHelper(c);
        }
        IntrospectionHelper ih = HELPERS.get(c.getName());
        if (ih != null && ih.bean == c) {
            return ih;
        }
        // If a helper cannot be found, or if the helper is for another
        // classloader, create a new IH and cache it
        // Note: This new instance of IntrospectionHelper is intentionally
        // created without holding a lock, to prevent potential deadlocks.
        // See bz-65424 for details
        ih = new IntrospectionHelper(c);
        synchronized (HELPERS) {
            IntrospectionHelper cached = HELPERS.get(c.getName());
            if (cached != null && cached.bean == c) {
                return cached;
            }
            // cache the recently created one
            HELPERS.put(c.getName(), ih);
            return ih;
        }
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
     *              doesn't accept an object of the supplied type.
     *
     * @exception BuildException if the introspected class doesn't support
     *                           the given attribute, or if the setting
     *                           method fails.
     */
    public void setAttribute(final Project p, final Object element, final String attributeName,
            final Object value) throws BuildException {
        final AttributeSetter as = attributeSetters.get(
                attributeName.toLowerCase(Locale.ENGLISH));
        if (as == null && value != null) {
            if (element instanceof DynamicAttributeNS) {
                final DynamicAttributeNS dc = (DynamicAttributeNS) element;
                final String uriPlusPrefix = ProjectHelper.extractUriFromComponentName(attributeName);
                final String uri = ProjectHelper.extractUriFromComponentName(uriPlusPrefix);
                final String localName = ProjectHelper.extractNameFromComponentName(attributeName);
                final String qName = uri.isEmpty() ? localName : uri + ":" + localName;
                dc.setDynamicAttribute(uri, localName, qName, value.toString());
                return;
            }
            if (element instanceof DynamicObjectAttribute) {
                final DynamicObjectAttribute dc = (DynamicObjectAttribute) element;
                dc.setDynamicAttribute(attributeName.toLowerCase(Locale.ENGLISH), value);
                return;
            }
            if (element instanceof DynamicAttribute) {
                final DynamicAttribute dc = (DynamicAttribute) element;
                dc.setDynamicAttribute(attributeName.toLowerCase(Locale.ENGLISH), value.toString());
                return;
            }
            if (attributeName.contains(":")) {
                return; // Ignore attribute from unknown uri's
            }
            final String msg = getElementName(p, element)
                    + " doesn't support the \"" + attributeName + "\" attribute.";
            throw new UnsupportedAttributeException(msg, attributeName);
        }
        if (as != null) { // possible if value == null
            try {
                as.setObject(p, element, value);
            } catch (final IllegalAccessException ie) {
                // impossible as getMethods should only return public methods
                throw new BuildException(ie);
            } catch (final InvocationTargetException ite) {
                throw extractBuildException(ite);
            }
        }
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
    public void setAttribute(final Project p, final Object element, final String attributeName,
                             final String value) throws BuildException {
        setAttribute(p, element, attributeName, (Object) value);
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
    public void addText(final Project project, final Object element, String text)
        throws BuildException {
        if (addText == null) {
            text = text.trim();
            // Element doesn't handle text content
            if (text.isEmpty()) {
                // Only whitespace - ignore
                return;
            }
            // Not whitespace - fail
            throw new BuildException(project.getElementName(element)
                    + " doesn't support nested text data (\"" + condenseText(text) + "\").");
        }
        try {
            addText.invoke(element, text);
        } catch (final IllegalAccessException ie) {
            // impossible as getMethods should only return public methods
            throw new BuildException(ie);
        } catch (final InvocationTargetException ite) {
            throw extractBuildException(ite);
        }
    }

    /**
     * part of the error message created by {@link #throwNotSupported
     * throwNotSupported}.
     * @since Ant 1.8.0
     */
    protected static final String NOT_SUPPORTED_CHILD_PREFIX =
        " doesn't support the nested \"";

    /**
     * part of the error message created by {@link #throwNotSupported
     * throwNotSupported}.
     * @since Ant 1.8.0
     */
    protected static final String NOT_SUPPORTED_CHILD_POSTFIX = "\" element.";

    /**
     * Utility method to throw a NotSupported exception
     *
     * @param project the Project instance.
     * @param parent the object which doesn't support a requested element
     * @param elementName the name of the Element which is trying to be created.
     */
    public void throwNotSupported(final Project project, final Object parent, final String elementName) {
        final String msg = project.getElementName(parent)
            + NOT_SUPPORTED_CHILD_PREFIX + elementName
            + NOT_SUPPORTED_CHILD_POSTFIX;
        throw new UnsupportedElementException(msg, elementName);
    }

    /**
     * Get the specific NestedCreator for a given project/parent/element combination
     * @param project ant project
     * @param parentUri URI of the parent.
     * @param parent the parent class
     * @param elementName element to work with. This can contain
     *  a URI,localname tuple of of the form uri:localname
     * @param child the bit of XML to work with
     * @return a nested creator that can handle the child elements.
     * @throws BuildException if the parent does not support child elements of that name
     */
    private NestedCreator getNestedCreator(
        final Project project, String parentUri, final Object parent,
        final String elementName, final UnknownElement child) throws BuildException {

        String uri = ProjectHelper.extractUriFromComponentName(elementName);
        final String name = ProjectHelper.extractNameFromComponentName(elementName);

        if (uri.equals(ProjectHelper.ANT_CORE_URI)) {
            uri = "";
        }
        if (parentUri.equals(ProjectHelper.ANT_CORE_URI)) {
            parentUri = "";
        }
        NestedCreator nc = null;
        if (uri.equals(parentUri) || uri.isEmpty()) {
            nc = nestedCreators.get(name.toLowerCase(Locale.ENGLISH));
        }
        if (nc == null) {
            nc = createAddTypeCreator(project, parent, elementName);
        }
        if (nc == null
                && (parent instanceof DynamicElementNS || parent instanceof DynamicElement)) {
            final String qName = child == null ? name : child.getQName();
            final Object nestedElement = createDynamicElement(parent,
                    child == null ? "" : child.getNamespace(), name, qName);
            if (nestedElement != null) {
                nc = new NestedCreator(null) {
                    @Override
                    Object create(final Project project, final Object parent, final Object ignore) {
                        return nestedElement;
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
     * Invokes the "correct" createDynamicElement method on parent in
     * order to obtain a child element by name.
     *
     * @since Ant 1.8.0.
     */
    private Object createDynamicElement(final Object parent, final String ns,
                                        final String localName, final String qName) {
        Object nestedElement = null;
        if (parent instanceof DynamicElementNS) {
            final DynamicElementNS dc = (DynamicElementNS) parent;
            nestedElement = dc.createDynamicElement(ns, localName, qName);
        }
        if (nestedElement == null && parent instanceof DynamicElement) {
            final DynamicElement dc = (DynamicElement) parent;
            nestedElement =
                dc.createDynamicElement(localName.toLowerCase(Locale.ENGLISH));
        }
        return nestedElement;
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
     * @deprecated since 1.6.x.
     *             This is not a namespace aware method.
     *
     * @exception BuildException if no method is available to create the
     *                           element instance, or if the creating method fails.
     */
    @Deprecated
    public Object createElement(final Project project, final Object parent, final String elementName)
            throws BuildException {
        final NestedCreator nc = getNestedCreator(project, "", parent, elementName, null);
        try {
            final Object nestedElement = nc.create(project, parent, null);
            if (project != null) {
                project.setProjectReference(nestedElement);
            }
            return nestedElement;
        } catch (final IllegalAccessException | InstantiationException ie) {
            // impossible as getMethods should only return public methods
            throw new BuildException(ie);
        } catch (final InvocationTargetException ite) {
            throw extractBuildException(ite);
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
     * @param ue           The unknown element associated with the element.
     * @return a creator object to create and store the element instance.
     */
    public Creator getElementCreator(
        final Project project, final String parentUri, final Object parent, final String elementName, final UnknownElement ue) {
        final NestedCreator nc = getNestedCreator(project, parentUri, parent, elementName, ue);
        return new Creator(project, parent, nc);
    }

    /**
     * Indicates whether the introspected class is a dynamic one,
     * supporting arbitrary nested elements and/or attributes.
     *
     * @return <div><code>true</code> if the introspected class is dynamic;
     *         <code>false</code> otherwise.</div>
     * @since Ant 1.6.3
     *
     * @see DynamicElement
     * @see DynamicElementNS
     */
    public boolean isDynamic() {
        return DynamicElement.class.isAssignableFrom(bean)
                || DynamicElementNS.class.isAssignableFrom(bean);
    }

    /**
     * Indicates whether the introspected class is a task container,
     * supporting arbitrary nested tasks/types.
     *
     * @return <code>true</code> if the introspected class is a container;
     *         <code>false</code> otherwise.
     * @since Ant 1.6.3
     *
     * @see TaskContainer
     */
    public boolean isContainer() {
        return TaskContainer.class.isAssignableFrom(bean);
    }

    /**
     * Indicates if this element supports a nested element of the
     * given name.
     *
     * @param elementName the name of the nested element being checked
     *
     * @return true if the given nested element is supported
     */
    public boolean supportsNestedElement(final String elementName) {
        return supportsNestedElement("", elementName);
    }

    /**
     * Indicate if this element supports a nested element of the
     * given name.
     *
     * <p>Note that this method will always return true if the
     * introspected class is {@link #isDynamic dynamic} or contains a
     * method named "add" with void return type and a single argument.
     * To ge a more thorough answer, use the four-arg version of this
     * method instead.</p>
     *
     * @param parentUri   the uri of the parent
     * @param elementName the name of the nested element being checked
     *
     * @return true if the given nested element is supported
     */
    public boolean supportsNestedElement(final String parentUri, final String elementName) {
        return isDynamic() || !addTypeMethods.isEmpty()
                || supportsReflectElement(parentUri, elementName);
    }

    /**
     * Indicate if this element supports a nested element of the
     * given name.
     *
     * <p>Note that this method will always return true if the
     * introspected class is {@link #isDynamic dynamic}, so be
     * prepared to catch an exception about unsupported children when
     * calling {@link #getElementCreator getElementCreator}.</p>
     *
     * @param parentUri   the uri of the parent
     * @param elementName the name of the nested element being checked
     * @param project currently executing project instance
     * @param parent the parent element
     *
     * @return true if the given nested element is supported
     * @since Ant 1.8.0.
     */
    public boolean supportsNestedElement(final String parentUri, final String elementName,
                                         final Project project, final Object parent) {
        return !addTypeMethods.isEmpty()
                && createAddTypeCreator(project, parent, elementName) != null
                || isDynamic() || supportsReflectElement(parentUri, elementName);
    }

    /**
     * Check if this element supports a nested element from reflection.
     *
     * @param parentUri   the uri of the parent
     * @param elementName the name of the nested element being checked
     *
     * @return true if the given nested element is supported
     * @since Ant 1.8.0
     */
    public boolean supportsReflectElement(
        String parentUri, final String elementName) {
        final String name = ProjectHelper.extractNameFromComponentName(elementName);
        if (!nestedCreators.containsKey(name.toLowerCase(Locale.ENGLISH))) {
            return false;
        }
        String uri = ProjectHelper.extractUriFromComponentName(elementName);
        if (uri.equals(ProjectHelper.ANT_CORE_URI) || uri.isEmpty()) {
            return true;
        }
        if (parentUri.equals(ProjectHelper.ANT_CORE_URI)) {
            parentUri = "";
        }
        return uri.equals(parentUri);
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
    public void storeElement(final Project project, final Object parent, final Object child,
        final String elementName) throws BuildException {
        if (elementName == null) {
            return;
        }
        final NestedCreator ns = nestedCreators.get(elementName.toLowerCase(Locale.ENGLISH));
        if (ns == null) {
            return;
        }
        try {
            ns.store(parent, child);
        } catch (final IllegalAccessException | InstantiationException ie) {
            // impossible as getMethods should only return public methods
            throw new BuildException(ie);
        } catch (final InvocationTargetException ite) {
            throw extractBuildException(ite);
        }
    }

    /**
     * Helper method to extract the inner fault from an {@link InvocationTargetException}, and turn
     * it into a BuildException. If it is already a BuildException, it is type cast and returned; if
     * not a new BuildException is created containing the child as nested text.
     * @param ite the exception
     * @return the nested exception
     */
    private static BuildException extractBuildException(final InvocationTargetException ite) {
        final Throwable t = ite.getTargetException();
        if (t instanceof BuildException) {
            return (BuildException) t;
        }
        return new BuildException(t);
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
    public Class<?> getElementType(final String elementName) throws BuildException {
        final Class<?> nt = nestedTypes.get(elementName);
        if (nt == null) {
            throw new UnsupportedElementException("Class "
                    + bean.getName() + " doesn't support the nested \""
                    + elementName + "\" element.", elementName);
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
    public Class<?> getAttributeType(final String attributeName) throws BuildException {
        final Class<?> at = attributeTypes.get(attributeName);
        if (at == null) {
            throw new UnsupportedAttributeException("Class "
                    + bean.getName() + " doesn't support the \""
                    + attributeName + "\" attribute.", attributeName);
        }
        return at;
    }

    /**
     * Returns the addText method when the introspected
     * class supports nested text.
     *
     * @return the method on this introspected class that adds nested text.
     *         Cannot be <code>null</code>.
     * @throws BuildException if the introspected class does not
     *         support the nested text.
     * @since Ant 1.6.3
     */
    public Method getAddTextMethod() throws BuildException {
        if (!supportsCharacters()) {
            throw new BuildException("Class " + bean.getName()
                    + " doesn't support nested text data.");
        }
        return addText;
    }

    /**
     * Returns the adder or creator method of a named nested element.
     *
     * @param  elementName The name of the attribute to find the setter
     *         method of. Must not be <code>null</code>.
     * @return the method on this introspected class that adds or creates this
     *         nested element. Can be <code>null</code> when the introspected
     *         class is a dynamic configurator!
     * @throws BuildException if the introspected class does not
     *         support the named nested element.
     * @since Ant 1.6.3
     */
    public Method getElementMethod(final String elementName) throws BuildException {
        final NestedCreator creator = nestedCreators.get(elementName);
        if (creator == null) {
            throw new UnsupportedElementException("Class "
                    + bean.getName() + " doesn't support the nested \""
                    + elementName + "\" element.", elementName);
        }
        return creator.method;
    }

    /**
     * Returns the setter method of a named attribute.
     *
     * @param  attributeName The name of the attribute to find the setter
     *         method of. Must not be <code>null</code>.
     * @return the method on this introspected class that sets this attribute.
     *         This will never be <code>null</code>.
     * @throws BuildException if the introspected class does not
     *         support the named attribute.
     * @since Ant 1.6.3
     */
    public Method getAttributeMethod(final String attributeName) throws BuildException {
        final AttributeSetter setter = attributeSetters.get(attributeName);
        if (setter == null) {
            throw new UnsupportedAttributeException("Class "
                    + bean.getName() + " doesn't support the \""
                    + attributeName + "\" attribute.", attributeName);
        }
        return setter.method;
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
     * Returns an enumeration of the names of the attributes supported by the introspected class.
     *
     * @return an enumeration of the names of the attributes supported by the introspected class.
     * @see #getAttributeMap
     */
    public Enumeration<String> getAttributes() {
        return Collections.enumeration(attributeSetters.keySet());
    }

    /**
     * Returns a read-only map of attributes supported by the introspected class.
     *
     * @return an attribute name to attribute <code>Class</code>
     *         unmodifiable map. Can be empty, but never <code>null</code>.
     * @since Ant 1.6.3
     */
    public Map<String, Class<?>> getAttributeMap() {
        return attributeTypes.isEmpty()
            ? Collections.emptyMap() : Collections.unmodifiableMap(attributeTypes);
    }

    /**
     * Returns an enumeration of the names of the nested elements supported
     * by the introspected class.
     *
     * @return an enumeration of the names of the nested elements supported
     *         by the introspected class.
     * @see #getNestedElementMap
     */
    public Enumeration<String> getNestedElements() {
        return Collections.enumeration(nestedTypes.keySet());
    }

    /**
     * Returns a read-only map of nested elements supported
     * by the introspected class.
     *
     * @return a nested-element name to nested-element <code>Class</code>
     *         unmodifiable map. Can be empty, but never <code>null</code>.
     * @since Ant 1.6.3
     */
    public Map<String, Class<?>> getNestedElementMap() {
        return nestedTypes.isEmpty()
            ? Collections.emptyMap() : Collections.unmodifiableMap(nestedTypes);
    }

    /**
     * Returns a read-only list of extension points supported
     * by the introspected class.
     * <p>
     * A task/type or nested element with void methods named <code>add()</code>
     * or <code>addConfigured()</code>, taking a single class or interface
     * argument, supports extensions point. This method returns the list of
     * all these <em>void add[Configured](type)</em> methods.
     *
     * @return a list of void, single argument add() or addConfigured()
     *         <code>Method</code>s of all supported extension points.
     *         These methods are sorted such that if the argument type of a
     *         method derives from another type also an argument of a method
     *         of this list, the method with the most derived argument will
     *         always appear first. Can be empty, but never <code>null</code>.
     * @since Ant 1.6.3
     */
    public List<Method> getExtensionPoints() {
        return addTypeMethods.isEmpty()
                ? Collections.emptyList() : Collections.unmodifiableList(addTypeMethods);
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
     * <li>Resource (resolved as a FileResource relative to the appropriate project)
     * <li>FileProvider (resolved as a FileResource relative to the appropriate project)
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
                                                  final Class<?> arg,
                                                  final String attrName) {
        // use wrappers for primitive classes, e.g. int and
        // Integer are treated identically
        final Class<?> reflectedArg = PRIMITIVE_TYPE_MAP.getOrDefault(arg, arg);

        // Object.class - it gets handled differently by AttributeSetter
        if (java.lang.Object.class == reflectedArg) {
            return new AttributeSetter(m, arg) {
                @Override
                public void set(final Project p, final Object parent, final String value)
                        throws InvocationTargetException,
                    IllegalAccessException {
                    throw new BuildException(
                        "Internal ant problem - this should not get called");
                }
            };
        }
        // simplest case - setAttribute expects String
        if (String.class.equals(reflectedArg)) {
            return new AttributeSetter(m, arg) {
                @Override
                public void set(final Project p, final Object parent, final String value)
                        throws InvocationTargetException, IllegalAccessException {
                    m.invoke(parent, (Object[]) new String[] {value});
                }
            };
        }
        // char and Character get special treatment - take the first character
        if (java.lang.Character.class.equals(reflectedArg)) {
            return new AttributeSetter(m, arg) {
                @Override
                public void set(final Project p, final Object parent, final String value)
                        throws InvocationTargetException, IllegalAccessException {
                    if (value.isEmpty()) {
                        throw new BuildException("The value \"\" is not a "
                                + "legal value for attribute \"" + attrName + "\"");
                    }
                    m.invoke(parent, (Object[]) new Character[] {value.charAt(0)});
                }
            };
        }
        // boolean and Boolean get special treatment because we have a nice method in Project
        if (java.lang.Boolean.class.equals(reflectedArg)) {
            return new AttributeSetter(m, arg) {
                @Override
                public void set(final Project p, final Object parent, final String value)
                        throws InvocationTargetException, IllegalAccessException {
                    m.invoke(parent, (Object[]) new Boolean[] {
                            Project.toBoolean(value) ? Boolean.TRUE : Boolean.FALSE });
                }
            };
        }
        // Class doesn't have a String constructor but a decent factory method
        if (java.lang.Class.class.equals(reflectedArg)) {
            return new AttributeSetter(m, arg) {
                @Override
                public void set(final Project p, final Object parent, final String value)
                        throws InvocationTargetException, IllegalAccessException, BuildException {
                    try {
                        m.invoke(parent, Class.forName(value));
                    } catch (final ClassNotFoundException ce) {
                        throw new BuildException(ce);
                    }
                }
            };
        }
        // resolve relative paths through Project
        if (java.io.File.class.equals(reflectedArg)) {
            return new AttributeSetter(m, arg) {
                @Override
                public void set(final Project p, final Object parent, final String value)
                        throws InvocationTargetException, IllegalAccessException {
                    m.invoke(parent, p.resolveFile(value));
                }
            };
        }
        // resolve relative nio paths through Project
        if (java.nio.file.Path.class.equals(reflectedArg)) {
            return new AttributeSetter(m, arg) {
                @Override
                public void set(final Project p, final Object parent, final String value)
                        throws InvocationTargetException, IllegalAccessException {
                    m.invoke(parent, p.resolveFile(value).toPath());
                }
            };
        }

        // resolve Resources/FileProviders as FileResources relative to Project:
        if (Resource.class.equals(reflectedArg) || FileProvider.class.equals(reflectedArg)) {
            return new AttributeSetter(m, arg) {
                @Override
                void set(final Project p, final Object parent, final String value)
                        throws InvocationTargetException, IllegalAccessException, BuildException {
                    m.invoke(parent, new FileResource(p, p.resolveFile(value)));
                }
            };
        }
        // EnumeratedAttributes have their own helper class
        if (EnumeratedAttribute.class.isAssignableFrom(reflectedArg)) {
            return new AttributeSetter(m, arg) {
                @Override
                public void set(final Project p, final Object parent, final String value)
                        throws InvocationTargetException, IllegalAccessException, BuildException {
                    try {
                        final EnumeratedAttribute ea =
                                (EnumeratedAttribute) reflectedArg.getDeclaredConstructor().newInstance();
                        ea.setValue(value);
                        m.invoke(parent, ea);
                    } catch (final InstantiationException | NoSuchMethodException ie) {
                        throw new BuildException(ie);
                    }
                }
            };
        }

        final AttributeSetter setter = getEnumSetter(reflectedArg, m, arg);
        if (setter != null) {
            return setter;
        }

        if (java.lang.Long.class.equals(reflectedArg)) {
            return new AttributeSetter(m, arg) {
                @Override
                public void set(final Project p, final Object parent, final String value)
                        throws InvocationTargetException, IllegalAccessException, BuildException {
                    try {
                        m.invoke(parent, StringUtils.parseHumanSizes(value));
                    } catch (final NumberFormatException e) {
                        throw new BuildException("Can't assign non-numeric"
                                                 + " value '" + value + "' to"
                                                 + " attribute " + attrName);
                    } catch (final InvocationTargetException | IllegalAccessException e) {
                        throw e;
                    } catch (final Exception e) {
                        throw new BuildException(e);
                    }
                }
            };
        }
        // worst case. look for a public String constructor and use it
        // also supports new Whatever(Project, String) as for Path or Reference
        // This is used (deliberately) for all primitives/wrappers other than
        // char, boolean, and long.
        boolean includeProject;
        Constructor<?> c;
        try {
            // First try with Project.
            c = reflectedArg.getConstructor(Project.class, String.class);
            includeProject = true;
        } catch (final NoSuchMethodException nme) {
            // OK, try without.
            try {
                c = reflectedArg.getConstructor(String.class);
                includeProject = false;
            } catch (final NoSuchMethodException nme2) {
                // Well, no matching constructor.
                return null;
            }
        }
        final boolean finalIncludeProject = includeProject;
        final Constructor<?> finalConstructor = c;

        return new AttributeSetter(m, arg) {
            @Override
            public void set(final Project p, final Object parent, final String value)
                    throws InvocationTargetException, IllegalAccessException, BuildException {
                try {
                    final Object[] args = finalIncludeProject
                            ? new Object[] {p, value} : new Object[] {value};

                    final Object attribute = finalConstructor.newInstance(args);
                    if (p != null) {
                        p.setProjectReference(attribute);
                    }
                    m.invoke(parent, attribute);
                } catch (final InvocationTargetException e) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof IllegalArgumentException) {
                        throw new BuildException("Can't assign value '" + value
                                                 + "' to attribute " + attrName
                                                 + ", reason: "
                                                 + cause.getClass()
                                                 + " with message '"
                                                 + cause.getMessage() + "'");
                    }
                    throw e;
                } catch (final InstantiationException ie) {
                    throw new BuildException(ie);
                }
            }
        };
    }

    private AttributeSetter getEnumSetter(
        final Class<?> reflectedArg, final Method m, final Class<?> arg) {
        if (reflectedArg.isEnum()) {
            return new AttributeSetter(m, arg) {
                @Override
                public void set(final Project p, final Object parent, final String value)
                    throws InvocationTargetException, IllegalAccessException,
                    BuildException {
                    Enum<?> setValue;
                    try {
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        final Enum<?> enumValue = Enum.valueOf((Class<? extends Enum>) reflectedArg,
                                value);
                        setValue = enumValue;
                    } catch (final IllegalArgumentException e) {
                        // there is a specific logic here for the value
                        // being out of the allowed set of enumerations.
                        throw new BuildException("'" + value + "' is not a permitted value for "
                                + reflectedArg.getName());
                    }
                    m.invoke(parent, setValue);
                }
            };
        }
        return null;
    }

    /**
     * Returns a description of the type of the given element in
     * relation to a given project. This is used for logging purposes
     * when the element is asked to cope with some data it has no way of handling.
     *
     * @param project The project the element is defined in. Must not be <code>null</code>.
     *
     * @param element The element to describe. Must not be <code>null</code>.
     *
     * @return a description of the element type
     */
    private String getElementName(final Project project, final Object element) {
        return project.getElementName(element);
    }

    /**
     * Extracts the name of a property from a method name by subtracting
     * a given prefix and converting into lower case. It is up to calling
     * code to make sure the method name does actually begin with the
     * specified prefix - no checking is done in this method.
     *
     * @param methodName The name of the method in question. Must not be <code>null</code>.
     * @param prefix     The prefix to remove. Must not be <code>null</code>.
     *
     * @return the lower-cased method name with the prefix removed.
     */
    private static String getPropertyName(final String methodName, final String prefix) {
        return methodName.substring(prefix.length()).toLowerCase(Locale.ENGLISH);
    }

    /**
     * creator - allows use of create/store external
     * to IntrospectionHelper.
     * The class is final as it has a private constructor.
     */
    public static final class Creator {
        private final NestedCreator nestedCreator;
        private final Object parent;
        private final Project project;
        private Object nestedObject;
        private String polyType;

        /**
         * Creates a new Creator instance.
         * This object is given to the UnknownElement to create
         * objects for sub-elements. UnknownElement calls
         * create to create an object, the object then gets
         * configured and then UnknownElement calls store.
         * SetPolyType may be used to override the type used
         * to create the object with. SetPolyType gets called before create.
         *
         * @param project the current project
         * @param parent  the parent object to create the object in
         * @param nestedCreator the nested creator object to use
         */
        private Creator(final Project project, final Object parent, final NestedCreator nestedCreator) {
            this.project = project;
            this.parent = parent;
            this.nestedCreator = nestedCreator;
        }

        /**
         * Used to override the class used to create the object.
         *
         * @param polyType a ant component type name
         */
        public void setPolyType(final String polyType) {
            this.polyType = polyType;
        }

        /**
         * Create an object using this creator, which is determined by introspection.
         *
         * @return the created object
         */
        public Object create() {
            if (polyType != null) {
                if (!nestedCreator.isPolyMorphic()) {
                    throw new BuildException(
                            "Not allowed to use the polymorphic form for this element");
                }
                final ComponentHelper helper = ComponentHelper.getComponentHelper(project);
                nestedObject = helper.createComponent(polyType);
                if (nestedObject == null) {
                    throw new BuildException("Unable to create object of type " + polyType);
                }
            }
            try {
                nestedObject = nestedCreator.create(project, parent, nestedObject);
                if (project != null) {
                    project.setProjectReference(nestedObject);
                }
                return nestedObject;
            } catch (final IllegalAccessException | InstantiationException ex) {
                throw new BuildException(ex);
            } catch (final IllegalArgumentException ex) {
                if (polyType == null) {
                    throw ex;
                }
                throw new BuildException("Invalid type used " + polyType);
            } catch (final InvocationTargetException ex) {
                throw extractBuildException(ex);
            }
        }

        /**
         * @return the real object (used currently only for presetdef).
         */
        public Object getRealObject() {
            return nestedCreator.getRealObject();
        }

        /**
         * Stores the nested element object using a storage method determined by introspection.
         *
         */
        public void store() {
            try {
                nestedCreator.store(parent, nestedObject);
            } catch (final IllegalAccessException | InstantiationException ex) {
                throw new BuildException(ex);
            } catch (final IllegalArgumentException ex) {
                if (polyType == null) {
                    throw ex;
                }
                throw new BuildException("Invalid type used " + polyType);
            } catch (final InvocationTargetException ex) {
                throw extractBuildException(ex);
            }
        }
    }

    /**
     * Internal interface used to create nested elements. Not documented
     * in detail for reasons of source code readability.
     */
    private abstract static class NestedCreator {
        private final Method method; // the method called to add/create the nested element

        protected NestedCreator(final Method m) {
            method = m;
        }
        Method getMethod() {
            return method;
        }
        boolean isPolyMorphic() {
            return false;
        }
        Object getRealObject() {
            return null;
        }
        abstract Object create(Project project, Object parent, Object child)
                throws InvocationTargetException, IllegalAccessException, InstantiationException;

        void store(final Object parent, final Object child)
                 throws InvocationTargetException, IllegalAccessException, InstantiationException {
            // DO NOTHING
        }
    }

    private static class CreateNestedCreator extends NestedCreator {
        CreateNestedCreator(final Method m) {
            super(m);
        }

        @Override
        Object create(final Project project, final Object parent, final Object ignore)
                throws InvocationTargetException, IllegalAccessException {
            return getMethod().invoke(parent);
        }
    }

    /** Version to use for addXXX and addConfiguredXXX */
    private static class AddNestedCreator extends NestedCreator {

        static final int ADD = 1;
        static final int ADD_CONFIGURED = 2;

        private final Constructor<?> constructor;
        private final int behavior; // ADD or ADD_CONFIGURED

        AddNestedCreator(final Method m, final Constructor<?> c, final int behavior) {
            super(m);
            this.constructor = c;
            this.behavior = behavior;
        }

        @Override
        boolean isPolyMorphic() {
            return true;
        }

        @Override
        Object create(final Project project, final Object parent, Object child)
                throws InvocationTargetException, IllegalAccessException, InstantiationException {
            if (child == null) {
                child = constructor.newInstance(
                        constructor.getParameterTypes().length == 0
                                ? new Object[] {} : new Object[] {project});
            }
            if (child instanceof PreSetDef.PreSetDefinition) {
                child = ((PreSetDef.PreSetDefinition) child).createObject(project);
            }
            if (behavior == ADD) {
                istore(parent, child);
            }
            return child;
        }

        @Override
        void store(final Object parent, final Object child)
                throws InvocationTargetException, IllegalAccessException, InstantiationException {
            if (behavior == ADD_CONFIGURED) {
                istore(parent, child);
            }
        }

        private void istore(final Object parent, final Object child)
                throws InvocationTargetException, IllegalAccessException {
            getMethod().invoke(parent, child);
        }
    }

    /**
     * Internal interface used to setting element attributes. Not documented
     * in detail for reasons of source code readability.
     */
    private abstract static class AttributeSetter {
        private final Method method; // the method called to set the attribute
        private final Class<?> type;
        protected AttributeSetter(final Method m, final Class<?> type) {
            method = m;
            this.type = type;
        }
        void setObject(final Project p, final Object parent, final Object value)
                throws InvocationTargetException, IllegalAccessException, BuildException {
            if (type != null) {
                Class<?> useType = type;
                if (type.isPrimitive()) {
                    if (value == null) {
                        throw new BuildException(
                            "Attempt to set primitive "
                            + getPropertyName(method.getName(), "set")
                            + " to null on " + parent);
                    }
                    useType = PRIMITIVE_TYPE_MAP.get(type);
                }
                if (value == null || useType.isInstance(value)) {
                    method.invoke(parent, value);
                    return;
                }
            }
            set(p, parent, value.toString());
        }
        abstract void set(Project p, Object parent, String value)
                throws InvocationTargetException, IllegalAccessException, BuildException;
    }

    /**
     * Clears the static cache of on build finished.
     */
    public static void clearCache() {
        HELPERS.clear();
    }

    /**
     * Create a NestedCreator for the given element.
     * @param project owning project
     * @param parent Parent object used to create the instance.
     * @param elementName name of the element
     * @return a nested creator, or null if there is no component of the given name, or it
     *        has no matching add type methods
     * @throws BuildException if something goes wrong
     */
    private NestedCreator createAddTypeCreator(
            final Project project, final Object parent, final String elementName) throws BuildException {
        if (addTypeMethods.isEmpty()) {
            return null;
        }
        final ComponentHelper helper = ComponentHelper.getComponentHelper(project);

        final MethodAndObject restricted =  createRestricted(helper, elementName, addTypeMethods);
        final MethodAndObject topLevel = createTopLevel(helper, elementName, addTypeMethods);

        if (restricted == null && topLevel == null) {
            return null;
        }

        if (restricted != null && topLevel != null) {
            throw new BuildException(
                "ambiguous: type and component definitions for "
                + elementName);
        }

        final MethodAndObject methodAndObject = restricted == null ? topLevel : restricted;

        Object rObject = methodAndObject.object;
        if (methodAndObject.object instanceof PreSetDef.PreSetDefinition) {
            rObject = ((PreSetDef.PreSetDefinition) methodAndObject.object)
                .createObject(project);
        }
        final Object nestedObject = methodAndObject.object;
        final Object realObject = rObject;

        return new NestedCreator(methodAndObject.method) {
            @Override
            Object create(final Project project, final Object parent, final Object ignore)
                    throws InvocationTargetException, IllegalAccessException {
                if (!getMethod().getName().endsWith("Configured")) {
                    getMethod().invoke(parent, realObject);
                }
                return nestedObject;
            }

            @Override
            Object getRealObject() {
                return realObject;
            }

            @Override
            void store(final Object parent, final Object child) throws InvocationTargetException,
                    IllegalAccessException, InstantiationException {
                if (getMethod().getName().endsWith("Configured")) {
                    getMethod().invoke(parent, realObject);
                }
            }
        };
    }

    /**
     * Inserts an add or addConfigured method into
     * the addTypeMethods array. The array is
     * ordered so that the more derived classes are first.
     * If both add and addConfigured are present, the addConfigured will take priority.
     * @param method the <code>Method</code> to insert.
     */
    private void insertAddTypeMethod(final Method method) {
        final Class<?> argClass = method.getParameterTypes()[0];
        final int size = addTypeMethods.size();
        for (int c = 0; c < size; ++c) {
            final Method current = addTypeMethods.get(c);
            if (current.getParameterTypes()[0].equals(argClass)) {
                if ("addConfigured".equals(method.getName())) {
                    // add configured replaces the add method
                    addTypeMethods.set(c, method);
                }
                return; // Already present
            }
            if (current.getParameterTypes()[0].isAssignableFrom(argClass)) {
                addTypeMethods.add(c, method);
                return; // higher derived
            }
        }
        addTypeMethods.add(method);
    }

    /**
     * Search the list of methods to find the first method
     * that has a parameter that accepts the nested element object.
     * @param paramClass the <code>Class</code> type to search for.
     * @param methods the <code>List</code> of methods to search.
     * @return a matching <code>Method</code>; null if none found.
     */
    private Method findMatchingMethod(final Class<?> paramClass, final List<Method> methods) {
        if (paramClass == null) {
            return null;
        }
        Class<?> matchedClass = null;
        Method matchedMethod = null;

        for (final Method method : methods) {
            final Class<?> methodClass = method.getParameterTypes()[0];
            if (methodClass.isAssignableFrom(paramClass)) {
                if (matchedClass == null) {
                    matchedClass = methodClass;
                    matchedMethod = method;
                } else if (!methodClass.isAssignableFrom(matchedClass)) {
                    throw new BuildException("ambiguous: types " + matchedClass.getName() + " and "
                            + methodClass.getName() + " match " + paramClass.getName());
                }
            }
        }
        return matchedMethod;
    }

    private String condenseText(final String text) {
        if (text.length() <= MAX_REPORT_NESTED_TEXT) {
            return text;
        }
        final int ends = (MAX_REPORT_NESTED_TEXT - ELLIPSIS.length()) / 2;
        return new StringBuffer(text).replace(ends, text.length() - ends, ELLIPSIS).toString();
    }


    private static class MethodAndObject {
        private final Method method;
        private final Object object;
        public MethodAndObject(final Method method, final Object object) {
            this.method = method;
            this.object = object;
        }
    }

    /**
     *
     */
    private AntTypeDefinition findRestrictedDefinition(
        final ComponentHelper helper, final String componentName, final List<Method> methods) {
        AntTypeDefinition definition = null;
        Class<?> matchedDefinitionClass = null;

        final List<AntTypeDefinition> definitions = helper.getRestrictedDefinitions(componentName);
        if (definitions == null) {
            return null;
        }
        synchronized (definitions) {
            for (final AntTypeDefinition d : definitions) {
                final Class<?> exposedClass = d.getExposedClass(helper.getProject());
                if (exposedClass == null) {
                    continue;
                }
                final Method method = findMatchingMethod(exposedClass, methods);
                if (method == null) {
                    continue;
                }
                if (matchedDefinitionClass != null) {
                    throw new BuildException(
                        "ambiguous: restricted definitions for "
                        + componentName + " "
                        + matchedDefinitionClass + " and " + exposedClass);
                }
                matchedDefinitionClass = exposedClass;
                definition = d;
            }
        }
        return definition;
    }

    private MethodAndObject createRestricted(
        final ComponentHelper helper, final String elementName, final List<Method> addTypeMethods) {

        final Project project = helper.getProject();

        final AntTypeDefinition restrictedDefinition =
            findRestrictedDefinition(helper, elementName, addTypeMethods);

        if (restrictedDefinition == null) {
            return null;
        }

        final Method addMethod = findMatchingMethod(
            restrictedDefinition.getExposedClass(project), addTypeMethods);
        if (addMethod == null) {
            throw new BuildException("Ant Internal Error - contract mismatch for "
                    + elementName);
        }
        final Object addedObject = restrictedDefinition.create(project);
        if (addedObject == null) {
            throw new BuildException(
                "Failed to create object " + elementName
                + " of type " + restrictedDefinition.getTypeClass(project));
        }
        return new MethodAndObject(addMethod, addedObject);
    }

    private MethodAndObject createTopLevel(
        final ComponentHelper helper, final String elementName, final List<Method> methods) {
        final Class<?> clazz = helper.getComponentClass(elementName);
        if (clazz == null) {
            return null;
        }
        final Method addMethod = findMatchingMethod(clazz, addTypeMethods);
        if (addMethod == null) {
            return null;
        }
        final Object addedObject = helper.createComponent(elementName);
        return new MethodAndObject(addMethod, addedObject);
    }

}
