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

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;

/**
 * The preset definition task generates a new definition
 * based on a current definition with some attributes or
 * elements preset.
 * <pre>
 * &lt;presetdef name="my.javac"&gt;
 *   &lt;javac deprecation="${deprecation}" debug="${debug}"/&gt;
 * &lt;/presetdef&gt;
 * &lt;my.javac srcdir="src" destdir="classes"/&gt;
 * </pre>
 *
 * @author Peter Reilly
 * @since Ant 1.6
 */
public class PreSetDef extends Task implements AntlibInterface, TaskContainer {
    private UnknownElement nestedTask;
    private String         name;
    private String         componentName;
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
     * Add a nested task to predefine attributes and elements on
     * @param nestedTask  Nested task/type to extend
     */
    public void addTask(Task nestedTask) {
        if (this.nestedTask != null) {
            throw new BuildException("Only one nested element allowed");
        }
        if (!(nestedTask instanceof UnknownElement)) {
            throw new BuildException(
                "addTask called with a task that is not an unknown element");
        }
        this.nestedTask = (UnknownElement) nestedTask;
    }


    /**
     * make a new definition
     */
    public void execute() {
        if (nestedTask == null) {
            throw new BuildException("Missing nested element");
        }
        if (name == null) {
            throw new BuildException("Name not specified");
        }

        name = ProjectHelper.genComponentName(uri, name);

        ComponentHelper helper = ComponentHelper.getComponentHelper(
            getProject());

        String componentName = ProjectHelper.genComponentName(
            nestedTask.getNamespace(), nestedTask.getTag());

        AntTypeDefinition def = helper.getDefinition(componentName);

        if (def == null) {
            throw new BuildException(
                "Unable to find typedef " + componentName);
        }

        MyAntTypeDefinition newDef = new MyAntTypeDefinition(def, nestedTask);

        newDef.setName(name);

        helper.addDataTypeDefinition(newDef);
    }

    private static class MyAntTypeDefinition extends AntTypeDefinition {
        AntTypeDefinition parent;
        UnknownElement    element;

        public MyAntTypeDefinition(AntTypeDefinition parent, UnknownElement el) {
            this.parent = parent;
            this.element = el;
        }

        public void setClass(Class clazz) {
            throw new BuildException("Not supported");
        }

        public void setClassName(String className) {
            throw new BuildException("Not supported");
        }

        /**
         * get the classname of the definition
         * @return the name of the class of this definition
         */
        public String getClassName() {
            return parent.getClassName();
        }

        /**
         * set the adapter class for this definition.
         * NOTE Supported
         * @param adapterClass the adapterClass
         */
        public void setAdapterClass(Class adapterClass) {
            throw new BuildException("Not supported");
        }

        /**
         * set the assignable class for this definition.
         * NOT SUPPORTED
         * @param adaptToClass the assignable class
         */

        public void setAdaptToClass(Class adaptToClass) {
            throw new BuildException("Not supported");
        }

        /**
         * set the classloader to use to create an instance
         * of the definition
         * @param classLoader the classLoader
         */
        public void setClassLoader(ClassLoader classLoader) {
            throw new BuildException("Not supported");
        }

        /**
         * get the classloader for this definition
         * @return the classloader for this definition
         */
        public ClassLoader getClassLoader() {
            return parent.getClassLoader();
        }

        /**
         * get the exposed class for this definition.
         * @return the exposed class
         */
        public Class getExposedClass(Project project) {
            return parent.getExposedClass(project);
        }

        /**
         * get the definition class
         * @param project the current project
         * @return the type of the definition
         */
        public Class getTypeClass(Project project) {
            return parent.getTypeClass(project);
        }


        /**
         * check if the attributes are correct
         * @param project the current project
         */
        public void checkClass(Project project) {
            parent.checkClass(project);
        }

        /**
         * create an instance of the definition.
         * The instance may be wrapped in a proxy class.
         * @param project the current project
         * @return the created object
         */
        public Object create(Project project) {
            Object o = parent.create(project);
            if (o == null) {
                return null;
            }
            element.configure(o);
            return o;
        }
    }
}
