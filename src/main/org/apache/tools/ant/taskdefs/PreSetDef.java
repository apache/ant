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
 * @since Ant 1.6
 */
public class PreSetDef extends AntlibDefinition implements TaskContainer {
    private UnknownElement nestedTask;
    private String         name;

    /**
     * Set the name of this definition.
     * @param name the name of the definition.
     */
     public void setName(String name) {
        this.name = name;
    }

    /**
     * Add a nested task to predefine attributes and elements on.
     * @param nestedTask  Nested task/type to extend.
     */
    @Override
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
     * Make a new definition.
     */
    @Override
    public void execute() {
        if (nestedTask == null) {
            throw new BuildException("Missing nested element");
        }
        if (name == null) {
            throw new BuildException("Name not specified");
        }
        name = ProjectHelper.genComponentName(getURI(), name);

        ComponentHelper helper = ComponentHelper.getComponentHelper(
            getProject());

        String componentName = ProjectHelper.genComponentName(
            nestedTask.getNamespace(), nestedTask.getTag());

        AntTypeDefinition def = helper.getDefinition(componentName);
        if (def == null) {
            throw new BuildException(
                "Unable to find typedef %s", componentName);
        }
        PreSetDefinition newDef = new PreSetDefinition(def, nestedTask);

        newDef.setName(name);

        helper.addDataTypeDefinition(newDef);
        log("defining preset " + name, Project.MSG_VERBOSE);
    }

    /**
     * This class contains the unknown element and the object
     * that is predefined.
     * @see AntTypeDefinition
     */
    public static class PreSetDefinition extends AntTypeDefinition {
        private AntTypeDefinition parent;
        private UnknownElement    element;

        /**
         * Creates a new <code>PresetDefinition</code> instance.
         *
         * @param parent The parent of this predefinition.
         * @param el     The predefined attributes, nested elements and text.
         */
        public PreSetDefinition(AntTypeDefinition parent, UnknownElement el) {
            if (parent instanceof PreSetDefinition) {
                PreSetDefinition p = (PreSetDefinition) parent;
                el.applyPreSet(p.element);
                parent = p.parent;
            }
            this.parent = parent;
            this.element = el;
        }

        /**
         * Override so that it is not allowed.
         *
         * @param clazz a <code>Class</code> value.
         */
        @Override
        public void setClass(Class<?> clazz) {
            throw new BuildException("Not supported");
        }

        /**
         * Override so that it is not allowed.
         *
         * @param className a <code>String</code> value.
         */
        @Override
        public void setClassName(String className) {
            throw new BuildException("Not supported");
        }

        /**
         * Get the classname of the definition.
         * @return the name of the class of this definition.
         */
        @Override
        public String getClassName() {
            return parent.getClassName();
        }

        /**
         * Set the adapter class for this definition.
         * NOT Supported
         * @param adapterClass the adapterClass.
         */
        @Override
        public void setAdapterClass(Class<?> adapterClass) {
            throw new BuildException("Not supported");
        }

        /**
         * Set the assignable class for this definition.
         * NOT SUPPORTED
         * @param adaptToClass the assignable class.
         */
        @Override
        public void setAdaptToClass(Class<?> adaptToClass) {
            throw new BuildException("Not supported");
        }

        /**
         * Set the classloader to use to create an instance
         * of the definition.
         * NOT SUPPORTED
         * @param classLoader the classLoader.
         */
        @Override
        public void setClassLoader(ClassLoader classLoader) {
            throw new BuildException("Not supported");
        }

        /**
         * Get the classloader for this definition.
         * @return the classloader for this definition.
         */
        @Override
        public ClassLoader getClassLoader() {
            return parent.getClassLoader();
        }

        /**
         * Get the exposed class for this definition.
         * @param project the current project.
         * @return the exposed class.
         */
        @Override
        public Class<?> getExposedClass(Project project) {
            return parent.getExposedClass(project);
        }

        /**
         * Get the definition class.
         * @param project the current project.
         * @return the type of the definition.
         */
        @Override
        public Class<?> getTypeClass(Project project) {
            return parent.getTypeClass(project);
        }


        /**
         * Check if the attributes are correct.
         * @param project the current project.
         */
        @Override
        public void checkClass(Project project) {
            parent.checkClass(project);
        }

        /**
         * Create an instance of the definition. The instance may be wrapped
         * in a proxy class. This is a special version of create for
         * IntrospectionHelper and UnknownElement.
         * @param project the current project.
         * @return the created object.
         */
        public Object createObject(Project project) {
            return parent.create(project);
        }

        /**
         * Get the preset values.
         * @return the predefined attributes, elements and text as
         *         an UnknownElement.
         */
        public UnknownElement getPreSets() {
            return element;
        }

        /**
         * Fake create an object, used by IntrospectionHelper and UnknownElement
         * to see that this is a predefined object.
         *
         * @param project the current project.
         * @return this object.
         */
        @Override
        public Object create(Project project) {
            return this;
        }

        /**
         * Equality method for this definition.
         *
         * @param other another definition.
         * @param project the current project.
         * @return true if the definitions are the same.
         */
        @Override
        public boolean sameDefinition(AntTypeDefinition other, Project project) {
            return (other != null && other.getClass() == getClass() && parent != null
                && parent.sameDefinition(((PreSetDefinition) other).parent, project)
                && element.similar(((PreSetDefinition) other).element));
        }

        /**
         * Similar method for this definition.
         *
         * @param other another definition.
         * @param project the current project.
         * @return true if the definitions are similar.
         */
        @Override
        public boolean similarDefinition(
            AntTypeDefinition other, Project project) {
            return (other != null && other.getClass().getName().equals(
                getClass().getName()) && parent != null
                && parent.similarDefinition(((PreSetDefinition) other).parent, project)
                && element.similar(((PreSetDefinition) other).element));
        }
    }
}
