/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.ant.xdoclet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.types.EnumeratedAttribute;
import xjavadoc.TagIterator;
import xjavadoc.XClass;
import xjavadoc.XCollections;
import xjavadoc.XMethod;
import xjavadoc.XParameter;
import xjavadoc.XTag;
import xdoclet.XDocletException;
import xdoclet.XDocletTagSupport;
import xdoclet.tagshandler.AbstractProgramElementTagsHandler;
import xdoclet.tagshandler.MethodTagsHandler;

/**
 * Custom tag handler for XDoclet templates for Ant-specific processing.
 *
 * @author               Erik Hatcher
 * @author               Jesse Stockall
 * @created              January 1, 2003
 * @xdoclet.taghandler   namespace="Ant"
 * @todo                 clean up logic so that all setters are gathered first (even superclass) and sorted along wih
 *      them
 * @todo                 need to create better logic for finding proper setters
 * @todo                 add ifIsAntTask, among other convenience tags
 */
public class TaskTagsHandler extends XDocletTagSupport
{

    /**
     * Default category for tasks without a category attribute.
     */
    public final static String DEFAULT_CATEGORY = "other";

    /**
     * Default requirement group for attributes without group.
     */
    public final static String DEFAULT_GROUP = "optional";

    /**
     * Requirement group description for optional attributes.
     */
    public final static String DESC_OPTIONAL = "optional";

    /**
     * Requirement group description for required attributes.
     */
    public final static String DESC_REQUIRED = "required";

    private static Map attributeDisplayMap = new HashMap();
    private static Map elementDisplayMap = new HashMap();

    private static String[] fluffPrefixes = {"set a", "set the", "sets a", "sets the"};

    static {
        attributeDisplayMap.put("java.lang.String", "String");
        attributeDisplayMap.put("boolean", "boolean");
        attributeDisplayMap.put("org.apache.tools.ant.types.Path", "Path");
        attributeDisplayMap.put("org.apache.tools.ant.types.Reference", "Reference");
        attributeDisplayMap.put("java.io.File", "File");
        attributeDisplayMap.put("java.util.Date", "Date");
        attributeDisplayMap.put("java.net.URL", "URL");
        attributeDisplayMap.put("java.lang.Long", "long");
        attributeDisplayMap.put("java.lang.Integer", "int");
        attributeDisplayMap.put("java.lang.Float", "float");
        attributeDisplayMap.put("java.lang.Double", "double");

        elementDisplayMap.put("org.apache.tools.ant.types.Path", "Path");
        elementDisplayMap.put("org.apache.tools.ant.types.FileSet", "Fileset");
        elementDisplayMap.put("org.apache.tools.ant.taskdefs.Property", "see &lt;property&gt;");
        elementDisplayMap.put("org.apache.tools.ant.types.Mapper", "Mapper");
        elementDisplayMap.put("org.apache.tools.ant.types.PatternSet", "Patternset");
        elementDisplayMap.put("org.apache.tools.ant.types.FileList", "Filelist");
        elementDisplayMap.put("org.apache.tools.ant.types.FilterChain", "FilterChain");
        elementDisplayMap.put("org.apache.tools.ant.types.FilterSet", "Filterset");
        elementDisplayMap.put("org.apache.tools.ant.types.ZipFileSet", "ZipFileset");
        elementDisplayMap.put("org.apache.tools.ant.types.DirSet", "Dirset");
        elementDisplayMap.put("org.apache.tools.ant.types.XMLCatalog", "XMLCatalog");
    }

    /**
     * Provides the Ant task name. Order of rules:
     * <ol>
     *   <li> Value of
     *
     * @param clazz
     * @return
     * @ant:task     name="..."</li>
     *      <li> Lowercased classname with "Task" suffix removed</li>
     *    </ol>
     *
     */
    public final static String getTaskName(XClass clazz)
    {
        String tagValue = clazz.getDoc().getTagAttributeValue("ant.task", "name");

        if (tagValue == null) {
            // use classname, but strip "Task" suffix if there
            tagValue = clazz.getName();

            if (tagValue.endsWith("Task")) {
                tagValue = tagValue.substring(0, tagValue.indexOf("Task"));
            }

            tagValue = tagValue.toLowerCase();
        }
        return tagValue;
    }

    /**
     * Provides the Ant category name as the Value of the category attribute.
     *
     * @param clazz
     * @return
     */
    public final static String getCategoryName(XClass clazz)
    {
        String tagValue = clazz.getDoc().getTagAttributeValue("ant.task", "category");

        if (tagValue != null) {
            tagValue = tagValue.toLowerCase();
        }
        else {
            tagValue = DEFAULT_CATEGORY;
        }
        return tagValue;
    }

    /**
     * Iterates over all Ant tasks
     *
     * @param template
     * @param attributes
     * @exception XDocletException
     */
    public void forAllTasks(String template, Properties attributes) throws XDocletException
    {
        Collection classes = AbstractProgramElementTagsHandler.getAllClasses();
        XClass cur_class = null;

        Iterator iter = classes.iterator();

        while (iter.hasNext()) {
            cur_class = (XClass) iter.next();
            setCurrentClass(cur_class);

            if (AntSubTask.isAntTask(cur_class)) {
                generate(template);
            }
        }
    }

//    /**
//     * Iterates over all Ant attributes.
//     *
//     * @param template              XDoclet template
//     * @param attributes            Tag parameters
//     * @exception XDocletException  Oops!
//     */
//    public void forAllAttributes(String template, Properties attributes) throws XDocletException
//    {
//        // throw exception if not an Ant task
//
//        XClass cur_class = getCurrentClass();
//
//        XMethod[] methods = getAttributeMethods(cur_class);
//
////        System.out.println("# attributes = " + methods.length);
//
//        for (int i = 0; i < methods.length; i++) {
//            setCurrentMethod(methods[i]);
//            generate(template);
//        }
//    }

    /**
     * Iterates over all Ant attributes.
     *
     * @param template              XDoclet template
     * @param attributes            Tag parameters
     * @exception XDocletException  Oops!
     */
    public void forAllAttributesInGroup(String template, Properties attributes) throws XDocletException
    {
        // throw exception if not an Ant task

        XClass cur_class = getCurrentClass();

        XMethod[] methods = getAttributeMethods(cur_class);

        String group = attributes.getProperty("group", DEFAULT_GROUP);

        for (int i = 0; i < methods.length; i++) {
            String value = methods[i].getDoc().getTagAttributeValue("ant.attribute", "group");

            if ((value != null && value.equals(group)) || (value == null && group.equals(DEFAULT_GROUP))) {
                setCurrentMethod(methods[i]);
                generate(template);
            }
        }
    }

    /**
     * Determines if there's at least one Ant attribute.
     *
     * @param template              XDoclet template
     * @param attributes            Tag parameters
     * @exception XDocletException  Oops!
     */
    public void ifHasAttributes(String template, Properties attributes) throws XDocletException
    {
        // throw exception if not an Ant task

        XClass cur_class = getCurrentClass();

        XMethod[] methods = getAttributeMethods(cur_class);

        if (methods.length > 0) {
            generate(template);
        }
    }

    /**
     * Iterates over all Ant nested element methods (addXXX, addConfiguredXXX, addXXX)
     *
     * @param template              XDoclet template
     * @param attributes            Tag parameters
     * @exception XDocletException  Oops!
     */
    public void forAllElements(String template, Properties attributes) throws XDocletException
    {
        // throw exception if not an Ant task

        XClass cur_class = getCurrentClass();

        XMethod[] methods = getElementMethods(cur_class);

        for (int i = 0; i < methods.length; i++) {
            setCurrentMethod(methods[i]);
            generate(template);
        }
    }

    /**
     * Iterates over all ant.attribute.group tags.
     *
     * @param template           XDoclet template
     * @param attributes         Tag parameters
     * @throws XDocletException  Oops!
     */
    public void forAllAttributeGroups(String template, Properties attributes) throws XDocletException
    {
        Collection tags = getCurrentClass().getDoc().getTags("ant.attribute.group");

        for (TagIterator t = XCollections.tagIterator(tags); t.hasNext(); ) {
            setCurrentClassTag(t.next());

            generate(template);
        }
    }

    /**
     * Provides the name of a requirement group.
     *
     * @return                   The description of the group, or 'optional' if not is defined
     * @throws XDocletException
     */
    public String attributeGroupName() throws XDocletException
    {
        XTag tag = getCurrentClassTag();
        String name = tag.getAttributeValue("name");

        return name != null ? name : DEFAULT_GROUP;
    }

    /**
     * Provides the description for a requirement group.
     *
     * @return                   The description of the group, or 'Optional' if not is defined
     * @throws XDocletException
     */
    public String attributeGroupDesc() throws XDocletException
    {
        XTag tag = getCurrentClassTag();
        String desc = tag.getAttributeValue("description");

        return desc != null ? desc : DESC_OPTIONAL;
    }

    /**
     * Provides the element name for the current method
     *
     * @return
     * @exception XDocletException
     */
    public String elementName() throws XDocletException
    {
        String methodName = getCurrentMethod().getName();
        String elementName = "<not a valid element>";

        if (methodName.startsWith("addConfigured")) {
            elementName = methodName.substring(13, methodName.length());
        }
        else if (methodName.startsWith("add")) {
            elementName = methodName.substring(3, methodName.length());
        }
        else if (methodName.startsWith("create")) {
            elementName = methodName.substring(6, methodName.length());
        }
        return elementName.toLowerCase();
    }

    public String displayAttributeType() throws XDocletException
    {
        Collection parameters = getCurrentMethod().getParameters();
        XParameter param = XCollections.parameterIterator(parameters).next();

        String methodType = param.getType().getQualifiedName();
        String display = (String) attributeDisplayMap.get(methodType);

        if (display == null) {

//            System.out.println("type = " + methodType);

            Class clazz = getAttributeClass(methodType);

            if (clazz == null) {
                return methodType;
            }

            Object instance = null;

            try {
                instance = clazz.newInstance();
            }
            catch (InstantiationException e) {
            }
            catch (IllegalAccessException e) {
            }

            if (instance != null && instance instanceof EnumeratedAttribute) {
                EnumeratedAttribute enum = (EnumeratedAttribute) instance;
                String[] values = enum.getValues();

                display = "";
                for (int i = 0; i < values.length; i++) {
                    display += "&quot;" + values[i] + "&quot;";
                    if (i != (values.length - 1)) {
                        display += ", ";
                    }
                }
                return display;
            }

            display = "";
        }
        return display;
    }

    public String displayElementType() throws XDocletException
    {
        String elementType = elementType();
        String display = (String) elementDisplayMap.get(elementType);

        if (display == null) {
            display = "";
        }
        return display;
    }

    /**
     * Provides the element type for the current method
     *
     * @return
     * @exception XDocletException
     */
    public String elementType() throws XDocletException
    {
        XClass clazz = elementClassDoc();

        if (clazz == null) {
            throw new XDocletException("Method is not an Ant element!");
        }
        return clazz.getQualifiedName();
    }

    /**
     * Provides the Ant task name.
     *
     * @return
     * @exception XDocletException
     * @see                         #getTaskName(xjavadoc.XClass)
     */
    public String taskName() throws XDocletException
    {
        return getTaskName(getCurrentClass());
    }

    public String propertyName()
    {
        return MethodTagsHandler.getPropertyNameFor(getCurrentMethod()).toLowerCase();
    }

    public String shortMethodDescription() throws XDocletException
    {
        String desc = getCurrentMethod().getDoc().getFirstSentence();

        if (desc == null || desc.length() == 0) {
            desc = "no description";
        }

        desc = desc.trim();

        String descLower = desc.toLowerCase();

        for (int i = 0; i < fluffPrefixes.length; i++) {
            String prefix = fluffPrefixes[i].toLowerCase() + " ";

            if (descLower.startsWith(prefix)) {
                desc = desc.substring(prefix.length());
                break;
            }
        }

        desc = desc.substring(0, 1).toUpperCase() + desc.substring(1);

        if (!desc.endsWith(".")) {
            desc += ".";
        }

        return desc;
    }

    /**
     * Provides the Ant category name.
     *
     * @return
     * @exception XDocletException
     * @see                         #getCategoryName(xjavadoc.XClass)
     */
    public String categoryName() throws XDocletException
    {
        return getCategoryName(getCurrentClass());
    }

    /**
     * Provides the requirment group for the current method
     *
     * @return                   The group listed in the source, or 'optional' of none is listed
     * @throws XDocletException  oops
     */
    public String attributeGroup() throws XDocletException
    {
        String value = getCurrentMethod().getDoc().getTagAttributeValue("ant.attribute", "group");

        return value != null ? value : DEFAULT_GROUP;
    }

    private Class getAttributeClass(String type) throws XDocletException
    {
//        System.out.println("type = " + type);

        Class clazz = null;

        try {
            clazz = Class.forName(type);
        }
        catch (ClassNotFoundException e) {
            int lastDotPosition = type.lastIndexOf('.');

            if (lastDotPosition < 0) {
                // probably a primitive
                return null;
            }
            type = type.substring(0, lastDotPosition) + "$" + type.substring(lastDotPosition + 1);
            try {
                clazz = Class.forName(type);
            }
            catch (ClassNotFoundException e1) {
                throw new XDocletException(e1.getMessage());
            }
        }
        return clazz;
    }


    /**
     * @param cur_class
     * @return
     * @exception XDocletException
     * @todo                        refactor to cache methods per class, and save some time
     */
    private XMethod[] getAttributeMethods(XClass cur_class) throws XDocletException
    {
        // Use Ant's own introspection mechanism to gather the
        // attributes this class supports
        IntrospectionHelper is = null;

        try {
            is = IntrospectionHelper.getHelper(Class.forName(cur_class.getQualifiedName()));
        }
        catch (ClassNotFoundException e) {
            throw new XDocletException(e, e.getMessage());
        }

        // Regroup the attributes, since IntrospectionHelper
        // doesn't give us the whole data structure directly
        Enumeration enum = is.getAttributes();
        Properties attributeTypeMap = new Properties();

        while (enum.hasMoreElements()) {
            String name = (String) enum.nextElement();
            Class type = is.getAttributeType(name);

            attributeTypeMap.setProperty(name, type.getName());
//            System.out.println(name + " = " + type.getName());
        }

        // We need to return XMethod[] from this method
        // so get all methods from the current class
        XMethod[] allMethods = getMethods(cur_class);

//        System.out.println("allMethods = " + allMethods.length);

        // And now filter the methods based
        // on what IntrospectionHelper says
        List attributeMethods = new ArrayList();

        for (int i = 0; i < allMethods.length; i++) {
            XMethod method = allMethods[i];
            String methodName = method.getName();

//            System.out.println("methodName = " + methodName);

            if (!methodName.startsWith("set")) {
                continue;
            }

            String attributeName = methodName.substring(3).toLowerCase();

//            System.out.println("attributeName = " + attributeName);

            if ((method.getParameters().size() != 1) || (!method.isPublic())) {
                continue;
            }

            String attributeType = XCollections.parameterIterator(method.getParameters()).next().getType().getQualifiedName();

//            System.out.println("attributeType = " + attributeType);

            String mapAttribute = attributeTypeMap.getProperty(attributeName);

            if (mapAttribute == null) {
                continue;
            }

            // inner classes are noted with $ in our map, but not
            // n the parameter type name.
            if (!attributeType.equals(mapAttribute.replace('$', '.'))) {
                continue;
            }

//            System.out.println(methodName + " : " + attributeName + " : " + attributeType);

            attributeMethods.add(method);
        }

        return (XMethod[]) attributeMethods.toArray(new XMethod[0]);
    }

    /**
     * @param cur_class
     * @return
     * @exception XDocletException
     * @todo                        add checks for number parameters and appropriate return value check for proper
     *      exception too? method prefixes: add, create, addConfigured (but not addText)
     * @todo                        add DynamicConfigurator (this should be noted in the template, not dealt with here)
     */
    private XMethod[] getElementMethods(XClass cur_class) throws XDocletException
    {
        // Use Ant's own introspection mechanism to gather the
        // elements this class supports
        IntrospectionHelper is = null;

        try {
            is = IntrospectionHelper.getHelper(Class.forName(cur_class.getQualifiedName()));
        }
        catch (ClassNotFoundException e) {
            throw new XDocletException(e.getMessage());
        }

        // Regroup the elements, since IntrospectionHelper
        // doesn't give us the whole data structure directly
        Enumeration enum = is.getNestedElements();
        Properties elementTypeMap = new Properties();

        while (enum.hasMoreElements()) {
            String name = (String) enum.nextElement();
            Class type = is.getElementType(name);

            elementTypeMap.setProperty(name, type.getName());
//            System.out.println(name + " = " + type.getName());
        }

        // We need to return MethodDoc[] from this method
        // so get all methods from the current class
        XMethod[] allMethods = getMethods(cur_class);

        // And now filter the MethodDoc's based
        // on what IntrospectionHelper says
        List elementMethods = new ArrayList();

        for (int i = 0; i < allMethods.length; i++) {
            XMethod method = allMethods[i];
            String methodName = method.getName();

            // Object create(), void add(Object), void addConfigured(Object)
            String elementName = null;

            // true if addXXX or addConfiguredXXX
            boolean adder = false;

            if (methodName.startsWith("create")) {
                elementName = methodName.substring(6).toLowerCase();
            }

            if (methodName.startsWith("add")) {
                int length = 3;

                if (methodName.startsWith("addConfigured")) {
                    length = 13;
                }

                elementName = methodName.substring(length).toLowerCase();
                adder = true;
            }

            if (elementName == null) {
                continue;
            }

//            System.out.println("elementName = " + elementName);

            String elementType = null;

            if (adder) {
                if (method.getParameters().size() != 1) {
                    continue;
                }
                elementType = XCollections.parameterIterator(method.getParameters()).next().getType().getQualifiedName();
            }
            else {
                elementType = method.getReturnType().getType().getQualifiedName();
            }

            if (!method.isPublic()) {
                continue;
            }

            String mapElementType = elementTypeMap.getProperty(elementName);

//            System.out.println("elementType = " + elementType + " mapElementType = " + mapElementType);
            if (mapElementType == null) {
                continue;
            }

            // inner classes are noted with $ in our map, but not
            // the parameter type name.
            if (!elementType.equals(mapElementType.replace('$', '.'))) {
                continue;
            }

            elementMethods.add(method);
        }

        return (XMethod[]) elementMethods.toArray(new XMethod[0]);
    }

    /**
     * This is a slightly refactored (thank you IntelliJ) version of some cut-and-paste from XDoclet code. It sorts all
     * methods together rather than in batches of superclasses like XDoclet stuff does.
     *
     * @param cur_class
     * @return
     * @exception XDocletException
     */
    private XMethod[] getMethods(XClass cur_class) throws XDocletException
    {
        Map already = new HashMap();

        List methods = new ArrayList();

        while (cur_class != null) {
            // hardcoded to stop when it hits Task, nothing there
            // or above that needs to be processed
            if (cur_class.getQualifiedName().equals("org.apache.tools.ant.Task") ||
                cur_class.getQualifiedName().equals("org.apache.tools.ant.taskdefs.MatchingTask")) {
                break;
            }

            Collection curMethods = cur_class.getMethods();

            Iterator iter = curMethods.iterator();

            while (iter.hasNext()) {
                XMethod method = (XMethod) iter.next();

                if (isDeprecated(method)) {
                    continue;
                }
                if (shouldIgnore(method)) {
                    continue;
                }

                String methodName = method.getName();

//                System.out.println("method = " + method + ":" + methodName);

                if (method.getContainingClass() == cur_class) {
                    if (already.containsKey(methodName) == false) {
                        already.put(methodName, method);
                        methods.add(method);
                    }
                }
            }

            cur_class = cur_class.getSuperclass();
        }

        return sortMethods(methods);
    }

    private boolean isDeprecated(XMethod method)
    {
        Collection tags = method.getDoc().getTags();
        Iterator iter = tags.iterator();

        while (iter.hasNext()) {
            XTag tag = (XTag) iter.next();

            if (tag.getName().equals("@deprecated")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Provides the element type for the current method. If the return type is null, the first parameter is used.
     *
     * @return
     */
    private XClass elementClassDoc()
    {
        XClass clazz = null;
        String methodName = getCurrentMethod().getName();

        if (methodName.startsWith("addConfigured") ||
            methodName.startsWith("add") ||
            methodName.startsWith("create")) {
            clazz = getCurrentMethod().getReturnType().getType();
            if ("void".equals(clazz.getName())) {
                Collection params = getCurrentMethod().getParameters();

                if (params.size() == 1) {
                    clazz = XCollections.parameterIterator(params).next().getType();
                }
            }
        }
//        System.out.println(methodName + ": clazz = " + clazz.getQualifiedName());
        return clazz;
    }

    /**
     * For now, lump attributes and elements together since we won't have those tags on the same method.
     *
     * @param method
     * @return        True if the method should be ignored.
     */
    private boolean shouldIgnore(XMethod method)
    {
        String value = method.getDoc().getTagAttributeValue("ant.attribute", "ignore");

        if ("true".equals(value)) {
            return true;
        }

        value = method.getDoc().getTagAttributeValue("ant.element", "ignore");
        if ("true".equals(value)) {
            return true;
        }
        return false;
    }

    private XMethod[] sortMethods(List methods)
    {
        //sort methods
        Collections.sort(methods,
            new Comparator()
            {
                public int compare(Object o1, Object o2)
                {
                    XMethod m1 = (XMethod) o1;
                    XMethod m2 = (XMethod) o2;

                    return m1.getName().compareTo(m2.getName());
                }


                public boolean equals(Object obj)
                {
                    //dumb
                    return obj == this;
                }
            });

        return (XMethod[]) methods.toArray(new XMethod[0]);
    }
}

