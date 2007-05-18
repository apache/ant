/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.HashMap;
import java.util.Iterator;

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
 * @since Ant 1.6
 */
public class MacroDef extends AntlibDefinition  {

    private NestedSequential nestedSequential;
    private String     name;
    private boolean    backTrace = true;
    private List       attributes = new ArrayList();
    private Map        elements   = new HashMap();
    private String     textName   = null;
    private Text       text       = null;
    private boolean    hasImplicitElement = false;

    /**
     * Name of the definition
     * @param name the name of the definition
     */
     public void setName(String name) {
        this.name = name;
    }

    /**
     * Add the text element.
     * @param text the nested text element to add
     * @since ant 1.6.1
     */
    public void addConfiguredText(Text text) {
        if (this.text != null) {
            throw new BuildException(
                "Only one nested text element allowed");
        }
        if (text.getName() == null) {
            throw new BuildException(
                "the text nested element needed a \"name\" attribute");
        }
        // Check if used by attributes
        for (Iterator i = attributes.iterator(); i.hasNext();) {
            Attribute attribute = (Attribute) i.next();
            if (text.getName().equals(attribute.getName())) {
                throw new BuildException(
                    "the name \"" + text.getName()
                    + "\" is already used as an attribute");
            }
        }
        this.text = text;
        this.textName = text.getName();
    }

    /**
     * @return the nested text element
     * @since ant 1.6.1
     */
    public Text getText() {
        return text;
    }

    /**
     * Set the backTrace attribute.
     *
     * @param backTrace if true and the macro instance generates
     *                  an error, a backtrace of the location within
     *                  the macro and call to the macro will be output.
     *                  if false, only the location of the call to the
     *                  macro will be shown. Default is true.
     * @since ant 1.7
     */
    public void setBackTrace(boolean backTrace) {
        this.backTrace = backTrace;
    }

    /**
     * @return the backTrace attribute.
     * @since ant 1.7
     */
    public boolean getBackTrace() {
        return backTrace;
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
     * Gets this macro's attribute (and define?) list.
     *
     * @return the nested Attributes
     */
    public List getAttributes() {
        return attributes;
    }

    /**
     * Gets this macro's elements.
     *
     * @return the map nested elements, keyed by element name, with
     *         {@link TemplateElement} values.
     */
    public Map getElements() {
        return elements;
    }

    /**
     * Check if a character is a valid character for an element or
     * attribute name.
     *
     * @param c the character to check
     * @return true if the character is a letter or digit or '.' or '-'
     *         attribute name
     */
    public static boolean isValidNameCharacter(char c) {
        // ? is there an xml api for this ?
        return Character.isLetterOrDigit(c) || c == '.' || c == '-';
    }

    /**
     * Check if a string is a valid name for an element or attribute.
     *
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
                "the name \"" + attribute.getName()
                + "\" has already been used by the text element");
        }
        for (int i = 0; i < attributes.size(); ++i) {
            Attribute att = (Attribute) attributes.get(i);
            if (att.getName().equals(attribute.getName())) {
                throw new BuildException(
                    "the name \"" + attribute.getName()
                        + "\" has already been used in "
                        + "another attribute element");
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
        if (hasImplicitElement
            || (element.isImplicit() && elements.size() != 0)) {
            throw new BuildException(
                "Only one element allowed when using implicit elements");
        }
        hasImplicitElement = element.isImplicit();
        elements.put(element.getName(), element);
    }

    /**
     * Create a new ant type based on the embedded tasks and types.
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
        log("creating macro  " + name, Project.MSG_VERBOSE);
    }


    /**
     * An attribute for the MacroDef task.
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
     * A nested text element for the MacroDef task.
     * @since ant 1.6.1
     */
    public static class Text {
        private String  name;
        private boolean optional;
        private boolean trim;
        private String  description;
        private String  defaultString;

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
         * The optional attribute of the text element.
         *
         * @param optional if true this is optional
         */
        public void setOptional(boolean optional) {
            this.optional = optional;
        }

        /**
         * @return true if the text is optional
         */
        public boolean getOptional() {
            return optional;
        }

        /**
         * The trim attribute of the text element.
         *
         * @param trim if true this String.trim() is called on
         *             the contents of the text element.
         */
        public void setTrim(boolean trim) {
            this.trim = trim;
        }

        /**
         * @return true if the text is trim
         */
        public boolean getTrim() {
            return trim;
        }

        /**
         * @param desc Description of the text.
         */
        public void setDescription(String desc) {
            description = desc;
        }

        /**
         * @return the description of the text, or <code>null</code> if
         *         no description is available.
         */
        public String getDescription() {
            return description;
        }

        /**
         * @param defaultString default text for the string.
         */
        public void setDefault(String defaultString) {
            this.defaultString = defaultString;
        }

        /**
         * @return the default text if set, null otherwise.
         */
        public String getDefault() {
            return defaultString;
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
            Text other = (Text) obj;
            return safeCompare(name, other.name)
                && optional == other.optional
                && trim == other.trim
                && safeCompare(defaultString, other.defaultString);
        }

        /**
         * @return a hash code value for this object.
         */
        public int hashCode() {
            return objectHashCode(name);
        }
    }

    private static boolean safeCompare(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * A nested element for the MacroDef task.
     */
    public static class TemplateElement {

        private String name;
        private String description;
        private boolean optional = false;
        private boolean implicit = false;

        /**
         * Sets the name of this element.
         *
         * @param name the name of the element
         */
        public void setName(String name) {
            if (!isValidName(name)) {
                throw new BuildException(
                    "Illegal name [" + name + "] for macro element");
            }
            this.name = name.toLowerCase(Locale.US);
        }

        /**
         * Gets the name of this element.
         *
         * @return the name of the element.
         */
        public String getName() {
            return name;
        }

        /**
         * Sets a textual description of this element,
         * for build documentation purposes only.
         *
         * @param desc Description of the element.
         * @since ant 1.6.1
         */
        public void setDescription(String desc) {
            description = desc;
        }

        /**
         * Gets the description of this element.
         *
         * @return the description of the element, or <code>null</code> if
         *         no description is available.
         * @since ant 1.6.1
         */
        public String getDescription() {
            return description;
        }

        /**
         * Sets whether this element is optional.
         *
         * @param optional if true this element may be left out, default
         *                 is false.
         */
        public void setOptional(boolean optional) {
            this.optional = optional;
        }

        /**
         * Gets whether this element is optional.
         *
         * @return the optional attribute
         */
        public boolean isOptional() {
            return optional;
        }

        /**
         * Sets whether this element is implicit.
         *
         * @param implicit if true this element may be left out, default
         *                 is false.
         */
        public void setImplicit(boolean implicit) {
            this.implicit = implicit;
        }

        /**
         * Gets whether this element is implicit.
         *
         * @return the implicit attribute
         */
        public boolean isImplicit() {
            return implicit;
        }

        /**
         * equality method.
         *
         * @param obj an <code>Object</code> value
         * @return a <code>boolean</code> value
         */
        public boolean equals(Object obj) {
            if (obj == this) {
              return true;
            }
            if (obj == null || !obj.getClass().equals(getClass())) {
                return false;
            }
            TemplateElement t = (TemplateElement) obj;
            return
                (name == null ? t.name == null : name.equals(t.name))
                && optional == t.optional
                && implicit == t.implicit;
        }

        /**
         * @return a hash code value for this object.
         */
        public int hashCode() {
            return objectHashCode(name)
                + (optional ? 1 : 0) + (implicit ? 1 : 0);
        }

    } // END static class TemplateElement

    /**
     * same or similar equality method for macrodef, ignores project and
     * runtime info.
     *
     * @param obj an <code>Object</code> value
     * @param same if true test for sameness, otherwise just similiar
     * @return a <code>boolean</code> value
     */
    private boolean sameOrSimilar(Object obj, boolean same) {
        if (obj == this) {
            return true;
        }

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
        // Allow two macro definitions with the same location
        // to be treated as similar - bugzilla 31215
        if (other.getLocation() != null
            && other.getLocation().equals(getLocation())
            && !same) {
            return true;
        }
        if (text == null) {
            if (other.text != null) {
                return false;
            }
        } else {
            if (!text.equals(other.text)) {
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
     * Similar method for this definition
     *
     * @param obj another definition
     * @return true if the definitions are similar
     */
    public boolean similar(Object obj) {
        return sameOrSimilar(obj, false);
    }

    /**
     * Equality method for this definition
     *
     * @param obj another definition
     * @return true if the definitions are the same
     */
    public boolean sameDefinition(Object obj) {
        return sameOrSimilar(obj, true);
    }

    /**
     * extends AntTypeDefinition, on create
     * of the object, the template macro definition
     * is given.
     */
    private static class MyAntTypeDefinition extends AntTypeDefinition {
        private MacroDef macroDef;

        /**
         * Creates a new <code>MyAntTypeDefinition</code> instance.
         *
         * @param macroDef a <code>MacroDef</code> value
         */
        public MyAntTypeDefinition(MacroDef macroDef) {
            this.macroDef = macroDef;
        }

        /**
         * Create an instance of the definition.
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
            return macroDef.sameDefinition(otherDef.macroDef);
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
            return macroDef.similar(otherDef.macroDef);
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
