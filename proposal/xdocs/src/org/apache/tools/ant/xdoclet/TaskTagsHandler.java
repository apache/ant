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
package org.apache.tools.ant.xdoclet;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;

import xdoclet.XDocletException;
import xdoclet.XDocletTagSupport;
import xdoclet.tags.AbstractProgramElementTagsHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Custom tag handler for XDoclet templates for Ant-specific processing.
 *
 * @author     Erik Hatcher
 * @created    February 17, 2002
 *
 * @todo clean up logic so that all setters are gathered first (even
 * superclass) and sorted along wih them
 * @todo need to create better logic for finding proper setters
 * @todo add ifIsAntTask, among other convenience tags
 */
public class TaskTagsHandler extends XDocletTagSupport {

    /** Default category for tasks without a category attribute. */
    public static final String DEFAULT_CATEGORY = "other";

    /**
     * Iterates over all Ant tasks
     */
    public void forAllTasks(String template, Properties attributes) throws XDocletException {
        ClassDoc[] classes = AbstractProgramElementTagsHandler.getAllClasses();
        ClassDoc cur_class = null;

        for (int i = 0; i < classes.length; i++) {
            cur_class = classes[i];
            setCurrentClass(cur_class);

            if (TaskSubTask.isAntTask(cur_class)) {
                generate(template);
            }
        }
    }

    /**
     * Iterates over all Ant attributes.
     *
     *@param  template              XDoclet template
     *@param  attributes            Tag parameters
     *@exception  XDocletException  Oops!
     */
    public void forAllAttributes(String template, Properties attributes) throws XDocletException {
        // throw exception if not an Ant task

        ClassDoc cur_class = getCurrentClass();

        MethodDoc[] methods = getAttributeMethods(cur_class);

        for (int i = 0; i < methods.length; i++) {
            setCurrentMethod(methods[i]);
            generate(template);
        }
    }

    /**
     * Determines if there's at least one Ant attribute.
     *
     *@param  template              XDoclet template
     *@param  attributes            Tag parameters
     *@exception  XDocletException  Oops!
     */
    public void ifHasAttributes(String template, Properties attributes) throws XDocletException {
        // throw exception if not an Ant task

        ClassDoc cur_class = getCurrentClass();

        MethodDoc[] methods = getAttributeMethods(cur_class);

        if (methods.length > 0) {
            generate(template);
        }
    }

    /**
     *  Iterates over all Ant nested element methods (addXXX, addConfiguredXXX, addXXX)
     *
     *@param  template              XDoclet template
     *@param  attributes            Tag parameters
     *@exception  XDocletException  Oops!
     */
    public void forAllElements(String template, Properties attributes) throws XDocletException {
        // throw exception if not an Ant task

        ClassDoc cur_class = getCurrentClass();

        MethodDoc[] methods = getElementMethods(cur_class);

        for (int i = 0; i < methods.length; i++) {
            setCurrentMethod(methods[i]);
            generate(template);
        }
    }


    /**
     * Provides the element name for the current method
     */
    public String elementName() throws XDocletException {
        String methodName = getCurrentMethod().name();
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

    /**
     * Provides the element type for the current method
     */
    public String elementType() throws XDocletException {
        ClassDoc classDoc = elementClassDoc();
        if (classDoc == null) {
            throw new XDocletException("Method is not an Ant element!");
        }
        return classDoc.qualifiedName();
    }

    /**
     * Provides the element type for the current method.  If the return type
     * is null, the first parameter is used.
     */
    private ClassDoc elementClassDoc() throws XDocletException {
        ClassDoc classDoc = null;
        String methodName = getCurrentMethod().name();
        if (methodName.startsWith("addConfigured") ||
            methodName.startsWith("add") ||
            methodName.startsWith("create"))
        {
            classDoc = getCurrentMethod().returnType().asClassDoc();
            if (classDoc == null) {
                Parameter[] params = getCurrentMethod().parameters();
                if (params.length == 1) {
                    classDoc = params[0].type().asClassDoc();
                }
            }
        }
        return classDoc;
    }

    /**
     * Provides the Ant task name.
     *
     * @see #getTaskName(ClassDoc)
	 * @doc:tag      type="content"
     */
    public String taskName() throws XDocletException {
        return getTaskName(getCurrentClass());
    }

    /**
     * Provides the Ant task name.
     *
     * Order of rules:
     * <ol>
     *   <li>Value of @ant:task name="..."</li>
     *   <li>Lowercased classname with "Task" suffix removed</li>
     * </ol>
     */
    public static final String getTaskName(ClassDoc clazz) throws XDocletException {
        // sheesh!  There should be a friendlier method than this!
        String tagValue = getTagValue(clazz, "ant:task", "name", -1,
                null, null, null, null,
                null, false, XDocletTagSupport.FOR_CLASS, false);

        if (tagValue == null) {
            // use classname, but strip "Task" suffix if there
            tagValue = clazz.name();

            if (tagValue.endsWith("Task")) {
                tagValue = tagValue.substring(0, tagValue.indexOf("Task"));
            }

            tagValue = tagValue.toLowerCase();
        }
        return tagValue;
    }

    /**
     * Provides the Ant category name.
     *
     * @see #getCategoryName(ClassDoc)
     */
    public String categoryName() throws XDocletException {
        return getCategoryName(getCurrentClass());
    }

    /**
     * Provides the Ant category name as the Value of the category attribute,
     * <code>@ant:task&nbsp;category="..."</code>.
     */
    public static final String getCategoryName(ClassDoc clazz) throws XDocletException {
        String tagValue = getTagValue(clazz, "ant:task", "category", -1,
                null, null, null, null,
                null, false, XDocletTagSupport.FOR_CLASS, false);
        if (tagValue != null) {
            tagValue = tagValue.toLowerCase();
        }
        else {
            tagValue = DEFAULT_CATEGORY;
        }
        return tagValue;
    }


    /**
     * Returns true if the method corresponds to an Ant task attribute using
     * the rules from IntrospectionHelper
     *
     * @todo filter out deprecated methods
     *       only filter org.apache.tools.ant.Task setters that are hidden
     *       check that it returns void and only has single argument
     *       incorporate rules for argument types from IntrospectionHelper
     *           - i.e. not an array
     *           - primitives/wrappers
     *           - File
     *           - Path
     *           - EnumeratedAttribute
     *           - Class with string constructor
     */
    private boolean isAntAttribute(MethodDoc method) {
        String[] excludeList = new String[]{"setLocation", "setDescription", "setOwningTarget", "setRuntimeConfigurableWrapper",
                                            "setTaskName", "setTaskType", "setProject"};
        for (int i = 0; i < excludeList.length; i++) {
            if (excludeList[i].equals(method.name())) {
                return true;
            }
        }

        return false;
    }

    private MethodDoc[] getAttributeMethods(ClassDoc cur_class) throws XDocletException {
        MethodDoc[] methods = getMethods(cur_class);
        List attributeMethods = new ArrayList();
        Map nameTypeMap = new HashMap();

        for (int i = 0; i < methods.length; i++) {
            MethodDoc method = methods[i];

            if (!method.isPublic()) {
                continue;
            }

            if (!method.name().startsWith("set")) {
                continue;
            }

            // if superclass is org.apache.tools.ant.Task then
            // remove some known unallowed properties
            if (isAntAttribute(method)) {
                continue;
            }

            // ensure method only has one parameter
            Parameter[] params = method.parameters();
            if (params.length != 1) {
                continue;
            }

            Parameter param = params[0];

            // Screen out attribute setters if there are duplicates,
            // and only return the first non-String one
            // (this may or may not jive with IntrospectionHelper)
            MethodDoc oldMethod = (MethodDoc) nameTypeMap.get(method.name());
            if (oldMethod == null) {
                nameTypeMap.put(method.name(), method);
            }
            else {
                if ("java.lang.String".equals(oldMethod.parameters()[0].typeName())) {
                    attributeMethods.remove(oldMethod);
                    nameTypeMap.put(method.name(), method);
                }
            }

            attributeMethods.add(method);
        }

        return (MethodDoc[]) attributeMethods.toArray(new MethodDoc[0]);
    }

    /**
     * @todo add checks for number parameters and appropriate return value
     *       check for proper exception too?
     *       method prefixes: add, create, addConfigured (but not addText)
     */
    private MethodDoc[] getElementMethods(ClassDoc cur_class) throws XDocletException {
        MethodDoc[] methods = getMethods(cur_class);
        List attributeMethods = new ArrayList();

        for (int i = 0; i < methods.length; i++) {
            if (! methods[i].isPublic()) {
                continue;
            }

            String name = methods[i].name();

            // ensure if there are no parameters, there is a return type,
            // otherwise ensure there's only one parameter.
            Parameter[] params = methods[i].parameters();
            if (params.length == 0) {
                if (methods[i].returnType().asClassDoc() == null) {
                    continue;
                }

                // only the "createXXX" method has zero params
                // the "addXXX" and "addConfiguredXXX" have 1 param
                if (!name.startsWith("create")) {
                    continue;
                }
            }
            else if (params.length != 1) {
                continue;
            }

            if ((name.startsWith("add") && !name.equals("addTask")) ||
                    name.startsWith("create")) {
                attributeMethods.add(methods[i]);
            }

        }

        return (MethodDoc[]) attributeMethods.toArray(new MethodDoc[0]);
    }


    /**
     * This is a slightly refactored (thank you IntelliJ) version of
     * some cut-and-paste from XDoclet code.  It sorts all methods together
     * rather than in batches of superclasses like XDoclet stuff does.
     */
    private MethodDoc[] getMethods(ClassDoc cur_class) throws XDocletException {
        Map already = new HashMap();

        List methods = new ArrayList();

        while (cur_class != null) {
            List curMethods = Arrays.asList(cur_class.methods());

            for (int j = 0; j < curMethods.size(); j++) {
                MethodDoc method = (MethodDoc) curMethods.get(j);
                if (method.containingClass() == cur_class) {
                    if (already.containsKey(method) == false) {
                        already.put(method, method);
                        methods.add(method);
                    }
                }
            }

            cur_class = cur_class.superclass();
        }

        return sortMethods(methods);
    }

    private MethodDoc[] sortMethods(List methods) {
        //sort methods
        Collections.sort(methods,
                new Comparator() {
                    public int compare(Object o1, Object o2) {
                        MethodDoc m1 = (MethodDoc) o1;
                        MethodDoc m2 = (MethodDoc) o2;

                        return m1.name().compareTo(m2.name());
                    }


                    public boolean equals(Object obj) {
                        //dumb
                        return obj == this;
                    }
                });

        return (MethodDoc[]) methods.toArray(new MethodDoc[0]);
    }


}

