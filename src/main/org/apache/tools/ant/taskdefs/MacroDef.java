/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003-2004 The Apache Software Foundation.  All rights
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
import java.util.Locale;
import java.util.HashMap;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;

/**
 * Describe class <code>MacroDef</code> here.
 *
 * @author Peter Reilly
 * @since Ant 1.6
 */
public class MacroDef extends AntlibDefinition  {
    private NestedSequential nestedSequential;
    private String     name;
    private List       attributes = new ArrayList();
    private Map        elements   = new HashMap();
    private String     textName   = null;

    /**
     * Name of the definition
     * @param name the name of the definition
     */
     public void setName(String name) {
        this.name = name;
    }

    /**
     * Name of the text attribute.
     * @param textName the name of the attribute to use for the
     *                 text content of the macro.
     * @since ant 1.6.1
     */
    public void setTextName(String textName) {
        this.textName = textName;
    }

    /**
     * @return the name of the text content attribute
     * @since ant 1.6.1
     */

    public String getTextName() {
        return textName;
    }

    /**
     * This is the sequential nested element of the macrodef.
     *
     * @return a sequential element to be configured.
     */
    public NestedSequential createSequential() {
        if (this.nestedSequential != null) {
            throw new BuildException("Only one sequential allowed");
        }
        this.nestedSequential = new NestedSequential();
        return this.nestedSequential;
    }

    /**
     * The class corresponding to the sequential nested element.
     * This is a simple task container.
     */
    public static class NestedSequential implements TaskContainer {
        private List nested = new ArrayList();

        /**
         * Add a task or type to the container.
         *
         * @param task an unknown element.
         */
        public void addTask(Task task) {
            nested.add(task);
        }

        /**
         * @return the list of unknown elements
         */
        public List getNested() {
            return nested;
        }

        /**
         * A compare function to compare this with another
         * NestedSequential.
         * It calls similar on the nested unknown elements.
         *
         * @param other the nested sequential to compare with.
         * @return true if they are similar, false otherwise
         */
        public boolean similar(NestedSequential other) {
            if (nested.size() != other.nested.size()) {
                return false;
            }
            for (int i = 0; i < nested.size(); ++i) {
                UnknownElement me = (UnknownElement) nested.get(i);
                UnknownElement o = (UnknownElement) other.nested.get(i);
                if (!me.similar(o)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Convert the nested sequential to an unknown element
     * @return the nested sequential as an unknown element.
     */
    public UnknownElement getNestedTask() {
        UnknownElement ret = new UnknownElement("sequential");
        ret.setTaskName("sequential");
        ret.setNamespace("");
        ret.setQName("sequential");
        new RuntimeConfigurable(ret, "sequential");
        for (int i = 0; i < nestedSequential.getNested().size(); ++i) {
            UnknownElement e =
                (UnknownElement) nestedSequential.getNested().get(i);
            ret.addChild(e);
            ret.getWrapper().addChild(e.getWrapper());
        }
        return ret;
    }

    /**
     * @return the nested Attributes
     */
    public List getAttributes() {
        return attributes;
    }

    /**
     * @return the nested elements
     */
    public Map getElements() {
        return elements;
    }

    /**
     * Check if a character is a valid character for an element or
     * attribute name
     * @param c the character to check
     * @return true if the character is a letter or digit or '.' or '-'
     *         attribute name
     */
    public static boolean isValidNameCharacter(char c) {
        // ? is there an xml api for this ?
        return Character.isLetterOrDigit(c) || c == '.' || c == '-';
    }

    /**
     * Check if a string is a valid name for an element or
     * attribute
     * @param name the string to check
     * @return true if the name consists of valid name characters
     */
    private static boolean isValidName(String name) {
        if (name.length() == 0) {
            return false;
        }
        for (int i = 0; i < name.length(); ++i) {
            if (!isValidNameCharacter(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Add an attribute element.
     *
     * @param attribute an attribute nested element.
     */
    public void addConfiguredAttribute(Attribute attribute) {
        if (attribute.getName() == null) {
            throw new BuildException(
                "the attribute nested element needed a \"name\" attribute");
        }
        if (attribute.getName().equals(textName)) {
            throw new BuildException(
                "the attribute name \"" + attribute.getName()
                + "\" has already been used by the textname attribute");
        }
        for (int i = 0; i < attributes.size(); ++i) {
            if (((Attribute) attributes.get(i)).getName().equals(
                    attribute.getName())) {
                throw new BuildException(
                    "the attribute " + attribute.getName()
                    + " has already been specified");
            }
        }
        attributes.add(attribute);
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
        if (elements.get(element.getName()) != null) {
            throw new BuildException(
                "the element " + element.getName()
                + " has already been specified");
        }
        elements.put(element.getName(), element);
    }

    /**
     * Create a new ant type based on the embedded tasks and types.
     *
     */
    public void execute() {
        if (nestedSequential == null) {
            throw new BuildException("Missing sequential element");
        }
        if (name == null) {
            throw new BuildException("Name not specified");
        }

        name = ProjectHelper.genComponentName(getURI(), name);

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
    public static class Attribute {
        private String name;
        private String defaultValue;
        private String description;

        /**
         * The name of the attribute.
         *
         * @param name the name of the attribute
         */
        public void setName(String name) {
            if (!isValidName(name)) {
                throw new BuildException(
                    "Illegal name [" + name + "] for attribute");
            }
            this.name = name.toLowerCase(Locale.US);
        }

        /**
         * @return the name of the attribute
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

        /**
         * @param desc Description of the element.
         * @since ant 1.6.1
         */
        public void setDescription(String desc) {
            description = desc;
        }

        /**
         * @return the description of the element, or <code>null</code> if
         *         no description is available.
         * @since ant 1.6.1
         */
        public String getDescription() {
            return description;
        }

        /**
         * equality method
         *
         * @param obj an <code>Object</code> value
         * @return a <code>boolean</code> value
         */
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            Attribute other = (Attribute) obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (defaultValue == null) {
                if (other.defaultValue != null) {
                    return false;
                }
            } else if (!defaultValue.equals(other.defaultValue)) {
                return false;
            }
            return true;
        }

        /**
         * @return a hash code value for this object.
         */
        public int hashCode() {
            return objectHashCode(defaultValue) + objectHashCode(name);
        }
    }

    /**
     * A nested element for the MacroDef task.
     *
     */
    public static class TemplateElement {
        private String name;
        private boolean optional = false;
        private String description;

        /**
         * The name of the element.
         *
         * @param name the name of the element.
         */
        public void setName(String name) {
            if (!isValidName(name)) {
                throw new BuildException(
                    "Illegal name [" + name + "] for attribute");
            }
            this.name = name.toLowerCase(Locale.US);
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

        /**
         * @param desc Description of the element.
         * @since ant 1.6.1
         */
        public void setDescription(String desc) {
            description = desc;
        }

        /**
         * @return the description of the element, or <code>null</code> if
         *         no description is available.
         * @since ant 1.6.1
         */
        public String getDescription() {
            return description;
        }

        /**
         * equality method
         *
         * @param obj an <code>Object</code> value
         * @return a <code>boolean</code> value
         */
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            TemplateElement other = (TemplateElement) obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            return optional == other.optional;
        }

        /**
         * @return a hash code value for this object.
         */
        public int hashCode() {
            return objectHashCode(name) + (optional ? 1 : 0);
        }
    }

    /**
     * equality method for macrodef, ignores project and
     * runtime info.
     *
     * @param obj an <code>Object</code> value
     * @return a <code>boolean</code> value
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!obj.getClass().equals(getClass())) {
            return false;
        }
        MacroDef other = (MacroDef) obj;
        if (name == null) {
            return other.name == null;
        }
        if (!name.equals(other.name)) {
            return false;
        }
        if (textName == null) {
            if (other.textName != null) {
                return false;
            }
        } else {
            if (!textName.equals(other.textName)) {
                return false;
            }
        }
        if (getURI() == null || getURI().equals("")
            || getURI().equals(ProjectHelper.ANT_CORE_URI)) {
            if (!(other.getURI() == null || other.getURI().equals("")
                  || other.getURI().equals(ProjectHelper.ANT_CORE_URI))) {
                return false;
            }
        } else {
            if (!getURI().equals(other.getURI())) {
                return false;
            }
        }

        if (!nestedSequential.similar(other.nestedSequential)) {
            return false;
        }
        if (!attributes.equals(other.attributes)) {
            return false;
        }
        if (!elements.equals(other.elements)) {
            return false;
        }
        return true;
    }

    /**
     * @return a hash code value for this object.
     */
    public int hashCode() {
        return objectHashCode(name)
            + objectHashCode(getURI())
            + objectHashCode(nestedSequential)
            + objectHashCode(attributes)
            + objectHashCode(elements);
    }

    /**
     * extends AntTypeDefinition, on create
     * of the object, the template macro definition
     * is given.
     */
    private static class MyAntTypeDefinition extends AntTypeDefinition {
        private MacroDef    macroDef;

        /**
         * Creates a new <code>MyAntTypeDefinition</code> instance.
         *
         * @param macroDef a <code>MacroDef</code> value
         */
        public MyAntTypeDefinition(MacroDef macroDef) {
            this.macroDef = macroDef;
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
            ((MacroInstance) o).setMacroDef(macroDef);
            return o;
        }

        /**
         * Equality method for this definition
         *
         * @param other another definition
         * @param project the current project
         * @return true if the definitions are the same
         */
        public boolean sameDefinition(AntTypeDefinition other, Project project) {
            if (!super.sameDefinition(other, project)) {
                return false;
            }
            MyAntTypeDefinition otherDef = (MyAntTypeDefinition) other;
            return macroDef.equals(otherDef.macroDef);
        }

        /**
         * Similar method for this definition
         *
         * @param other another definition
         * @param project the current project
         * @return true if the definitions are the same
         */
        public boolean similarDefinition(
            AntTypeDefinition other, Project project) {
            if (!super.similarDefinition(other, project)) {
                return false;
            }
            MyAntTypeDefinition otherDef = (MyAntTypeDefinition) other;
            return macroDef.equals(otherDef.macroDef);
        }
    }

    private static int objectHashCode(Object o) {
        if (o == null) {
            return 0;
        } else {
            return o.hashCode();
        }
    }
}
