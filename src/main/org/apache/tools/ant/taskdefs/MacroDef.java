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

package org.apache.tools.ant.taskdefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;

/**
 * Describe class <code>MacroDef</code> here.
 *
 * @author Peter Reilly
 * @since Ant 1.6
 */
public class MacroDef extends Task implements AntlibInterface, TaskContainer {
    private UnknownElement nestedTask;
    private String     name;
    private String     componentName;
    private List       params = new ArrayList();
    private Map        elements = new HashMap();
    private String         uri;

    /**
     * Name of the definition
     * @param name the name of the definition
     */
     public void setName(String name) {
        this.name = name;
    }

    /**
     * The URI for this definition.
     * @param uri the namespace URI
     * @throws BuildException if uri is not allowed
     */
    public void setURI(String uri) throws BuildException {
        if (uri.equals(ProjectHelper.ANT_CORE_URI)) {
            uri = "";
        }
        if (uri.startsWith("ant:") && !uri.startsWith("antlib:")) {
            throw new BuildException("Attempt to use a reserved URI " + uri);
        }
        this.uri = uri;
    }

    /**
     * Set the class loader.
     * Not used
     * @param classLoader a <code>ClassLoader</code> value
     */
    public void setAntlibClassLoader(ClassLoader classLoader) {
        // Ignore
    }

    /**
     * Add a nested task to ExtendType
     * @param nestedTask  Nested task/type to extend
     */
    public void addTask(Task nestedTask) {
        if (this.nestedTask != null) {
            throw new BuildException("Only one sequential/Parallel allowed");
        }
        UnknownElement ue = (UnknownElement) nestedTask;
        if (!ue.getNamespace().equals("")
            || (!ue.getTag().equals("sequential")
                && !ue.getTag().equals("parallel"))) {
            throw new BuildException("Unsupported tag " + ue.getQName());
        }
        this.nestedTask = ue;
    }

    /**
     * @return the nested task
     */
    public UnknownElement getNestedTask() {
        return nestedTask;
    }

    /**
     * @return the nested Params
     */
    public List getParams() {
        return params;
    }

    /**
     * @return the nested elements
     */
    public Map getElements() {
        return elements;
    }

    /**
     * Add a param element.
     *
     * @param param a param nested element.
     */
    public void addConfiguredParam(Param param) {
        if (param.getName() == null) {
            throw new BuildException(
                "the param nested element needed a \"name\" attribute");
        }
        params.add(param);
    }

    /**
     * Add an element element.
     *
     * @param element an element nested element.
     */
    public void addConfiguredElement(TemplateElement element) {
        if (element.getName() == null) {
            throw new BuildException(
                "the element nested element needed a \"name\" attribute");
        }
        elements.put(element.getName(), element);
    }

    /**
     * Create a new ant type based on the embedded tasks and types.
     *
     */
    public void execute() {
        if (nestedTask == null) {
            throw new BuildException("Missing nested element");
        }
        if (name == null) {
            throw new BuildException("Name not specified");
        }

        name = ProjectHelper.genComponentName(uri, name);

        MyAntTypeDefinition def = new MyAntTypeDefinition(this);
        def.setName(name);
        def.setClass(MacroInstance.class);

        ComponentHelper helper = ComponentHelper.getComponentHelper(
            getProject());

        helper.addDataTypeDefinition(def);
    }


    /**
     * A nested element for the MacroDef task.
     *
     */
    public static class Param {
        private String name;
        private String defaultValue;
        /**
         * The name of the parameter.
         *
         * @param name the name of the parameter
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the name of the parameter.
         */
        public String getName() {
            return name;
        }

        /**
         * The default value to use if the parameter is not
         * used in the templated instance.
         *
         * @param defaultValue the default value
         */
        public void setDefault(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        /**
         * @return the default value, null if not set
         */
        public String getDefault() {
            return defaultValue;
        }
    }

    /**
     * A nested element for the MacroDef task.
     *
     */
    public static class TemplateElement {
        private String name;
        private boolean optional = false;
        /**
         * The name of the element.
         *
         * @param name the name of the element.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the name of the element.
         */
        public String getName() {
            return name;
        }

        /**
         * is this element optional ?
         *
         * @param optional if true this element may be left out, default
         *                 is false.
         */
        public void setOptional(boolean optional) {
            this.optional = optional;
        }

        /**
         * @return the optional attribute
         */
        public boolean isOptional() {
            return optional;
        }
    }

    /**
     * extends AntTypeDefinition, on create
     * of the object, the template macro definition
     * is given.
     */
    private static class MyAntTypeDefinition extends AntTypeDefinition {
        private MacroDef    template;

        /**
         * Creates a new <code>MyAntTypeDefinition</code> instance.
         *
         * @param template a <code>MacroDef</code> value
         */
        public MyAntTypeDefinition(MacroDef template) {
            this.template = template;
        }

        /**
         * create an instance of the definition.
         * The instance may be wrapped in a proxy class.
         * @param project the current project
         * @return the created object
         */
        public Object create(Project project) {
            Object o = super.create(project);
            if (o == null) {
                return null;
            }
            ((MacroInstance) o).setTemplate(template);
            return o;
        }
    }
}
